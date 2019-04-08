package com.mapr.rendezvous.proxy.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapr.rendezvous.commons.kafka.AdminService;
import com.mapr.rendezvous.commons.kafka.KafkaClient;
import com.mapr.rendezvous.commons.kafka.entity.ModelInfo;
import com.mapr.rendezvous.commons.kafka.util.KafkaNameUtility;
import com.mapr.rendezvous.proxy.db.entity.Model;
import com.mapr.rendezvous.proxy.db.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelDiscoveryService {
    private final static String TOPIC = "discovery";
    private final static ObjectMapper MAPPER = new ObjectMapper();

    private final String stream;
    private final AdminService admin;
    private final KafkaClient client;
    private final ModelRepository repository;

    @SneakyThrows
    @PostConstruct
    private void init() {
        admin.createStreamIfNotExists(stream);
        admin.createTopicIfNotExists(stream, TOPIC);
        Set<String> topics = Collections.singleton(KafkaNameUtility.convertToKafkaTopic(stream, TOPIC));
        client.subscribe(topics).subscribe(this::handle);
    }

    @SneakyThrows
    private void handle(ConsumerRecord<String,byte[]> record) {
        ModelInfo info = MAPPER.readValue(record.value(), ModelInfo.class);

        log.info("Received info about model id:{}", info.getId());
        log.debug(info.toString());

        updateModel(info);
    }

    private void updateModel(ModelInfo info) {
        Optional<Model> opModel = repository.findById(info.getId());

        Model model = opModel.orElseGet(Model::new);
        model.setId(info.getId());
        model.setModelClass(info.getModelClass());
        model.setAccuracy(info.getAccuracy());

        repository.save(model);
    }
}
