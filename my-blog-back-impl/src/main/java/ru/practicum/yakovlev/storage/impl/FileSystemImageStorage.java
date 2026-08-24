package ru.practicum.yakovlev.storage.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.yakovlev.exception.ImageStorageException;
import ru.practicum.yakovlev.storage.ImageStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
@Slf4j
public class FileSystemImageStorage implements ImageStorage {

    private final Path rootDirectory;

    public FileSystemImageStorage(@Value("${blog.image-storage.directory}") String directory) {
        this.rootDirectory = Path.of(directory).toAbsolutePath().normalize();
        log.info("Image storage initialized: rootDirectory={}", rootDirectory);
    }

    @Override
    public String save(Long postId, String originalFilename, byte[] content) {
        String filename = sanitizeFilename(originalFilename);
        String storageKey = postId + "/" + UUID.randomUUID() + "_" + filename;
        Path target = resolveInsideRoot(Path.of(storageKey));
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            log.info("Image saved: postId={}, path={}, sizeBytes={}", postId, target, content.length);
            return storageKey;
        } catch (IOException exception) {
            throw new ImageStorageException("Failed to save image for post " + postId, exception);
        }
    }

    @Override
    public byte[] read(String relativePath) {
        Path target = resolveInsideRoot(Path.of(relativePath));
        try {
            byte[] content = Files.readAllBytes(target);
            log.debug("Image loaded: path={}, sizeBytes={}", target, content.length);
            return content;
        } catch (IOException exception) {
            throw new ImageStorageException("Failed to read image " + relativePath, exception);
        }
    }

    @Override
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path target = resolveInsideRoot(Path.of(relativePath));
            if (Files.deleteIfExists(target)) {
                log.info("Image deleted: path={}", target);
            } else {
                log.debug("Image deletion skipped because file does not exist: path={}", target);
            }
        } catch (IOException exception) {
            throw new ImageStorageException("Failed to delete image " + relativePath, exception);
        }
    }

    private Path resolveInsideRoot(Path relativePath) {
        if (relativePath.isAbsolute()) {
            throw invalidPath(relativePath);
        }
        Path target = rootDirectory.resolve(relativePath).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw invalidPath(relativePath);
        }
        return target;
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "image";
        }
        Path filename = Path.of(originalFilename.replace('\\', '/')).getFileName();
        return filename == null || filename.toString().isBlank() ? "image" : filename.toString();
    }

    private ImageStorageException invalidPath(Path path) {
        return new ImageStorageException(
                "Image path escapes storage directory: " + path,
                new IllegalArgumentException("Path must be inside image storage directory")
        );
    }

}
