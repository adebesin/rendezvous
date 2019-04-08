package com.mapr.rendezvous.model.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapr.rendezvous.commons.kafka.AdminService;
import com.mapr.rendezvous.commons.kafka.KafkaClient;
import com.mapr.rendezvous.commons.kafka.entity.ModelInfo;
import com.mapr.rendezvous.commons.kafka.util.KafkaNameUtility;
import com.mapr.rendezvous.model.config.ModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscoveryInfoSender {
    private final static String TOPIC = "discovery";
    private final static ObjectMapper MAPPER = new ObjectMapper();

    private final String stream;
    private final ModelConfig config;
    private final AdminService admin;
    private final KafkaClient client;

    @SneakyThrows
    @PostConstruct
    private void init() {
        String topic = KafkaNameUtility.convertToKafkaTopic(stream, TOPIC);
        admin.createStreamIfNotExists(stream);
        admin.createTopicIfNotExists(stream, TOPIC);
        client.publish(topic, MAPPER.writeValueAsBytes(convertToModelInfo(config))).subscribe();
    }

    private ModelInfo convertToModelInfo(ModelConfig config) {
        return ModelInfo.builder()
                .id(config.getId())
                .modelClass(config.getModelClass())
                .accuracy(config.getAccuracy())
                .build();
    }
}
