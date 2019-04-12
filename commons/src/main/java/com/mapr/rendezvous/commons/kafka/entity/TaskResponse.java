package com.mapr.rendezvous.commons.kafka.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private String requestId;
    private String modelClass;
    private String modelId;
    private String result;
    private Float accuracy;
}
