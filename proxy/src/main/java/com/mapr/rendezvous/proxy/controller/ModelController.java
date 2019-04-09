package com.mapr.rendezvous.proxy.controller;

import com.mapr.rendezvous.proxy.db.ModelService;
import com.mapr.rendezvous.proxy.db.entity.Model;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/model")
public class ModelController {
    private final ModelService service;

    @GetMapping(produces = "application/json")
    public Collection<Model> getAllModels() {
        log.info("Get request for '/model'");
        return service.getAllModels();
    }

    @PutMapping(path = "/{id}")
    public Model changePrimaryModel(@PathVariable("id") String id, @RequestParam("primary") Boolean primary) {
        return service.setPrimaryModel(id, primary);
    }
}
