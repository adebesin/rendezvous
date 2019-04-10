package com.mapr.rendezvous.proxy.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapr.db.exceptions.TableExistsException;
import com.mapr.rendezvous.commons.kafka.AdminService;
import com.mapr.rendezvous.commons.kafka.KafkaClient;
import com.mapr.rendezvous.commons.kafka.entity.TaskRequest;
import com.mapr.rendezvous.commons.kafka.entity.TaskResponse;
import com.mapr.rendezvous.commons.kafka.util.KafkaNameUtility;
import com.mapr.rendezvous.proxy.db.ModelService;
import config.ProxyConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.nio.file.FileAlreadyExistsException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSenderService {
    private final static String REQUEST_TOPIC = "tasks";
    private final static String RESPONSE_TOPIC_PATTERN = "proxy-%s";
    private final static ObjectMapper MAPPER = new ObjectMapper();

    private final ProxyConfig config;
    private final String stream;
    private final AdminService admin;
    private final KafkaClient client;
    private final ModelService modelService;

    private String requestTopic;
    private Map<String, DirectProcessor<TaskResponse>> handlers = new ConcurrentHashMap<>();

    @SneakyThrows
    @PostConstruct
    private void init() {
        requestTopic = KafkaNameUtility.convertToKafkaTopic(stream, REQUEST_TOPIC);
        String responseTopic = format(RESPONSE_TOPIC_PATTERN, config.getProxyId());
        String responseStreamAndTopic = KafkaNameUtility.convertToKafkaTopic(stream, responseTopic);

        admin.createStreamAsync(stream)
                .onErrorResume(TableExistsException.class, ex -> Mono.empty())
                .then(admin.createTopicAsync(stream, REQUEST_TOPIC))
                .then(admin.createTopicAsync(stream, responseTopic))
                .onErrorResume(FileAlreadyExistsException.class, ex -> Mono.empty())
                .thenMany(client.subscribe(Collections.singleton(responseStreamAndTopic)))
                .map(this::convert)
                .subscribe(this::handleResponse);
    }

    public Mono<TaskResponse> sendAndReceive(TaskRequest task) {
        return Mono.defer(() -> {
            task.setRequestId(UUID.randomUUID().toString());
            task.setProxyId(config.getProxyId());

            DirectProcessor<TaskResponse> handler = DirectProcessor.create();
            handlers.put(task.getRequestId(), handler);

            return send(task)
                    .doOnError(throwable -> log.error("Failed to send", throwable))
                    .then(Mono.fromCallable(() -> modelService.getPrimaryId().orElse("")))
                    .flatMap(id -> receive(id, task.getTimeout(), handler))
                    .doOnSuccessOrError((v, e) -> handlers.remove(task.getRequestId()));
        });
    }

    private Mono<TaskResponse> receive(String primaryId, Long timeout, DirectProcessor<TaskResponse> handler) {
        List<TaskResponse> responses = new ArrayList<>();
        Duration duration = Duration.ofMillis(timeout != null ? timeout : config.getDefaultTimeout());
        return handler.timeout(duration)
                .filter(response -> checkIfPrimaryAndSave(primaryId, response, responses))
                .buffer(1)
                .onErrorReturn(TimeoutException.class, responses)
                .map(list -> {
                    list.sort(Comparator.comparing(TaskResponse::getAccuracy));
                    return list;
                })
                .map(list -> list.get(0))
                .next();
    }

    @SneakyThrows
    public Mono<Void> send(TaskRequest task) {
        return client.publish(requestTopic, MAPPER.writeValueAsBytes(task));
    }

    @SneakyThrows
    private TaskResponse convert(ConsumerRecord<String, byte[]> record) {
        return MAPPER.readValue(record.value(), TaskResponse.class);
    }

    private boolean checkIfPrimaryAndSave(String primaryId, TaskResponse response, List<TaskResponse> result) {
        if (response.getModelId().equals(primaryId))
            return true;

        result.add(response);

        return false;
    }

    private void handleResponse(TaskResponse response) {
        DirectProcessor<TaskResponse> handler = handlers.get(response.getRequestId());
        if (handler != null)
            handler.onNext(response);
    }
}
