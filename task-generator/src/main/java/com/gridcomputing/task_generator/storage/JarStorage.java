package com.gridcomputing.task_generator.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class JarStorage {

    @Value("${jar-storage.dir:./task-generator/jars}")
    private String jarsDirPath;

    private Path storageDir;

    private final ConcurrentHashMap<String, Path> jars = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws IOException {
        storageDir = Paths.get(jarsDirPath).toAbsolutePath().normalize();
        Files.createDirectories(storageDir);
        log.info("Хранилище .jar файлов: {}", storageDir);

        try (var stream = Files.list(storageDir)) {
            stream.filter(p -> p.toString().endsWith(".jar"))
                    .forEach(p -> {
                        String fileName = p.getFileName().toString();
                        String jarId  = fileName.substring(0, fileName.length() - 4);
                        jars.put(jarId, p);
                    });
        }
        if (!jars.isEmpty()) {
            log.info("Восстановлено {} ранее загруженных .jar файлов", jars.size());
        }
    }

    public String save(byte[] jarBytes) throws IOException {
        String jarId = UUID.randomUUID().toString();
        Path jarPath = storageDir.resolve(jarId + ".jar");
        Files.write(jarPath, jarBytes);
        jars.put(jarId, jarPath);
        log.info("Сохранён .jar: jarId={}, размер={} байт", jarId, jarBytes.length);
        return jarId;
    }

    public byte[] read(String jarId) throws IOException {
        Path jarPath = jars.get(jarId);
        if (jarPath == null) {
            throw new IllegalArgumentException("Jar с id={}" + jarId + " не найден");
        }
        return Files.readAllBytes(jarPath);
    }

    public boolean exists(String jarId) {
        return  Files.exists(jars.get(jarId));
    }
}
