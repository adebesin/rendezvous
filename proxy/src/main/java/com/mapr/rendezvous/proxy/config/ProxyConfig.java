package com.mapr.rendezvous.proxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Data
@Configuration
@ConfigurationProperties(prefix = "config")
public class ProxyConfig {
    private String proxyId = UUID.randomUUID().toString();
    private Long defaultTimeout = 30000L;
}
