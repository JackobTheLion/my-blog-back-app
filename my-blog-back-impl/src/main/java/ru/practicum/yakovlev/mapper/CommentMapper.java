package ru.practicum.yakovlev.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import ru.practicum.yakovlev.dto.CommentResponseDto;
import ru.practicum.yakovlev.dto.CreateCommentRequest;
import ru.practicum.yakovlev.dto.UpdateCommentRequest;
import ru.practicum.yakovlev.model.Comment;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentMapper {

    CommentResponseDto toDto(Comment comment);

    List<CommentResponseDto> toDto(List<Comment> comments);

    @Mapping(target = "id", ignore = true)
    Comment toEntity(CreateCommentRequest commentRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postId", ignore = true)
    Comment update(@MappingTarget Comment target, UpdateCommentRequest source);

}
