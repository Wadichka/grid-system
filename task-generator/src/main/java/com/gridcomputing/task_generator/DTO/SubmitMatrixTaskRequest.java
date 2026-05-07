package com.gridcomputing.task_generator.DTO;

public record SubmitMatrixTaskRequest(String jarId, long[][] matrix) {
}
