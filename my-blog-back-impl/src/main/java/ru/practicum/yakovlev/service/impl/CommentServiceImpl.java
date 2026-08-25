package ru.practicum.yakovlev.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.yakovlev.dto.CommentResponseDto;
import ru.practicum.yakovlev.dto.CreateCommentRequest;
import ru.practicum.yakovlev.dto.UpdateCommentRequest;
import ru.practicum.yakovlev.exception.CommentNotFoundException;
import ru.practicum.yakovlev.mapper.CommentMapper;
import ru.practicum.yakovlev.model.Comment;
import ru.practicum.yakovlev.repository.CommentRepository;
import ru.practicum.yakovlev.service.CommentService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    @Override
    public List<CommentResponseDto> findAllByPostId(Long postId) {
        log.debug("Loading comments: postId={}", postId);
        List<Comment> allByPostId = commentRepository.findAllByPostId(postId);
        log.debug("Comments loaded: postId={}, count={}", postId, allByPostId.size());
        return commentMapper.toDto(allByPostId);
    }

    @Override
    public CommentResponseDto getComment(Long postId, Long commentId) {
        log.debug("Loading comment: postId={}, commentId={}", postId, commentId);
        Comment comment = getCommentOrThrow(postId, commentId);
        return commentMapper.toDto(comment);
    }

    @Override
    public CommentResponseDto createComment(CreateCommentRequest commentRequest, Long postId) {
        if (!postId.equals(commentRequest.postId())) {
            throw new IllegalArgumentException("Post id in path and body must match");
        }
        log.info("Creating comment: postId={}", postId);
        Comment entity = commentMapper.toEntity(commentRequest);
        Comment save = commentRepository.save(entity);
        log.info("Comment created: postId={}, commentId={}", postId, save.getId());
        return commentMapper.toDto(save);
    }

    @Override
    public CommentResponseDto update(UpdateCommentRequest commentRequest, Long postId, Long commentId) {
        if (!postId.equals(commentRequest.postId())) {
            throw new IllegalArgumentException("Post id in path and body must match");
        }
        if (!commentId.equals(commentRequest.id())) {
            throw new IllegalArgumentException("Comment id in path and body must match");
        }
        log.info("Updating comment: postId={}, commentId={}", postId, commentId);
        Comment existing = getCommentOrThrow(postId, commentId);
        Comment update = commentMapper.update(existing, commentRequest);
        Comment updated = commentRepository.update(update);
        log.info("Comment updated: postId={}, commentId={}", postId, commentId);
        return commentMapper.toDto(updated);
    }

    @Override
    public void deleteComment(Long postId, Long commentId) {
        log.info("Deleting comment: postId={}, commentId={}", postId, commentId);
        commentRepository.deleteByIdAndPostId(postId, commentId);
        log.info("Comment deleted: postId={}, commentId={}", postId, commentId);
    }

    private Comment getCommentOrThrow(Long postId, Long commentId) {
        return commentRepository.findByIdAndPostId(postId, commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment id %s of post id %s not found".formatted(commentId, postId)));
    }

}
