package com.gridcomputing.task_generator.taxi;

import com.gridcomputing.task_generator.domain.TaskSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class TaxiTaskSplitter implements TaskSplitter<TaxiTask> {

    @Override
    public List<SubTaskData> split(TaxiTask task) {
        List<SubTaskData> subtasks = new ArrayList<>();

        // Каждая подзадача фиксирует первое назначение taxi[0] → passenger[i]
        for (int passengerIdx = 0; passengerIdx < task.getY(); passengerIdx++) {
            byte[] serialized = serializePrefix(0, passengerIdx);
            subtasks.add(new SubTaskData(UUID.randomUUID().toString(), serialized));
        }

        log.info("Задача разбита на {} подзадач (по одному назначению taxi[0] → passenger[i])",
                subtasks.size());
        return subtasks;
    }

    /**
     * Формат подзадачи:
     *   k                       — длина префикса (тут всегда 1)
     *   taxi_idx passenger_idx  — фиксированное назначение
     */
    private byte[] serializePrefix(int taxiIdx, int passengerIdx) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(baos, false, StandardCharsets.UTF_8)) {
            pw.println(1);
            pw.println(taxiIdx + " " + passengerIdx);
        }
        return baos.toByteArray();
    }
}