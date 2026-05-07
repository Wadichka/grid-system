package com.gridcomputing.task_generator.matrix;

import com.gridcomputing.task_generator.domain.TaskSplitter;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public List<SubTaskData> split(MatrixTask task) {
        int n = task.getN();

        // Генерируем все пары (top, bottom) с условием top <= bottom
        List<int[]> allPairs = new ArrayList<>();
        for (int top = 0; top < n; top++) {
            for (int bottom = top; bottom < n; bottom++) {
                allPairs.add(new int[]{top, bottom});
            }
        }

        // Сортируем по размеру k = bottom - top + 1.
        // Это даёт распределятору пары от мелких к крупным —
        // первыми выполняются быстрые подзадачи, нагрузка размазывается равномернее.
        allPairs.sort(Comparator.comparingInt(p -> p[1] - p[0]));

        // Каждая пара становится отдельной подзадачей
        List<SubTaskData> subtasks = new ArrayList<>();
        for (int[] pair : allPairs) {
            byte[] serialized = serializePair(pair[0], pair[1]);
            subtasks.add(new SubTaskData(UUID.randomUUID().toString(), serialized));
        }

        log.info("Задача разбита на {} подзадач (по одной паре на подзадачу)", subtasks.size());
        return subtasks;
    }

    /**
     * Формат подзадачи: одна строка вида "top bottom"
     */
    private byte[] serializePair(int top, int bottom) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(baos, false, StandardCharsets.UTF_8)) {
            pw.println(top + " " + bottom);
        }
        return baos.toByteArray();
    }
}