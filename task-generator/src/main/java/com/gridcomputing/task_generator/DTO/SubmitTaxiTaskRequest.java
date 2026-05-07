package com.gridcomputing.task_generator.DTO;

public record SubmitTaxiTaskRequest(
        String jarId,
        long[][] graph,
        int[] taxis,
        int[] passengers,
        int[] destinations
) {}