package com.gridcomputing.task_distributor.grpc;

import com.gridcomputing.proto.*;
import com.gridcomputing.task_distributor.registry.NodeRegistry;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@GrpcService
@RequiredArgsConstructor
public class DistributorGrpcService extends DistributorServiceGrpc.DistributorServiceImplBase {

    private final NodeRegistry nodeRegistry;

    @Override
    public void uploadTaskBundle(TaskBundle request, StreamObserver<Ack> responseObserver) {
        log.info("Получен пакет задачи taskId={}, размер={} байт. Рассылаем на {} узлов.",
                request.getTaskId(), request.getJarData().size(), nodeRegistry.getConnections().size());

        int successCount = 0;
        StringBuilder errors = new StringBuilder();

        for (NodeRegistry.NodeConnection node : nodeRegistry.getConnections()) {
            try {
                Ack nodeAck = node.blockingStub().uploadTaskBundle(request);
                if (nodeAck.getSuccess()) {
                    successCount++;
                    log.info("Bundle принят узлом {}", node.nodeId());
                } else {
                    errors.append(node.nodeId()).append(": ").append(nodeAck.getMessage()).append("; ");
                }
            } catch (Exception e) {
                log.error("Ошибка отправки bundle на узлом {}", node.nodeId(), e);
                errors.append(node.nodeId()).append(": ").append(e.getMessage()).append("; ");
            }
        }

        boolean allOk = successCount == nodeRegistry.getConnections().size();
        Ack ack = Ack.newBuilder()
                .setSuccess(allOk)
                .setMessage(allOk
                        ? "Bundle разослан на все " + successCount + " узлы"
                        : "Принято " + successCount + " из " + nodeRegistry.getConnections().size() + ". Ошибки: " + errors)
                .build();

        responseObserver.onNext(ack);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<SubTask> processSubTasks(StreamObserver<SubTaskResult> responseObserver) {
        log.info("Открыт bidirectional поток подзадач");

        AtomicInteger pendingSubtasks = new AtomicInteger(0);
        AtomicBoolean clientCompleted = new AtomicBoolean(false);
        AtomicBoolean responseStreamClosed = new AtomicBoolean(false);
        Object responseLock = new Object();

        return new StreamObserver<>() {
            @Override
            public void onNext(SubTask subTask) {
                pendingSubtasks.incrementAndGet();

                NodeRegistry.NodeConnection node;
                try {
                    node = nodeRegistry.takeFreeNode();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Прерван при ожидании свободного узла для подзадачи {}\"", subTask.getSubtaskId());
                    pendingSubtasks.decrementAndGet();
                    closeResponseStream(pendingSubtasks, clientCompleted,
                            responseStreamClosed, responseObserver, responseLock);
                    return;
                }

                log.info("Подзадача {} направлена на узел {}", subTask.getSubtaskId(), node.nodeId());

                node.asyncStub().executeSubTask(subTask, new StreamObserver<>() {
                    @Override
                    public void onNext(SubTaskResult subTaskResult) {
                        synchronized (responseLock) {
                            responseObserver.onNext(subTaskResult);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.error("Ошибка вызова узла {} для подзадачи {}",
                                node.nodeId(), subTask.getSubtaskId(), throwable);
                        sendErrorResult(subTask.getSubtaskId(),
                                "Узел " + node.nodeId() + ": " + throwable.getMessage(),
                                responseObserver,
                                responseLock);

                        finishSubtask(node);
                    }

                    @Override
                    public void onCompleted() {
                        finishSubtask(node);
                    }

                    private void finishSubtask(NodeRegistry.NodeConnection node) {
                        nodeRegistry.releaseNode(node);
                        pendingSubtasks.decrementAndGet();
                        closeResponseStream(pendingSubtasks, clientCompleted,
                                responseStreamClosed, responseObserver, responseLock);
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Ошибка в потоке от формирователя", throwable);
                clientCompleted.set(true);
            }

            @Override
            public void onCompleted() {
                log.info("Поток подзадач закрыт формирователем, в ожидании: {} подзадач",
                        pendingSubtasks.get());
                clientCompleted.set(true);
                closeResponseStream(pendingSubtasks, clientCompleted,
                        responseStreamClosed, responseObserver, responseLock);
            }
        };
    }

    private void closeResponseStream(AtomicInteger pendingSubtasks,
                                     AtomicBoolean clientCompleted,
                                     AtomicBoolean responseStreamClosed,
                                     StreamObserver<SubTaskResult> responseObserver,
                                     Object responseLock) {
        if (clientCompleted.get() && pendingSubtasks.get() == 0) {
            if (responseStreamClosed.compareAndSet(false, true)) {
                synchronized (responseLock) {
                    responseObserver.onCompleted();
                }
                log.info("Поток ответов закрыт");
            }
        }
    }

    private void sendErrorResult(String subtaskId,
                                 String errorMessage,
                                 StreamObserver<SubTaskResult> responseObserver,
                                 Object responseLock) {
        SubTaskResult errorResult = SubTaskResult.newBuilder()
                .setSubtaskId(subtaskId)
                .setSuccess(false)
                .setMessage(errorMessage)
                .build();
        synchronized (responseLock) {
            responseObserver.onNext(errorResult);
        }

    }
}
