package com.mapr.rendezvous.proxy.controller;

import com.mapr.rendezvous.commons.kafka.entity.TaskRequest;
import com.mapr.rendezvous.commons.kafka.entity.TaskResponse;
import com.mapr.rendezvous.proxy.kafka.TaskSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/task")
public class TasksController {
    private final TaskSenderService taskService;

    @PutMapping
    public TaskResponse putTask(TaskRequest request) {
        log.info("PUT request for '/task' {}", request);
        return taskService.sendAndReceive(request);
    }
}
