package com.mapr.rendezvous.proxy.grpc;

import com.mapr.rendezvous.proxy.kafka.TaskSenderService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class GrpcServerService {
    private final TaskSenderService senderService;

    @Value("${grpc.port:50051}")
    private int GRPC_PORT;
    private Server server;

    @PostConstruct
    public void init() throws IOException {
        server = ServerBuilder.forPort(GRPC_PORT)
                .addService(new TaskSenderImpl(senderService))
                .build()
                .start();
    }

    @PreDestroy
    public void shutdown() {
        server.shutdown();
    }
}
