package com.mapr.rendezvous.proxy.db.entity;

import com.mapr.rendezvous.commons.entity.ModelClass;
import com.mapr.springframework.data.maprdb.core.mapping.Document;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@Document
public class Model {
    @Id
    private String id;
    private Float accuracy;
    private ModelClass modelClass;
    private Boolean primary;
}
