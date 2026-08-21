package ru.practicum.yakovlev.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.yakovlev.api.CommentController;
import ru.practicum.yakovlev.dto.CommentRequestDto;
import ru.practicum.yakovlev.dto.CommentResponseDto;
import ru.practicum.yakovlev.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentControllerImpl implements CommentController {

    private final CommentService commentService;

    @Override
    public List<CommentResponseDto> getComments(Long postId) {
        return commentService.findAllByPostId(postId);
    }

    @Override
    public CommentResponseDto getComment(Long postId, Long commentId) {
        return commentService.getComment(postId, commentId);
    }

    @Override
    public CommentResponseDto createComment(CommentRequestDto commentDto, Long postId) {
        return commentService.createComment(commentDto, postId);
    }

    @Override
    public CommentResponseDto updateComment(CommentRequestDto commentDto, Long postId, Long commentId) {
        return commentService.update(commentDto, postId, commentId);
    }

    @Override
    public void deleteComment(Long postId, Long commentId) {
        commentService.deleteComment(postId, commentId);
    }
}
