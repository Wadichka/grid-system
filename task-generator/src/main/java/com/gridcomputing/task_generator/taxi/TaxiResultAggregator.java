package com.gridcomputing.task_generator.taxi;

import com.gridcomputing.task_generator.domain.ResultAggregator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

@Slf4j
@Component
public class TaxiResultAggregator implements ResultAggregator<TaxiTask, TaxiResult> {

    @Override
    public TaxiResult aggregate(TaxiTask task, List<PartialResult> partialResults) {
        if (partialResults.isEmpty()) {
            throw new IllegalStateException("Нет результатов для агрегации");
        }

        TaxiResult best = null;

        for (PartialResult pr : partialResults) {
            try {
                TaxiResult parsed = parseResult(pr.data());
                if (best == null || parsed.totalCost() < best.totalCost()) {
                    best = parsed;
                }
            } catch (IOException e) {
                throw new IllegalStateException("Ошибка парсинга результата подзадачи", e);
            }
        }

        log.info("Лучшее назначение: стоимость={}, размер={}",
                best.totalCost(), best.assignment().size());
        return best;
    }

    private TaxiResult parseResult(byte[] outputData) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(outputData), StandardCharsets.UTF_8));

        long totalCost = Long.parseLong(reader.readLine().trim());
        int size = Integer.parseInt(reader.readLine().trim());

        List<int[]> assignment = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            StringTokenizer st = new StringTokenizer(reader.readLine());
            int taxiIdx = Integer.parseInt(st.nextToken());
            int passIdx = Integer.parseInt(st.nextToken());
            assignment.add(new int[]{taxiIdx, passIdx});
        }

        return new TaxiResult(totalCost, assignment);
    }
}