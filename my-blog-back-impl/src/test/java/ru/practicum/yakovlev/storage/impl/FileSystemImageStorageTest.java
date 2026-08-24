package ru.practicum.yakovlev.storage.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.practicum.yakovlev.exception.ImageStorageException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemImageStorageTest {

    @TempDir
    private Path storageDirectory;

    private FileSystemImageStorage imageStorage;

    @BeforeEach
    void setUp() {
        imageStorage = new FileSystemImageStorage(storageDirectory.toString());
    }

    @Test
    @DisplayName("Saves and reads image content with a sanitized filename")
    void shouldSaveAndReadImageWithSanitizedFilename() {
        byte[] content = {1, 2, 3};

        String relativePath = imageStorage.save(42L, "../../avatar.png", content);

        assertTrue(relativePath.matches("42/[0-9a-f-]{36}_avatar\\.png"));
        assertFalse(relativePath.contains(".."));
        assertTrue(Files.exists(storageDirectory.resolve(relativePath)));
        assertArrayEquals(content, imageStorage.read(relativePath));
    }

    @Test
    @DisplayName("Uses a fallback name when the original filename is missing")
    void shouldUseFallbackNameWhenOriginalFilenameIsMissing() {
        String relativePath = imageStorage.save(1L, null, new byte[]{1});

        assertTrue(relativePath.endsWith("_image"));
    }

    @Test
    @DisplayName("Deletes an existing image and ignores an empty path")
    void shouldDeleteExistingImageAndIgnoreEmptyPath() {
        String relativePath = imageStorage.save(1L, "image.png", new byte[]{1});

        imageStorage.delete(relativePath);

        assertFalse(Files.exists(storageDirectory.resolve(relativePath)));
        assertDoesNotThrow(() -> imageStorage.delete(relativePath));
        assertDoesNotThrow(() -> imageStorage.delete(null));
        assertDoesNotThrow(() -> imageStorage.delete(" "));
    }

    @Test
    @DisplayName("Wraps a missing image error in a storage exception")
    void shouldWrapMissingImageErrorInStorageException() {
        assertThrows(
                ImageStorageException.class,
                () -> imageStorage.read("1/missing.png")
        );
    }

    @Test
    @DisplayName("Rejects traversal and absolute image paths")
    void shouldRejectTraversalAndAbsolutePaths() {
        assertThrows(ImageStorageException.class, () -> imageStorage.read("../outside.png"));
        assertThrows(ImageStorageException.class, () -> imageStorage.delete("../../outside.png"));
        assertThrows(ImageStorageException.class, () -> imageStorage.read(storageDirectory.resolve("image.png").toString()));
    }
}
