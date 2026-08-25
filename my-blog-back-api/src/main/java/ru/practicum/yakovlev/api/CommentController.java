package ru.practicum.yakovlev.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;
import ru.practicum.yakovlev.dto.CommentResponseDto;
import ru.practicum.yakovlev.dto.CreateCommentRequest;
import ru.practicum.yakovlev.dto.UpdateCommentRequest;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.COMMENTS_BASE_PATH)
public interface CommentController {

    @GetMapping
    List<CommentResponseDto> getComments(@PathVariable("postId") @Positive Long postId);

    @GetMapping("/{commentId}")
    CommentResponseDto getComment(@PathVariable("postId") @Positive Long postId,
                                  @PathVariable("commentId") @Positive Long commentId);

    @PostMapping
    CommentResponseDto createComment(@RequestBody @Valid CreateCommentRequest commentRequest,
                                     @PathVariable("postId") @Positive Long postId);

    @PutMapping("/{commentId}")
    CommentResponseDto updateComment(@RequestBody @Valid UpdateCommentRequest commentRequest,
                                     @PathVariable("postId") @Positive Long postId,
                                     @PathVariable("commentId") @Positive Long commentId);

    @DeleteMapping("/{commentId}")
    void deleteComment(@PathVariable("postId") @Positive Long postId,
                       @PathVariable("commentId") @Positive Long commentId);

}
