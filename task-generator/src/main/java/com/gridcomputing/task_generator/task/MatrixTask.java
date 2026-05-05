package com.gridcomputing.task_generator.task;

import com.gridcomputing.task_generator.domain.Task;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;


@Getter
public class MatrixTask implements Task {

    private final String taskId =  UUID.randomUUID().toString();
    private final long[][] matrix;
    private final int n;

    public MatrixTask(long[][] matrix) {
        if (matrix ==  null || matrix.length == 0 || matrix[0].length == 0) {
            throw new IllegalArgumentException("Матрица не должна быть пустой!");
        }
        this.n = matrix.length;
        for (long[] row : matrix) {
            if (row.length != n) {
                throw new IllegalArgumentException("Матрица должна быть квадратной!");
            }
        }
        this.matrix = matrix;
    }

    @Override
    public byte[] getSharedData() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (PrintWriter printWriter = new PrintWriter(
                byteArrayOutputStream, false, StandardCharsets.UTF_8)) {
            printWriter.println(n);
            for (long[] row : matrix) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int j = 0; j < n; j++) {
                    if (j > 0) {
                        stringBuilder.append(' ');
                    }
                    stringBuilder.append(row[j]);
                }
                printWriter.println(stringBuilder);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }
}
