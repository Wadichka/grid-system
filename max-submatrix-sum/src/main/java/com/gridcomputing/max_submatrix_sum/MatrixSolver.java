package com.gridcomputing.max_submatrix_sum;

import com.gridcomputing.api.SubtaskSolver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

public class MatrixSolver implements SubtaskSolver {

    @Override
    public byte[] solve(byte[] sharedData, byte[] subtaskData) {
        try {
            long[][] matrix = parseMatrix(sharedData);
            int n = matrix.length;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(subtaskData), StandardCharsets.UTF_8));
            int p = Integer.parseInt(reader.readLine().trim());

            long bestSum = Long.MIN_VALUE;
            int bestTop = -1;
            int bestLeft = -1;
            int bestK = -1;

            for (int i = 0; i < p; i++) {
                StringTokenizer st = new StringTokenizer(reader.readLine());
                int top = Integer.parseInt(st.nextToken());
                int bottom = Integer.parseInt(st.nextToken());
                int k = bottom - top + 1;

                Result result = findBestSquareForRowRange(matrix, n, top, bottom);

                if (result.sum() > bestSum) {
                    bestSum = result.sum();
                    bestTop = top;
                    bestLeft = result.left();
                    bestK = k;
                }
            }

            String output = bestSum + " " + bestTop + " " + bestLeft + " " + bestK;
            return output.getBytes(StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка парсинга данных", e);
        }
    }

    private long[][] parseMatrix(byte[] sharedData) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(sharedData), StandardCharsets.UTF_8));
        int n = Integer.parseInt(reader.readLine().trim());
        long[][] matrix = new long[n][n];
        for (int i = 0; i < n; i++) {
            StringTokenizer st = new StringTokenizer(reader.readLine());
            for (int j = 0; j < n; j++) {
                matrix[i][j] = Long.parseLong(st.nextToken());
            }
        }
        return matrix;
    }

    private Result findBestSquareForRowRange(long[][] matrix, int n, int top, int bottom) {
        int k = bottom - top + 1;

        long[] columnSum = new long[n];
        for (int j = 0; j < n; j++) {
            long sum = 0;
            for (int i = top; i <= bottom; i++) {
                sum += matrix[i][j];
            }
            columnSum[j] = sum;
        }

        long windowSum = 0;
        for (int j = 0; j < k; j++) {
            windowSum += columnSum[j];
        }
        long bestSum = windowSum;
        int bestLeft = 0;

        for (int j = k; j < n; j++) {
            windowSum += columnSum[j] - columnSum[j - k];
            if (windowSum > bestSum) {
                bestSum = windowSum;
                bestLeft = j - k + 1;
            }
        }

        return new Result(bestSum, bestLeft);
    }

    private record Result(long sum, int left) {}
}