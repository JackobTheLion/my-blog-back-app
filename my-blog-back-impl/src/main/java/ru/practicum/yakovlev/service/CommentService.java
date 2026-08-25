package ru.practicum.yakovlev.service;

import ru.practicum.yakovlev.dto.CommentResponseDto;
import ru.practicum.yakovlev.dto.CreateCommentRequest;
import ru.practicum.yakovlev.dto.UpdateCommentRequest;

import java.util.List;

public interface CommentService {

    List<CommentResponseDto> findAllByPostId(Long postId);

    CommentResponseDto getComment(Long postId, Long commentId);

    CommentResponseDto createComment(CreateCommentRequest commentRequest, Long postId);

    CommentResponseDto update(UpdateCommentRequest commentRequest, Long postId, Long commentId);

    void deleteComment(Long postId, Long commentId);

}
