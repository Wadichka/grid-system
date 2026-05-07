package com.gridcomputing.task_generator.taxi;

import java.util.List;

public record TaxiResult(long totalCost, List<int[]> assignment) {

    public record AssignmentPair(int taxiIdx, int passengerIdx) {}

    public List<AssignmentPair> assignmentPairs() {
        return assignment.stream()
                .map(arr -> new AssignmentPair(arr[0], arr[1]))
                .toList();
    }
}