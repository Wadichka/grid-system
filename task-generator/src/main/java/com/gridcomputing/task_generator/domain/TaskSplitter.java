package com.gridcomputing.task_generator.domain;

import java.util.List;

public interface TaskSplitter<T extends Task> {

    List<SubTaskData> split(T task);
    record SubTaskData(String subTaskId, byte[] data){}
}
