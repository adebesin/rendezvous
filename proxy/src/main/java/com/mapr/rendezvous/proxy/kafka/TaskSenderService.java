package com.mapr.rendezvous.proxy.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapr.rendezvous.commons.kafka.AdminService;
import com.mapr.rendezvous.commons.kafka.KafkaClient;
import com.mapr.rendezvous.commons.kafka.entity.TaskRequest;
import com.mapr.rendezvous.commons.kafka.entity.TaskResponse;
import com.mapr.rendezvous.commons.kafka.util.KafkaNameUtility;
import com.mapr.rendezvous.proxy.db.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.DirectProcessor;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSenderService {
    private final static String REQUEST_TOPIC = "tasks";
    private final static String RESPONSE_TOPIC = "proxy-1";
    private final static ObjectMapper MAPPER = new ObjectMapper();

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
        String responseTopic = KafkaNameUtility.convertToKafkaTopic(stream, RESPONSE_TOPIC);

        admin.createStreamIfNotExists(stream);
        admin.createTopicIfNotExists(stream, REQUEST_TOPIC);
        admin.createTopicIfNotExists(stream, RESPONSE_TOPIC);

        client.subscribe(Collections.singleton(responseTopic)).map(this::convert).subscribe(this::handleResponse);
    }

    public TaskResponse sendAndReceive(TaskRequest task) {
        String primaryId = modelService.getPrimaryId().orElse("");

        task.setRequestId(UUID.randomUUID().toString());
        task.setProxyId("1");

        DirectProcessor<TaskResponse> handler = DirectProcessor.create();
        handlers.put(task.getRequestId(), handler);

        send(task);

        TaskResponse response = receive(primaryId, task.getTimeout(), handler);
        handlers.remove(task.getRequestId());

        return response;
    }

    public TaskResponse receive(String primaryId, Long timeout, DirectProcessor<TaskResponse> handler) {
        List<TaskResponse> responses = Collections.synchronizedList(new ArrayList<>());
        Duration duration = Duration.ofMillis(timeout != null ? timeout : 10000);
        List<TaskResponse> result = handler.timeout(duration)
                .filter(response -> checkIfPrimaryAndSave(primaryId, response, responses))
                .buffer(1).onErrorReturn(TimeoutException.class, responses).blockFirst();

        if (result == null || result.isEmpty()) {
            return null;
        } else {
            result.sort(Comparator.comparing(TaskResponse::getAccuracy));
            return result.get(0);
        }
    }

    @SneakyThrows
    public void send(TaskRequest task) {
        client.publish(requestTopic, MAPPER.writeValueAsBytes(task)).subscribe();
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
