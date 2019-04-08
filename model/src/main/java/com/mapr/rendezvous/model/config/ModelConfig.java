package com.mapr.rendezvous.model.config;

import com.mapr.rendezvous.commons.entity.ModelClass;
import com.mapr.rendezvous.commons.kafka.entity.ModelInfo;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
@ConfigurationProperties(prefix = "config")
public class ModelConfig extends ModelInfo {

    public ModelConfig() {
        super(UUID.randomUUID().toString(), ModelClass.Model1, 0.0f);
    }
}
