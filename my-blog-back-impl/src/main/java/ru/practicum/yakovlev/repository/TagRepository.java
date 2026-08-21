package ru.practicum.yakovlev.repository;

import ru.practicum.yakovlev.model.Tag;

import java.util.List;
import java.util.Map;

public interface TagRepository {

    List<Tag> findAllByPostId(Long postId);

    Map<Long, List<Tag>> findAllByPostIds(List<Long> postIds);

    List<Tag> addAllForPost(Long postId, List<Tag> tags);

    void replaceAllForPost(Long postId, List<Tag> tags);
}
