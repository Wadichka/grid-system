package com.gridcomputing.task_generator.taxi;

import com.gridcomputing.task_generator.domain.TaskSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaxiTaskSplitter implements TaskSplitter<TaxiTask> {

    @Value("${batching.size:16}")
    private int batchSize;

    @Override
    public List<SubTaskData> split(TaxiTask task) {
        int totalPrefixes = task.getY();

        int numBatches = (totalPrefixes + batchSize - 1) / batchSize;

        List<List<Integer>> batches = new ArrayList<>();
        for (int i = 0; i < numBatches; i++) {
            batches.add(new ArrayList<>());
        }

        for (int passengerIdx = 0; passengerIdx < totalPrefixes; passengerIdx++) {
            int wave = passengerIdx / numBatches;
            int positionInWave = passengerIdx % numBatches;
            int batchIdx = (wave % 2 == 0)
                    ? positionInWave
                    : numBatches - 1 - positionInWave;
            batches.get(batchIdx).add(passengerIdx);
        }

        List<SubTaskData> subtasks = new ArrayList<>();
        for (List<Integer> batch : batches) {
            byte[] serialized = serializeBatch(batch);
            subtasks.add(new SubTaskData(UUID.randomUUID().toString(), serialized));
        }

        log.info("Taxi задача разбита на {} подзадач (batch_size={}, всего префиксов={})",
                subtasks.size(), batchSize, totalPrefixes);
        return subtasks;
    }

    /**
     * Формат батча:
     *   P                              — количество префиксов в батче
     *   1                              — длина каждого префикса (всегда 1)
     *   taxi_idx_1 passenger_idx_1     — первый префикс
     *   1
     *   taxi_idx_2 passenger_idx_2
     *   ...
     */
    private byte[] serializeBatch(List<Integer> passengerIndices) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(baos, false, StandardCharsets.UTF_8)) {
            pw.println(passengerIndices.size());
            for (int passengerIdx : passengerIndices) {
                pw.println(1);  // длина префикса
                pw.println("0 " + passengerIdx);
            }
        }
        return baos.toByteArray();
    }
}