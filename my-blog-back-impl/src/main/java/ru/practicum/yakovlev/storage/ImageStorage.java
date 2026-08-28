package ru.practicum.yakovlev.storage;

public interface ImageStorage {

    String save(Long postId, String originalFilename, byte[] content);

    byte[] read(String relativePath);

    void delete(String relativePath);

}
