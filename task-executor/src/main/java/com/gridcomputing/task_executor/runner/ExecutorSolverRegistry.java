package com.gridcomputing.task_executor.runner;

import com.gridcomputing.api.SubtaskSolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ExecutorSolverRegistry {

    private final ConcurrentHashMap<String, LoadedSolver> solvers = new ConcurrentHashMap<>();

    public void register(String taskId, Path jarPath) {
        try {
            URL jarURL = jarPath.toUri().toURL();
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarURL},
                    SubtaskSolver.class.getClassLoader()
            );

            ServiceLoader<SubtaskSolver> loader = ServiceLoader.load(SubtaskSolver.class, classLoader);
            SubtaskSolver subtaskSolver = loader.findFirst().orElseThrow(() ->
                    new NoSuchElementException("No subtask solver found, check \"META-INF/services/\"")
            );
            solvers.put(taskId, new LoadedSolver(subtaskSolver, classLoader));
            log.info("Зарегистрирован solver для taskId={}: {}", taskId, subtaskSolver.getClass().getName());

        } catch (IOException e) {
            throw new RuntimeException("Error while reading file " + jarPath, e);
        } catch (Exception e) {
            throw new RuntimeException("Can not load solver from " + jarPath, e);
        }
    }

    public byte[] solve(String taskId, byte[] sharedData, byte[] subtaskData) {
        LoadedSolver loadedSolver = solvers.get(taskId);
        if (loadedSolver == null) {
            throw new IllegalStateException("Solver для taskId=" + taskId + " не загружен");
        }
        return loadedSolver.subtaskSolver().solve(sharedData, subtaskData);
    }

    public void unregister(String taskId) {
        LoadedSolver loadedSolver = solvers.get(taskId);
        if (loadedSolver != null) {
            try {
                loadedSolver.classLoader().close();
                log.info("Выгружен solver для taskId={}", taskId);
            } catch (IOException e) {
                log.warn("Ошибка при закрытии classloader для taskId={}", taskId, e);
            }
        }
    }

    private record LoadedSolver(SubtaskSolver subtaskSolver, URLClassLoader classLoader) {}
}
