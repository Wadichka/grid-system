package com.gridcomputing.task_generator.controller;

import com.gridcomputing.task_generator.DTO.SubmitTuskRequest;
import com.gridcomputing.task_generator.service.TaskService;
import com.gridcomputing.task_generator.storage.JarStorage;
import com.gridcomputing.task_generator.task.MatrixTask;
import com.gridcomputing.task_generator.task.MatrixResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final JarStorage jarStorage;

    @PostMapping("/api/jars")
    public ResponseEntity<?> uploadJar(@RequestParam("jarFile") MultipartFile file) {
        try {
            String jarId = jarStorage.save(file.getBytes());
            return ResponseEntity.ok().body(Map.of(
                    "jarId", jarId,
                    "size", file.getSize()
                    ));
        } catch (IOException e) {
            log.error("Ошибка сохранения .jar файла", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/tasks")
    public ResponseEntity<?> submitTask(@RequestBody SubmitTuskRequest request) {
        try {
            if (!jarStorage.exists(request.jarId())) {
                return ResponseEntity.badRequest().body("Jar с id="
                        + request.jarId()
                        + " не найден. Загрузите .jar через POST /api/jars.");
            }

            byte[] jarBytes = jarStorage.read(request.jarId());
            MatrixTask task = new MatrixTask(request.matrix());
            MatrixResult result = taskService.process(task, jarBytes);

            return ResponseEntity.ok().body(Map.of(
                    "taskId", task.getTaskId(),
                    "matrixSize", task.getN(),
                    "maxSum", result.sum(),
                    "topLeftRow", result.topLeftRow(),
                    "topLeftCol", result.topLeftColumn(),
                    "size", result.size()
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}