package com.mapr.rendezvous.model.service;

import com.mapr.rendezvous.commons.kafka.entity.TaskRequest;
import com.mapr.rendezvous.commons.kafka.entity.TaskResponse;

public abstract class AbstractTaskRunner {

    public abstract TaskResponse handle(TaskRequest request);
}
