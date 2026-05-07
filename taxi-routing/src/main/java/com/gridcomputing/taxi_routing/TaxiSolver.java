package com.gridcomputing.taxi_routing;

import com.gridcomputing.api.SubtaskSolver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.StringTokenizer;


public class TaxiSolver implements SubtaskSolver {

    private static final long INF = Long.MAX_VALUE / 4;

    @Override
    public byte[] solve(byte[] sharedData, byte[] subtaskData) {
        try {
            ProblemData data = parseShared(sharedData);
            long[][] dist = floydWarshall(data.graph, data.v);
            long[][] cost = buildCostMatrix(dist, data.taxis, data.passengers);

            BacktrackState state = applyPrefix(subtaskData, data.x, data.y, cost);

            backtrack(state.assignment, state.usedPassengers, state.currentCost,
                    0, data.x, data.y, cost, state.bestAssignment, state.bestCost);

            return formatResult(state.bestAssignment, state.bestCost[0], data.x);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка парсинга данных", e);
        }
    }

    private ProblemData parseShared(byte[] sharedData) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(sharedData), StandardCharsets.UTF_8));

        int v = Integer.parseInt(reader.readLine().trim());

        long[][] graph = new long[v][v];
        for (int i = 0; i < v; i++) {
            StringTokenizer st = new StringTokenizer(reader.readLine());
            for (int j = 0; j < v; j++) {
                long val = Long.parseLong(st.nextToken());
                graph[i][j] = (val < 0) ? INF : val;
            }
        }

        int x = Integer.parseInt(reader.readLine().trim());
        int[] taxis = readIntArray(reader.readLine(), x);

        int y = Integer.parseInt(reader.readLine().trim());
        int[] passengers = readIntArray(reader.readLine(), y);
        int[] destinations = readIntArray(reader.readLine(), y); // не используем для оптимизации

        return new ProblemData(v, graph, x, taxis, y, passengers, destinations);
    }

    private int[] readIntArray(String line, int n) {
        int[] arr = new int[n];
        StringTokenizer st = new StringTokenizer(line);
        for (int i = 0; i < n; i++) {
            arr[i] = Integer.parseInt(st.nextToken());
        }
        return arr;
    }

    private long[][] floydWarshall(long[][] graph, int v) {
        long[][] dist = new long[v][v];
        for (int i = 0; i < v; i++) {
            dist[i] = graph[i].clone();
            dist[i][i] = 0;
        }
        for (int k = 0; k < v; k++) {
            for (int i = 0; i < v; i++) {
                for (int j = 0; j < v; j++) {
                    if (dist[i][k] < INF && dist[k][j] < INF) {
                        long alt = dist[i][k] + dist[k][j];
                        if (alt < dist[i][j]) {
                            dist[i][j] = alt;
                        }
                    }
                }
            }
        }
        return dist;
    }

    private long[][] buildCostMatrix(long[][] dist, int[] taxis, int[] passengers) {
        long[][] cost = new long[taxis.length][passengers.length];
        for (int i = 0; i < taxis.length; i++) {
            for (int j = 0; j < passengers.length; j++) {
                cost[i][j] = dist[taxis[i]][passengers[j]];
            }
        }
        return cost;
    }

    private BacktrackState applyPrefix(byte[] subtaskData, int x, int y, long[][] cost) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(subtaskData), StandardCharsets.UTF_8));

        int k = Integer.parseInt(reader.readLine().trim());

        int[] assignment = new int[x];
        Arrays.fill(assignment, -1);

        boolean[] usedPassengers = new boolean[y];
        long currentCost = 0;

        for (int i = 0; i < k; i++) {
            StringTokenizer st = new StringTokenizer(reader.readLine());
            int taxiIdx = Integer.parseInt(st.nextToken());
            int passIdx = Integer.parseInt(st.nextToken());
            assignment[taxiIdx] = passIdx;
            usedPassengers[passIdx] = true;
            currentCost += cost[taxiIdx][passIdx];
        }

        int[] bestAssignment = new int[x];
        long[] bestCost = {INF};

        return new BacktrackState(assignment, usedPassengers, currentCost, bestAssignment, bestCost);
    }

    private void backtrack(int[] currentAssignment, boolean[] usedPassengers, long currentCost,
                           int taxiIdx, int x, int y, long[][] cost,
                           int[] bestAssignment, long[] bestCost) {
        while (taxiIdx < x && currentAssignment[taxiIdx] >= 0) {
            taxiIdx++;
        }

        if (taxiIdx >= x) {
            if (currentCost < bestCost[0]) {
                bestCost[0] = currentCost;
                System.arraycopy(currentAssignment, 0, bestAssignment, 0, x);
            }
            return;
        }

        if (currentCost >= bestCost[0]) {
            return;
        }

        for (int p = 0; p < y; p++) {
            if (!usedPassengers[p] && cost[taxiIdx][p] < INF) {
                currentAssignment[taxiIdx] = p;
                usedPassengers[p] = true;

                backtrack(currentAssignment, usedPassengers, currentCost + cost[taxiIdx][p],
                        taxiIdx + 1, x, y, cost, bestAssignment, bestCost);

                currentAssignment[taxiIdx] = -1;
                usedPassengers[p] = false;
            }
        }
    }

    private byte[] formatResult(int[] bestAssignment, long bestCost, int x) {
        StringBuilder sb = new StringBuilder();
        if (bestCost >= INF) {
            sb.append(INF).append("\n").append(0).append("\n");
        } else {
            sb.append(bestCost).append("\n");
            int count = 0;
            for (int i = 0; i < x; i++) {
                if (bestAssignment[i] >= 0) count++;
            }
            sb.append(count).append("\n");
            for (int i = 0; i < x; i++) {
                if (bestAssignment[i] >= 0) {
                    sb.append(i).append(" ").append(bestAssignment[i]).append("\n");
                }
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private record ProblemData(int v, long[][] graph, int x, int[] taxis,
                               int y, int[] passengers, int[] destinations) {}

    private record BacktrackState(int[] assignment, boolean[] usedPassengers,
                                  long currentCost, int[] bestAssignment, long[] bestCost) {}
}