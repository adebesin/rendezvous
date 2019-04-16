package com.mapr.rendezvous.commons.kafka.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder=true)
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    private String requestId;
    private String proxyId;
    private Long timeout;
    private String modelClass;
    private String modelId;
}
