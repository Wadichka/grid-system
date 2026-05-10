package com.gridcomputing.task_generator.matrix;

import com.gridcomputing.task_generator.domain.TaskSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class MatrixTaskSplitter implements TaskSplitter<MatrixTask> {

    @Value("${batching.size:16}")
    private int batchSize;

    @Override
    public List<SubTaskData> split(MatrixTask task) {
        int n = task.getN();

        // Генерируем все пары (top, bottom) с их сложностями
        List<Pair> allPairs = new ArrayList<>();
        for (int top = 0; top < n; top++) {
            for (int bottom = top; bottom < n; bottom++) {
                int complexity = bottom - top + 1;  // около k операций суммирования на столбец
                allPairs.add(new Pair(top, bottom, complexity));
            }
        }

        allPairs.sort(Comparator.comparingInt(Pair::complexity).reversed());

        int totalPairs = allPairs.size();
        int numBatches = (totalPairs + batchSize - 1) / batchSize;  // ceil(total / batch)

        // Распределяем пары по батчам через snake distribution
        List<List<Pair>> batches = new ArrayList<>();
        for (int i = 0; i < numBatches; i++) {
            batches.add(new ArrayList<>());
        }

        for (int i = 0; i < totalPairs; i++) {
            int wave = i / numBatches;
            int positionInWave = i % numBatches;
            int batchIdx = (wave % 2 == 0)
                    ? positionInWave
                    : numBatches - 1 - positionInWave;

            batches.get(batchIdx).add(allPairs.get(i));
        }

        List<SubTaskData> subtasks = new ArrayList<>();
        for (List<Pair> batch : batches) {
            byte[] serialized = serializeBatch(batch);
            subtasks.add(new SubTaskData(UUID.randomUUID().toString(), serialized));
        }

        log.info("Задача разбита на {} подзадач (batch_size={}, всего пар={})",
                subtasks.size(), batchSize, totalPairs);

        for (int i = 0; i < batches.size(); i++) {
            int totalComplexity = batches.get(i).stream()
                    .mapToInt(Pair::complexity).sum();
            log.debug("Батч {}: {} пар, суммарная сложность {}",
                    i, batches.get(i).size(), totalComplexity);
        }

        return subtasks;
    }


    private byte[] serializeBatch(List<Pair> batch) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(baos, false, StandardCharsets.UTF_8)) {
            pw.println(batch.size());
            for (Pair pair : batch) {
                pw.println(pair.top() + " " + pair.bottom());
            }
        }
        return baos.toByteArray();
    }


    private record Pair(int top, int bottom, int complexity) {}
}