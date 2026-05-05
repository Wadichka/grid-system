package com.gridcomputing.task_generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@ImportGrpcClients(basePackages = "com.gridcomputing.proto")
public class TaskGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskGeneratorApplication.class, args);
	}

}
