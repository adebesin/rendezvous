package com.mapr.rendezvous.proxy.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PropertyPrimaryModelProvider implements PrimaryModelProvider {
    @Value("${model.primary:1}")
    private String primaryModelId;

    @Override
    public Optional<String> getPrimaryModel() {
        return Optional.of(primaryModelId);
    }
}
