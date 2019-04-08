package com.mapr.rendezvous.proxy.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapr.rendezvous.commons.kafka.AdminService;
import com.mapr.rendezvous.commons.kafka.KafkaClient;
import com.mapr.rendezvous.commons.kafka.entity.TaskRequest;
import com.mapr.rendezvous.commons.kafka.entity.TaskResponse;
import com.mapr.rendezvous.commons.kafka.util.KafkaNameUtility;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

    private String requestTopic;
    private String responseTopic;

    @SneakyThrows
    @PostConstruct
    private void init() {
        requestTopic = KafkaNameUtility.convertToKafkaTopic(stream, REQUEST_TOPIC);
        responseTopic = KafkaNameUtility.convertToKafkaTopic(stream, RESPONSE_TOPIC);

        admin.createStreamIfNotExists(stream);
        admin.createTopicIfNotExists(stream, REQUEST_TOPIC);
        admin.createTopicIfNotExists(stream, RESPONSE_TOPIC);
    }

    public List<TaskResponse> sendAndReceive(TaskRequest task) {
        send(task);
        return receive(task.getTimeout());
    }

    //TODO fix receive
    @SneakyThrows
    public List<TaskResponse> receive(Long timeout) {
        AtomicReference<List<TaskResponse>> result = new AtomicReference<>();
        Disposable disposable = client.subscribe(Collections.singleton(responseTopic)).map(this::convert).collectList()
                .doOnNext(result::set).subscribe();
        Thread.sleep(timeout != null ? timeout : 10000);
        disposable.dispose();
        return result.get();
    }

    @SneakyThrows
    public void send(TaskRequest task) {
        task.setProxyId("1");
        client.publish(requestTopic, MAPPER.writeValueAsBytes(task)).subscribe();
    }


    @SneakyThrows
    private TaskResponse convert(ConsumerRecord<String,byte[]> record) {
        log.info("received");
        return MAPPER.readValue(record.value(), TaskResponse.class);
    }
}
