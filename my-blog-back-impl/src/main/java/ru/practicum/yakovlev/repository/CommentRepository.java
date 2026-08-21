package ru.practicum.yakovlev.repository;

import ru.practicum.yakovlev.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {

    List<Comment> findAllByPostId(Long postId);

    Optional<Comment> findByIdAndPostId(Long postId, Long commentId);

    Comment save(Comment comment);

    Comment update(Comment comment);

    void deleteByIdAndPostId(Long postId, Long commentId);

}
