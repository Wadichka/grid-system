package com.gridcomputing.task_distributor.registry;

import com.gridcomputing.proto.ExecutorServiceGrpc;
import com.gridcomputing.task_distributor.config.NodesConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class NodeRegistry {

    private static final int MAX_MESSAGE_SIZE = 64 *  1024 * 1024;

    private final NodesConfig nodesConfig;

    @Getter
    private final List<NodeConnection> connections = new ArrayList<>();

    private final BlockingQueue<NodeConnection> freeNodes = new LinkedBlockingQueue<>();


    @PostConstruct
    public void init() {
        for (NodesConfig.Node node : nodesConfig.getNodes()) {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(node.getHost(), node.getPort())
                    .usePlaintext()
                    .maxInboundMessageSize(MAX_MESSAGE_SIZE)
                    .build();

            ExecutorServiceGrpc.ExecutorServiceBlockingStub blockingStub =
                    ExecutorServiceGrpc.newBlockingStub(channel);
            ExecutorServiceGrpc.ExecutorServiceStub asyncStub =
                    ExecutorServiceGrpc.newStub(channel);

            NodeConnection connection = new NodeConnection(node.getId(), channel, blockingStub, asyncStub);
            connections.add(connection);
            freeNodes.offer(connection);

            log.info("Node {} connected on port {}:{}", node.getId(),  node.getHost(), node.getPort());
        }

        if (connections.isEmpty()) {
            log.warn("Nodes list is empty");
        }
    }

    public NodeConnection takeFreeNode() throws InterruptedException {
        return freeNodes.take();
    }

    public void releaseNode(NodeConnection connection) {
        freeNodes.offer(connection);
    }

    @PreDestroy
    public void shutdown() {
        for (NodeConnection connection : connections) {
            try {
                connection.channel().shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    public record NodeConnection (
            String nodeId,
            ManagedChannel channel,
            ExecutorServiceGrpc.ExecutorServiceBlockingStub blockingStub,
            ExecutorServiceGrpc.ExecutorServiceStub asyncStub
    ) {}
}
