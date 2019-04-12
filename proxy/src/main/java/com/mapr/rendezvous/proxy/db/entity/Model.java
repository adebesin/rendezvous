package com.mapr.rendezvous.proxy.db.entity;

import com.mapr.springframework.data.maprdb.core.mapping.Document;
import lombok.Data;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.data.annotation.Id;

@Data
@Document
public class Model {
    @Id
    private String id;
    private Float accuracy;
    private Field.Str modelClass;
    private Boolean primary;
}
