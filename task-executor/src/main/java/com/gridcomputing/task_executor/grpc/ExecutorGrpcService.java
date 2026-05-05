package com.gridcomputing.task_executor.grpc;

import com.google.protobuf.ByteString;
import com.gridcomputing.proto.*;
import com.gridcomputing.task_executor.runner.ExecutorSolverRegistry;
import com.gridcomputing.task_executor.storage.TaskBundleStorage;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.io.IOException;
import java.nio.file.Files;


@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ExecutorGrpcService extends ExecutorServiceGrpc.ExecutorServiceImplBase {

    private final TaskBundleStorage storage;
    private final ExecutorSolverRegistry solverRegistry;

    @Override
    public void uploadTaskBundle(TaskBundle request, StreamObserver<Ack> responseObserver) {
        try {
            storage.save(request.getTaskId(), request.getJarData(), request.getSharedData());

            TaskBundleStorage.BundlePaths paths = storage.getPaths(request.getTaskId());
            solverRegistry.register(request.getTaskId(), paths.jarPath());

            responseObserver.onNext(Ack.newBuilder()
                    .setSuccess(true)
                    .setMessage("Bundle successfully uploaded and solver registered")
                    .build());
        } catch (IOException e) {
            log.error("Error while saving bundle", e);
            responseObserver.onNext(Ack.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error while saving bundle: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error while registering solver", e);
            responseObserver.onNext(Ack.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error while registering solver: " + e.getMessage())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void executeSubTask(SubTask request, StreamObserver<SubTaskResult> responseObserver) {
        log.info("Received request to execute sub-task {}", request.getTaskId());

        SubTaskResult result;
        try {
            TaskBundleStorage.BundlePaths paths = storage.getPaths(request.getTaskId());
            byte[] sharedData = Files.readAllBytes(paths.sharedDataPath());
            byte[] output = solverRegistry.solve(
                    request.getTaskId(),
                    sharedData,
                    request.getInputData().toByteArray()
            );

            result = SubTaskResult.newBuilder()
                    .setSubtaskId(request.getSubtaskId())
                    .setOutputData(ByteString.copyFrom(output))
                    .setSuccess(true)
                    .build();
        } catch (Exception e) {
            log.error("Error while executing sub-task {}", request.getSubtaskId(), e);
            result = SubTaskResult.newBuilder()
                    .setSubtaskId(request.getSubtaskId())
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build();
        }
        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }
}
