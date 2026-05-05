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
            BufferedReader sharedReader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(sharedData), StandardCharsets.UTF_8)
            );

            int n = Integer.parseInt(sharedReader.readLine().trim());

            long[][] matrix = new long[n][n];
            for (int i = 0; i < n; i++) {
                StringTokenizer tokenizer = new StringTokenizer(sharedReader.readLine().trim());
                for (int j = 0; j < n; j++) {
                    matrix[i][j] = Long.parseLong(tokenizer.nextToken());
                }
            }

            BufferedReader subtaskReader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(subtaskData), StandardCharsets.UTF_8)
            );
            StringTokenizer tokenizer = new StringTokenizer(subtaskReader.readLine().trim());
            int top = Integer.parseInt(tokenizer.nextToken());
            int bottom = Integer.parseInt(tokenizer.nextToken());

            Result result = findBestSquareForRowRange(matrix, n, top, bottom);
            int k = bottom - top + 1;

            String response = result.sum + " " + top + " " + result.left + " " + k;
            return response.getBytes(StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения входных данных", e);
        }
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