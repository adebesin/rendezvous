package com.mapr.rendezvous.proxy.db.configuration;

import com.mapr.springframework.data.maprdb.config.AbstractMapRConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DatabaseConfig  extends AbstractMapRConfiguration {
    private final DatabaseCredentials credentials;

    @Override
    protected String getDatabaseName() {
        return credentials.getName();
    }

    @Override
    protected String getHost() {
        return credentials.getHost();
    }

    @Override
    protected String getUsername() {
        return credentials.getUsername();
    }

    @Override
    protected String getPassword() {
        return credentials.getPassword();
    }
}
