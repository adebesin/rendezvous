package com.mapr.rendezvous.commons.kafka;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static reactor.kafka.sender.SenderRecord.create;

@Slf4j
public class KafkaClient {
    private final Map<UUID, KafkaClient.MessageHandler> messageHandlers = new ConcurrentHashMap<>();
    private final Scheduler consumerScheduler = Schedulers.newSingle("KafkaConsumer-thread");
    private final BlockingDeque<SubscriptionEvent> subscribeEvents = new LinkedBlockingDeque<>();
    private final KafkaSender<String, byte[]> kafkaSender;
    private final KafkaConsumer<String, byte[]> kafkaConsumer;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Disposable consumerTask;
    private final String HOST_URI = "localhost:9092";
    private final String GROUP_ID = UUID.randomUUID().toString();

    public KafkaClient() {
        Map<String, Object> producerProperties = new HashMap<>();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, HOST_URI);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        producerProperties.put(ProducerConfig.BATCH_SIZE_CONFIG, 67108864); //64 megabytes
        producerProperties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 128 * 1024 * 1024);
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, 0);
        producerProperties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 50_000);
        producerProperties.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 15728640); //15 megabytes
        SenderOptions<String, byte[]> senderOptions = SenderOptions.create(producerProperties);
        kafkaSender = KafkaSender.create(senderOptions);

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, HOST_URI);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);

        kafkaConsumer = new KafkaConsumer<>(consumerProps);
        consumerTask = connect();
    }

    private Disposable connect() {
        log.info("Starting kafka client {}", hashCode());
        return consumerScheduler.schedule(() -> {
            while (!closed.get()) {
                try {
                    LinkedList<SubscriptionEvent> subEvents = new LinkedList<>();
                    if (subscribeEvents.drainTo(subEvents) > 0) {
                        Set<TopicPartition> allTopics = subEvents.getLast()
                                .getAllTopics()
                                .stream()
                                .map(topic -> new TopicPartition(topic, 0))
                                .collect(toSet());
                        Set<TopicPartition> newTopics = subEvents.stream()
                                .flatMap(subscriptionEvent -> subscriptionEvent.getNewTopics().stream())
                                .map(topic -> new TopicPartition(topic, 0))
                                .filter(allTopics::contains)
                                .collect(toSet());
                        kafkaConsumer.assign(allTopics);
                        if (isNotEmpty(newTopics)) {
                            if (newTopics.size() > 10) {
                                log.debug("Subscribing to {} topics", newTopics.size());
                            } else {
                                log.debug("Subscribing to topics {}", newTopics);
                            }
                            kafkaConsumer.seekToEnd(newTopics);
                        }
                    }

                    if (kafkaConsumer.assignment().isEmpty()) {
                        Thread.sleep(100);
                        continue;
                    }
                    ConsumerRecords<String, byte[]> consumerRecords = kafkaConsumer.poll(Integer.MAX_VALUE);
                    if (Objects.isNull(consumerRecords) || consumerRecords.isEmpty()) {
                        continue;
                    }
                    for (ConsumerRecord<String, byte[]> consumerRecord : consumerRecords) {
                        String topicName = consumerRecord.topic();
                        for (KafkaClient.MessageHandler messageHandler : messageHandlers.values()) {
                            messageHandler.consume(topicName, consumerRecord);
                        }
                    }
                    doCommitSync();
                } catch (WakeupException ignore) {
                    // ignore, we're subscribing for new topics
                } catch (Exception e) {
                    log.error("Unexpected error: {}", e.getMessage(), e);
                }
            }
        });
    }

    @SneakyThrows
    public void close() {
        log.info("Closing kafka client {}", hashCode());
        if (!closed.get()) {
            closed.set(true);
            kafkaConsumer.wakeup();
            while (!consumerTask.isDisposed()) {
                Thread.sleep(100);
            }
            doCommitSync();
            kafkaConsumer.close();
            kafkaSender.close();
            consumerScheduler.dispose();
            log.info("Closed kafka client {}", hashCode());
        } else {
            log.warn("Received multiple close requests. Already closed {}", hashCode());
        }
    }

    public Mono<Void> publish(String topic, byte[] payload) {
        return kafkaSender.send(Mono.just(createSenderRecord(topic, payload)))
                .doOnSubscribe(aVoid -> log.trace("Publishing to '{}'", topic))
                .doOnError(t -> log.error("Failed to publish to '{}'", topic, t))
                .then();
    }

    public Flux<ConsumerRecord<String, byte[]>> subscribe(Set<String> topicNames) {
        return Flux.create(sink -> {
            UUID handlerId = UUID.randomUUID();
            KafkaClient.MessageHandler messageHandler = new MessageHandler(topicNames, sink::next);
            putMessageHandler(handlerId, messageHandler);
            sink.onDispose(() -> removeMessageHandler(handlerId));
        });
    }

    public Flux<ConsumerRecord<String, byte[]>> subscribe(Set<String> topicNames, int offset) {
        kafkaConsumer.endOffsets(topicNames.stream().map(topic -> new TopicPartition(topic, 0))
                .collect(toSet())).forEach((k, v) -> kafkaConsumer.seek(k, v - offset));
        return Flux.create(sink -> {
            UUID handlerId = UUID.randomUUID();
            KafkaClient.MessageHandler messageHandler = new MessageHandler(topicNames, sink::next);
            putMessageHandler(handlerId, messageHandler);
            sink.onDispose(() -> removeMessageHandler(handlerId));
        });
    }


    private void doCommitSync() {
        try {
            kafkaConsumer.commitSync();
        } catch (WakeupException ignored) {

        } catch (CommitFailedException e) {
            // the commit failed with an unrecoverable error. if there is any
            // internal state which depended on the commit, you can clean it
            // up here. otherwise it's reasonable to ignore the error and go on
            log.debug("Commit failed", e);
        }
    }

    @SneakyThrows
    @Synchronized
    private void putMessageHandler(UUID handlerId, KafkaClient.MessageHandler messageHandler) {
        //Set<String> previousTopics = getCurrentlySubscribedTopics();
        messageHandlers.put(handlerId, messageHandler);
        Set<String> currentTopics = getCurrentlySubscribedTopics();
        subscribeEvents.putLast(new SubscriptionEvent(currentTopics, messageHandler.getTopics().collect(toSet())));
        //Set<String> newTopics = Sets.difference(messageHandler.getTopics().collect(toSet()), previousTopics);
        //subscribeEvents.putLast(new SubscriptionEvent(currentTopics, newTopics));
        kafkaConsumer.wakeup();
    }

    @SneakyThrows
    @Synchronized
    private void removeMessageHandler(UUID handlerId) {
        if (messageHandlers.remove(handlerId) != null) {
            subscribeEvents.putLast(new SubscriptionEvent(getCurrentlySubscribedTopics(), emptySet()));
            kafkaConsumer.wakeup();
        }
    }

    private Set<String> getCurrentlySubscribedTopics() {
        return messageHandlers.values()
                .stream()
                .flatMap(KafkaClient.MessageHandler::getTopics)
                .collect(toSet());
    }

    private SenderRecord<String, byte[], Long> createSenderRecord(String topic, byte[] payload) {
        return create(new ProducerRecord<>(topic, 0, GROUP_ID, payload), 0L);
    }

    @RequiredArgsConstructor
    private static class MessageHandler {
        private final Set<String> subscribedTopics;
        private final Consumer<ConsumerRecord<String, byte[]>> messageConsumer;

        public void consume(String topic, ConsumerRecord<String, byte[]> message) {
            if (subscribedTopics.contains(topic)) {
                messageConsumer.accept(message);
            }
        }

        public Stream<String> getTopics() {
            return subscribedTopics.stream();
        }
    }

    @Value
    private class SubscriptionEvent {
        private Set<String> allTopics;
        private Set<String> newTopics;
    }
}
