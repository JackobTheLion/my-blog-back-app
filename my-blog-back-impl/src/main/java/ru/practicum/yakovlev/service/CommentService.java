package ru.practicum.yakovlev.service;

import ru.practicum.yakovlev.dto.CommentRequestDto;
import ru.practicum.yakovlev.dto.CommentResponseDto;

import java.util.List;

public interface CommentService {

    List<CommentResponseDto> findAllByPostId(Long postId);

    CommentResponseDto getComment(Long postId, Long commentId);

    CommentResponseDto createComment(CommentRequestDto commentDto, Long postId);

    CommentResponseDto update(CommentRequestDto commentDto, Long postId, Long commentId);

    void deleteComment(Long postId, Long commentId);

}
