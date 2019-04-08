package com.mapr.rendezvous.proxy.db.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "database")
public class DatabaseCredentials {
    private String name = "/";
    private String host = "host";
    private String username = "mapr";
    private String password = "mapr";
}
