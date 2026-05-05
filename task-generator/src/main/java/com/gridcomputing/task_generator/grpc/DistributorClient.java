package com.gridcomputing.task_generator.grpc;

import com.google.protobuf.ByteString;
import com.gridcomputing.proto.*;
import com.gridcomputing.task_generator.domain.ResultAggregator;
import com.gridcomputing.task_generator.domain.TaskSplitter;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributorClient {

    private final DistributorServiceGrpc.DistributorServiceBlockingStub blockingStub;
    private final DistributorServiceGrpc.DistributorServiceStub asyncStub;

    public Ack uploadBundle(String tackId, byte[] jarBytes, byte[] sharedData) {
        log.info("Загрузка bundle для для taskId={}, размер={} байт", tackId, jarBytes.length);
        return blockingStub.uploadTaskBundle(TaskBundle.newBuilder()
                .setTaskId(tackId)
                .setJarData(ByteString.copyFrom(jarBytes))
                .setSharedData(ByteString.copyFrom(sharedData))
                .build());
    }

    public CompletableFuture<List<ResultAggregator.PartialResult>> processSubTasks(
            String tackId, List<TaskSplitter.SubTaskData> subtasks) {
        CompletableFuture<List<ResultAggregator.PartialResult>> future = new CompletableFuture<>();
        List<ResultAggregator.PartialResult> collected = new CopyOnWriteArrayList<>();

        StreamObserver<SubTask> requestObserver = asyncStub.processSubTasks(new StreamObserver<>() {

            @Override
            public void onNext(SubTaskResult result) {
                if (result.getSuccess()) {
                    collected.add(new ResultAggregator.PartialResult(
                            result.getSubtaskId(),
                            result.getOutputData().toByteArray()
                    ));
                    log.info("Получен результат подзадачи {} ({}/{})", result.getSubtaskId(),
                            collected.size(), subtasks.size());
                } else {
                    log.error("Подзадача {} провалилась: {}", result.getSubtaskId(), result.getMessage());
                    future.completeExceptionally(
                            new RuntimeException("Подзадача провалилась: " + result.getMessage()));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Ошибка в потоке результатов", throwable);
                future.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
                log.info("Распределятор закрыл поток результатов, всего получено: {}", collected.size());
                future.complete(collected);
            }
        });

        for (TaskSplitter.SubTaskData data : subtasks) {
            requestObserver.onNext(SubTask.newBuilder()
                    .setTaskId(tackId)
                    .setSubtaskId(data.subTaskId())
                    .setInputData(ByteString.copyFrom(data.data()))
                    .build());
        }
        requestObserver.onCompleted();

        return future;
    }
}
