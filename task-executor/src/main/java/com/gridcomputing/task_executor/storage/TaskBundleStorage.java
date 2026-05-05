package com.gridcomputing.task_executor.storage;

import com.google.protobuf.ByteString;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class TaskBundleStorage {

    @Value("${bundle-storage.dir}")
    private String storageDirPath;

    private Path storageDir;

    private final ConcurrentMap<String, BundlePaths> bundles = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws IOException {
        storageDir = Paths.get(storageDirPath).toAbsolutePath().normalize();
        Files.createDirectories(storageDir);
        log.info("Хранилище bundle-файлов инициализировано: {}", storageDir.toAbsolutePath());
    }

    public void save(String taskId, ByteString jarData, ByteString sharedData) throws IOException {
        Path jarPath = storageDir.resolve(taskId + ".jar");
        Path sharedPath = storageDir.resolve(taskId + ".bin");

        Files.write(jarPath, jarData.toByteArray());
        Files.write(sharedPath, sharedData.toByteArray());

        bundles.put(taskId, new BundlePaths(jarPath, sharedPath));
        log.info("Сохранён .jar для taskId={}, размер={} байт", taskId, jarData.size());
    }

    public BundlePaths getPaths(String taskId) {
        BundlePaths paths = bundles.get(taskId);
        if (paths == null) {
            throw new IllegalStateException("Bundle для taskId=" + taskId + " не загружен");
        }
        return paths;
    }

    public boolean has(String taskId) {
        return bundles.containsKey(taskId);
    }

    public record BundlePaths(Path jarPath, Path sharedDataPath) {}
}