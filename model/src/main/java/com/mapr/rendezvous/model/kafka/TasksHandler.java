package com.mapr.rendezvous.model.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapr.rendezvous.commons.kafka.KafkaClient;
import com.mapr.rendezvous.commons.kafka.entity.TaskRequest;
import com.mapr.rendezvous.commons.kafka.entity.TaskResponse;
import com.mapr.rendezvous.commons.kafka.util.KafkaNameUtility;
import com.mapr.rendezvous.model.service.TasksRunner;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Set;

import static java.lang.String.format;

@Slf4j
@Component
@RequiredArgsConstructor
public class TasksHandler {
    private final static String TOPIC = "tasks";
    private final static ObjectMapper MAPPER = new ObjectMapper();

    private final KafkaClient client;
    private final String stream;
    private final TasksRunner executor;

    @PostConstruct
    private void init() {
        Set<String> topics = Collections.singleton(KafkaNameUtility.convertToKafkaTopic(stream, TOPIC));
        client.subscribe(topics).subscribe(this::handle);
    }

    @SneakyThrows
    private void handle(ConsumerRecord<String, byte[]> record) {
        TaskRequest task = MAPPER.readValue(record.value(), TaskRequest.class);
        log.info("Received task {}", task);
        TaskResponse response = executor.handle(task);
        String topic = KafkaNameUtility.convertToKafkaTopic(stream, format("proxy-%s", task.getProxyId()));
        log.info("Sending response to {}", topic);
        client.publish(topic, MAPPER.writeValueAsBytes(response)).subscribe();
    }
}
