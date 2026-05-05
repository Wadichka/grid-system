package com.gridcomputing.task_distributor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
public class TaskDistributorApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskDistributorApplication.class, args);
	}

}
