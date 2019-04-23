package com.mapr.rendezvous.proxy.grpc;

import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;

@GRpcService
public class GrpcServer extends TaskSenderGrpc.TaskSenderImplBase {

    @Override
    public void sendTask(Task.TaskRequest request, StreamObserver<Task.TaskResponse> responseObserver) {
        final Task.TaskResponse.Builder replyBuilder = Task.TaskResponse.newBuilder();
        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }
}
