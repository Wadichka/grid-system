package com.gridcomputing.task_generator.controller;

import com.gridcomputing.task_generator.DTO.SubmitTaxiTaskRequest;
import com.gridcomputing.task_generator.DTO.SubmitMatrixTaskRequest;
import com.gridcomputing.task_generator.service.TaskService;
import com.gridcomputing.task_generator.storage.JarStorage;
import com.gridcomputing.task_generator.matrix.MatrixResultAggregator;
import com.gridcomputing.task_generator.matrix.MatrixTask;
import com.gridcomputing.task_generator.matrix.MatrixResult;
import com.gridcomputing.task_generator.matrix.MatrixTaskSplitter;
import com.gridcomputing.task_generator.taxi.TaxiResult;
import com.gridcomputing.task_generator.taxi.TaxiResultAggregator;
import com.gridcomputing.task_generator.taxi.TaxiTask;
import com.gridcomputing.task_generator.taxi.TaxiTaskSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final JarStorage jarStorage;

    private final MatrixTaskSplitter matrixSplitter;
    private final MatrixResultAggregator matrixAggregator;

    private final TaxiTaskSplitter taxiSplitter;
    private final TaxiResultAggregator taxiAggregator;


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

    @PostMapping("/api/tasks/matrix")
    public ResponseEntity<?> submitMatrixTask(@RequestBody SubmitMatrixTaskRequest request) {
        try {
            if (!jarStorage.exists(request.jarId())) {
                return ResponseEntity.badRequest().body("Jar с id="
                        + request.jarId()
                        + " не найден. Загрузите .jar через POST /api/jars.");
            }

            byte[] jarBytes = jarStorage.read(request.jarId());
            MatrixTask task = new MatrixTask(request.matrix());
            MatrixResult result = taskService.process(task, jarBytes, matrixSplitter, matrixAggregator);

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

    @PostMapping("/api/tasks/taxi")
    public ResponseEntity<?> submitTaxiTask(@RequestBody SubmitTaxiTaskRequest request) {
        try {
            if (!jarStorage.exists(request.jarId())) {
                return ResponseEntity.badRequest().body("Jar с id="
                        + request.jarId()
                        + " не найден. Загрузите .jar через POST /api/jars.");
            }
            byte[] jarBytes = jarStorage.read(request.jarId());
            TaxiTask task = new TaxiTask(
                    request.graph(),
                    request.taxis(),
                    request.passengers(),
                    request.destinations()
            );

            TaxiResult result = taskService.process(task, jarBytes, taxiSplitter, taxiAggregator);

            // Преобразуем в JSON-совместимый формат
            List<Map<String, Integer>> assignmentJson = result.assignment().stream()
                    .map(pair -> {
                        Map<String, Integer> map = new HashMap<>();
                        map.put("taxiIdx", pair[0]);
                        map.put("passengerIdx", pair[1]);
                        return map;
                    })
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "taskId", task.getTaskId(),
                    "taxiCount", task.getX(),
                    "passengerCount", task.getY(),
                    "totalCost", result.totalCost(),
                    "assignment", assignmentJson
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Ошибка обработки задачи такси", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}