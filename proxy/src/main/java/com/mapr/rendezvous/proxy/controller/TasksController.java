package com.mapr.rendezvous.proxy.controller;

import com.mapr.rendezvous.commons.kafka.entity.TaskRequest;
import com.mapr.rendezvous.commons.kafka.entity.TaskResponse;
import com.mapr.rendezvous.proxy.kafka.TaskSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/task")
public class TasksController {
    private final TaskSenderService taskService;

    @PutMapping
    public Mono<TaskResponse> putTask(TaskRequest request) {
        log.info("PUT request for '/task'");
        return taskService.sendAndReceive(request);
    }

    @ExceptionHandler({TimeoutException.class})
    public ResponseEntity handleException(TimeoutException e) {
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(e.getMessage());
    }
}
