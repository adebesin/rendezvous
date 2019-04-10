package com.mapr.rendezvous.model.service;

import com.mapr.rendezvous.commons.kafka.entity.TaskRequest;
import com.mapr.rendezvous.commons.kafka.entity.TaskResponse;
import com.mapr.rendezvous.model.config.ModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class TasksRunner extends AbstractTaskRunner {
    private final ModelConfig config;

    @Override
    public TaskResponse handle(TaskRequest request) {
        simulateProcessing();
        return TaskResponse.builder()
                .requestId(request.getRequestId())
                .modelId(config.getId())
                .modelClass(config.getModelClass())
                .result(generateResult())
                .accuracy(config.getAccuracy())
                .build();
    }

    @SneakyThrows
    private void simulateProcessing() {
        int sleepTime = ThreadLocalRandom.current().nextInt(config.getSleepStart(), config.getSleepEnd() + 1);
        log.debug("Sleeping for {}", sleepTime);
        Thread.sleep(sleepTime);
    }

    private String generateResult() {
        Double result = ThreadLocalRandom.current()
                .nextDouble(config.getResultStart(), config.getSleepEnd() + 1);
        log.debug("Generated {}", result);

        return result.toString();
    }
}
