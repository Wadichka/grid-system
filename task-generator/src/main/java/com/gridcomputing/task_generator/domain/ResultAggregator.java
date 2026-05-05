package com.gridcomputing.task_generator.domain;

import java.util.List;

public interface ResultAggregator<T extends Task, R> {

    R aggregate(T task, List<PartialResult> partialResults);
    record PartialResult(String taskId, byte[] data) {}
}
