package com.mapr.rendezvous.proxy.grpc;

import com.mapr.rendezvous.commons.kafka.entity.TaskRequest;
import com.mapr.rendezvous.commons.kafka.entity.TaskResponse;
import com.mapr.rendezvous.proxy.kafka.TaskSenderService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeoutException;

@Slf4j
public class TaskSenderImpl extends TaskSenderGrpc.TaskSenderImplBase {
    private final TaskSenderService senderService;

    public TaskSenderImpl(TaskSenderService senderService) {
        super();
        this.senderService = senderService;
    }

    @Override
    public void sendTask(Task.TaskRequest request, StreamObserver<Task.TaskResponse> responseObserver) {
      senderService.sendAndReceive(convertToKafkaTaskRequest(request))
        .doOnError(error -> {
          if(error instanceof TimeoutException) {
            log.error("Failed to send");
            responseObserver.onError(Status.ABORTED
                                       .withDescription(error.getMessage())
                                       .withCause(error)
                                       .asRuntimeException());
          } else {
            log.error("Failed to send", error);
            responseObserver.onError((Status.UNKNOWN
              .withDescription(error.getMessage())
              .withCause(error)
              .asRuntimeException()));
          }
        })
        .subscribe(response -> {
          responseObserver.onNext(convertToGrpcTaskResponse(response));
          responseObserver.onCompleted();
        });
    }

    private TaskRequest convertToKafkaTaskRequest(Task.TaskRequest request) {
        return TaskRequest.builder()
          .modelId(request.getModelId())
          .modelClass(request.getModelClass())
          .timeout(request.getTimeout())
          .build();
    }

    private  Task.TaskResponse convertToGrpcTaskResponse(TaskResponse response) {
      return Task.TaskResponse.newBuilder()
        .setModelId(response.getModelId())
        .setModelClass(response.getModelClass())
        .setRequestId(response.getRequestId())
        .setAccuracy(response.getAccuracy())
        .setResult(response.getResult())
        .build();
    }
}
