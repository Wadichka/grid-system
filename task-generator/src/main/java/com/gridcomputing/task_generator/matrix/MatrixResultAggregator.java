package com.gridcomputing.task_generator.matrix;

import com.gridcomputing.task_generator.domain.ResultAggregator;
import com.gridcomputing.task_generator.domain.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class MatrixResultAggregator implements ResultAggregator<MatrixTask, MatrixResult> {

    @Override
    public MatrixResult aggregate(MatrixTask task, List<PartialResult> partialResults) {
        if (partialResults.isEmpty()) {
            throw new IllegalStateException("Нет результатов для агрегации!");
        }

        MatrixResult best = null;

        for (PartialResult partialResult : partialResults) {
            String text = new String(partialResult.data(), StandardCharsets.UTF_8).trim();
            String[] parts = text.split("\\s+");

            if (parts.length != 4) {
                throw new IllegalStateException("Неверный формат результата: '" + text + "'");
            }

            long sum = Long.parseLong(parts[0]);
            int topLeftRow = Integer.parseInt(parts[1]);
            int topLeftCol = Integer.parseInt(parts[2]);
            int size = Integer.parseInt(parts[3]);

            if (best == null || sum > best.sum()) {
                best = new MatrixResult(sum, topLeftRow, topLeftCol, size);
            }
        }

        log.info("Итоговый результат: maxSum={}, position=({},{}), size={}",
                best.sum(), best.topLeftRow(), best.topLeftColumn(), best.size());
        return best;
    }
}
