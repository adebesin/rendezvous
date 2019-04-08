package com.mapr.rendezvous.proxy.db.repository;

import com.mapr.rendezvous.proxy.db.entity.Model;
import com.mapr.springframework.data.maprdb.repository.MapRRepository;

public interface ModelRepository extends MapRRepository<Model, String> {
}
