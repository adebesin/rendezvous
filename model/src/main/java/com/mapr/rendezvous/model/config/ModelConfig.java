package com.mapr.rendezvous.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Data
@Configuration
@ConfigurationProperties(prefix = "config")
public class ModelConfig {
    private String id = UUID.randomUUID().toString();
    private String modelClass = "TestModel";
    private Float accuracy = 0.0f;
    private Integer sleepStart = 10;
    private Integer sleepEnd = 100;

    private Double resultStart = 0.0;
    private Double resultEnd = 100.0;
}
