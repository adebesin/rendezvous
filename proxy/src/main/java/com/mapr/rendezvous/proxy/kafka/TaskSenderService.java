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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;

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
            TaskRequest taskRequest = fillMissingFieldsInRequest(task);
            String primaryModelId = taskRequest.getModelId() != null ?
                    taskRequest.getModelId() : modelProvider.getPrimaryModel().orElse("");

            DirectProcessor<TaskResponse> handler = DirectProcessor.create();
            handlers.put(taskRequest.getRequestId(), handler);

            return send(taskRequest)
                    .doOnError(throwable -> log.error("Failed to send", throwable))
                    .then(Mono.fromCallable(() -> primaryModelId))
                    .flatMap(id -> receive(id, taskRequest.getTimeout(), handler))
                    .doOnSuccessOrError((v, e) -> handlers.remove(taskRequest.getRequestId()));
        });
    }

    private Mono<TaskResponse> receive(String primaryId, Long timeout, DirectProcessor<TaskResponse> handler) {
        List<TaskResponse> responses = new ArrayList<>();
        Duration duration = Duration.ofMillis(timeout);
        return handler.timeout(duration)
                .filter(response -> checkIfPrimaryAndSave(primaryId, response, responses))
                .buffer(1)
                .onErrorReturn(TimeoutException.class, responses)
                .map(list -> {
                    list.sort(Comparator.comparing(TaskResponse::getAccuracy));
                    return list;
                })
                .map(list -> list.get(0))
                .onErrorMap(e -> onError(e, duration))
                .next();
    }

    @SneakyThrows
    private Throwable onError(Throwable throwable, Duration duration) {
        if (throwable instanceof IndexOutOfBoundsException)
            return new TimeoutException(format("Timeout %d milliseconds", duration.toMillis()));
        return throwable;
    }

    @SneakyThrows
    private Mono<Void> send(TaskRequest task) {
        log.debug("Sending task {}", task.getRequestId());
        log.debug(task.toString());
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
        if (handler != null) {
            log.debug("Received response for task {} from model {}", response.getRequestId(), response.getModelId());
            log.debug(response.toString());
            handler.onNext(response);
        }
    }

    private TaskRequest fillMissingFieldsInRequest(TaskRequest task) {
        TaskRequest.TaskRequestBuilder builder = TaskRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .proxyId(config.getProxyId());

        if (task.getModelId() != null && !task.getModelId().isEmpty())
            builder.modelId(task.getModelId());

        if (task.getModelClass() != null && !task.getModelClass().isEmpty())
            builder.modelClass(task.getModelClass());

        if (task.getTimeout() == null || task.getTimeout() == 0)
            builder.timeout(config.getDefaultTimeout());
        else
            builder.timeout(task.getTimeout());

        return builder.build();
    }
}
