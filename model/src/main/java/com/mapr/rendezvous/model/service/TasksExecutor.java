package com.mapr.rendezvous.model.service;

import com.mapr.rendezvous.commons.kafka.entity.TaskRequest;
import com.mapr.rendezvous.commons.kafka.entity.TaskResponse;
import com.mapr.rendezvous.model.config.ModelConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TasksExecutor {
    private final ModelConfig config;

    public TaskResponse handle(TaskRequest request) {
        return TaskResponse.builder()
                .requestId(request.getRequestId())
                .modelId(config.getId())
                .modelClass(config.getModelClass())
                .build();
    }
}
