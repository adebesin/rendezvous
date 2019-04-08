package com.mapr.rendezvous.commons.kafka.entity;

import com.mapr.rendezvous.commons.entity.ModelClass;
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
    private ModelClass modelClass;
    private String modelId;
    private String result;
}
