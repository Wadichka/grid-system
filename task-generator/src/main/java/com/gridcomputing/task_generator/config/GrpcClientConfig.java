package com.gridcomputing.task_generator.config;

import com.gridcomputing.proto.DistributorServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {
    @Bean
    public DistributorServiceGrpc.DistributorServiceBlockingStub  blockingStub(GrpcChannelFactory grpcChannelFactory) {
        return DistributorServiceGrpc.newBlockingStub(grpcChannelFactory.createChannel("default-channel"));
    }

    @Bean
    public DistributorServiceGrpc.DistributorServiceStub  asyncStub(GrpcChannelFactory grpcChannelFactory) {
        return DistributorServiceGrpc.newStub(grpcChannelFactory.createChannel("default-channel"));
    }
}
