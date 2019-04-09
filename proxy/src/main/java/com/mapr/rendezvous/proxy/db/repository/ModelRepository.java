package com.mapr.rendezvous.proxy.db.repository;

import com.mapr.rendezvous.proxy.db.entity.Model;
import com.mapr.springframework.data.maprdb.repository.MapRRepository;

import java.util.List;

public interface ModelRepository extends MapRRepository<Model, String> {
    List<Model> findByPrimaryTrue();
}
