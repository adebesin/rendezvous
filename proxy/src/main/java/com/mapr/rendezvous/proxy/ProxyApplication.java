package com.mapr.rendezvous.proxy;

import com.mapr.springframework.data.maprdb.repository.config.EnableMapRRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableMapRRepository
@SpringBootApplication
public class ProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProxyApplication.class, args);
    }
}
