package com.mapr.rendezvous.model.kafka;

import com.mapr.rendezvous.commons.kafka.AdminService;
import com.mapr.rendezvous.commons.kafka.KafkaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelKafkaConfig {
    @Value("${kafka.stream:/rendezvous}")
    private String stream;

    @Bean
    public String stream() {
        return stream;
    }

    @Bean
    public AdminService adminService() {
        return new AdminService();
    }

    @Bean
    public KafkaClient kafkaClient() {
        return new KafkaClient();
    }
}
