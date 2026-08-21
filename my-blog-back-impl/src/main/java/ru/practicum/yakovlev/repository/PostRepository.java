package ru.practicum.yakovlev.repository;

import ru.practicum.yakovlev.model.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository {

    List<Post> findAll(SearchCriteria searchCriteria, int pageNumber, int pageSize);

    long count(SearchCriteria searchCriteria);

    Optional<Post> findById(Long id);

    Post save(Post post);

    Post update(Post post);

    boolean isPostExists(Long id);

    void deleteById(Long id);

    Long incrementLikes(Long id);

    void updateImagePath(Long id, String relativePath);

    Optional<String> findImagePathByPostId(Long id);
}
