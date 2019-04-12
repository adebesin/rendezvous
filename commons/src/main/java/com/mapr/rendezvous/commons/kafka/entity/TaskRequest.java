package com.mapr.rendezvous.commons.kafka.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.protocol.types.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    private String requestId;
    private String proxyId;
    private Long timeout;
    private Field.Str modelClass;
    private String modelId;
}
