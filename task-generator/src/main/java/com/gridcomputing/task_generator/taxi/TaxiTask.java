package com.gridcomputing.task_generator.taxi;

import com.gridcomputing.task_generator.domain.Task;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Задача о назначении такси: дан граф городов (матрица смежности),
 * позиции X такси и Y пассажиров с точками назначения.
 * Нужно найти оптимальное назначение такси на пассажиров с минимальной
 * суммарной стоимостью холостого пробега.
 */
@Getter
public class TaxiTask implements Task {

    private final String taskId = UUID.randomUUID().toString();
    private final long[][] graph;
    private final int[] taxis;
    private final int[] passengers;
    private final int[] destinations;
    private final int v;
    private final int x;
    private final int y;

    public TaxiTask(long[][] graph, int[] taxis, int[] passengers, int[] destinations) {
        if (graph == null || graph.length == 0) {
            throw new IllegalArgumentException("Граф не может быть пустым");
        }
        this.v = graph.length;
        for (long[] row : graph) {
            if (row.length != v) {
                throw new IllegalArgumentException("Матрица смежности должна быть квадратной");
            }
        }
        if (taxis == null || taxis.length == 0) {
            throw new IllegalArgumentException("Должен быть хотя бы один такси");
        }
        if (passengers == null || passengers.length == 0) {
            throw new IllegalArgumentException("Должен быть хотя бы один пассажир");
        }
        if (destinations == null || destinations.length != passengers.length) {
            throw new IllegalArgumentException(
                    "Количество точек назначения должно совпадать с количеством пассажиров");
        }
        if (taxis.length > passengers.length) {
            throw new IllegalArgumentException(
                    "Текущая реализация поддерживает только X <= Y (такси не больше чем пассажиров)");
        }

        this.graph = graph;
        this.taxis = taxis;
        this.passengers = passengers;
        this.destinations = destinations;
        this.x = taxis.length;
        this.y = passengers.length;
    }

    @Override
    public byte[] getSharedData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(baos, false, StandardCharsets.UTF_8)) {
            pw.println(v);
            for (long[] row : graph) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < v; j++) {
                    if (j > 0) sb.append(' ');
                    sb.append(row[j]);
                }
                pw.println(sb);
            }
            pw.println(x);
            pw.println(intArrayToString(taxis));
            pw.println(y);
            pw.println(intArrayToString(passengers));
            pw.println(intArrayToString(destinations));
        }
        return baos.toByteArray();
    }

    private String intArrayToString(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(arr[i]);
        }
        return sb.toString();
    }
}