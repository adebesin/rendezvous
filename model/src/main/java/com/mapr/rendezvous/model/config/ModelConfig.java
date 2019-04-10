package com.mapr.rendezvous.model.config;

import com.mapr.rendezvous.commons.entity.ModelClass;
import com.mapr.rendezvous.commons.kafka.entity.ModelInfo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Data
@Configuration
@ConfigurationProperties(prefix = "config")
public class ModelConfig extends ModelInfo {
    private Integer sleepStart = 1000;
    private Integer sleepEnd = 5000;

    private Double resultStart = 0.0;
    private Double resultEnd = 100.0;

    public ModelConfig() {
        super(UUID.randomUUID().toString(), ModelClass.Model1, 0.0f);
    }
}
