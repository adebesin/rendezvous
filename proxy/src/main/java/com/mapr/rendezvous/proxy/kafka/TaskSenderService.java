package com.mapr.rendezvous.proxy.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapr.rendezvous.commons.kafka.AdminService;
import com.mapr.rendezvous.commons.kafka.KafkaClient;
import com.mapr.rendezvous.commons.kafka.entity.TaskRequest;
import com.mapr.rendezvous.commons.kafka.entity.TaskResponse;
import com.mapr.rendezvous.commons.kafka.util.KafkaNameUtility;
import com.mapr.rendezvous.proxy.config.ProxyConfig;
import com.mapr.rendezvous.proxy.model.PrimaryModelProvider;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
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
    private final PrimaryModelProvider modelProvider;

    private String requestTopic;
    private Map<String, DirectProcessor<TaskResponse>> handlers = new ConcurrentHashMap<>();

    @SneakyThrows
    @PostConstruct
    private void init() {
        requestTopic = KafkaNameUtility.convertToKafkaTopic(stream, REQUEST_TOPIC);
        String responseTopic = format(RESPONSE_TOPIC_PATTERN, config.getProxyId());
        String responseStreamAndTopic = KafkaNameUtility.convertToKafkaTopic(stream, responseTopic);

        admin.createStreamAsync(stream)
                .then(admin.createTopicAsync(stream, REQUEST_TOPIC))
                .then(admin.createTopicAsync(stream, responseTopic))
                .thenMany(client.subscribe(Collections.singleton(responseStreamAndTopic)))
                .map(this::convert)
                .subscribeOn(Schedulers.newSingle("TasksResponsesHandler"))
                .subscribe(this::handleResponse);
    }

    public Mono<TaskResponse> sendAndReceive(TaskRequest task) {
        return Mono.defer(() -> {
            TaskRequest taskRequest = task.toBuilder()
                    .requestId(UUID.randomUUID().toString())
                    .proxyId(config.getProxyId())
                    .build();

            DirectProcessor<TaskResponse> handler = DirectProcessor.create();
            handlers.put(taskRequest.getRequestId(), handler);

            return send(taskRequest)
                    .doOnError(throwable -> log.error("Failed to send", throwable))
                    .then(Mono.fromCallable(() -> modelProvider.getPrimaryModel().orElse("")))
                    .flatMap(id -> receive(id, taskRequest.getTimeout(), handler))
                    .doOnSuccessOrError((v, e) -> handlers.remove(taskRequest.getRequestId()));
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
                .doOnError(e -> onError(e, duration))
                .next();
    }

    @SneakyThrows
    private void onError(Throwable throwable, Duration duration) {
        if(throwable instanceof IndexOutOfBoundsException)
            throw new TimeoutException(format("Timeout %d milliseconds", duration.toMillis()));
        throw throwable;
    }

    @SneakyThrows
    private Mono<Void> send(TaskRequest task) {
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
