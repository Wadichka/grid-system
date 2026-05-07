package com.gridcomputing.task_generator.service;

import com.gridcomputing.task_generator.domain.ResultAggregator;
import com.gridcomputing.task_generator.domain.Task;
import com.gridcomputing.task_generator.domain.TaskSplitter;
import com.gridcomputing.task_generator.grpc.DistributorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final DistributorClient distributorClient;

    public <T extends Task, R> R process(T task, byte[] jarBytes,
                                         TaskSplitter<T> splitter,
                                         ResultAggregator<T, R> aggregator)
            throws ExecutionException, InterruptedException {

        log.info("Начало обработки задачи taskId={}", task.getTaskId());

        distributorClient.uploadBundle(task.getTaskId(), jarBytes, task.getSharedData());

        List<TaskSplitter.SubTaskData> subtasks = splitter.split(task);
        log.info("Задача разбита на {} подзадач", subtasks.size());

        List<ResultAggregator.PartialResult> results = distributorClient
                .processSubTasks(task.getTaskId(), subtasks)
                .get();

        R finalResult = aggregator.aggregate(task, results);
        log.info("Задача {} завершена, результат: {}", task.getTaskId(), finalResult);
        return finalResult;
    }
}
