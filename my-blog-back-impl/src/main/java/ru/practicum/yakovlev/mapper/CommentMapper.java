package ru.practicum.yakovlev.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import ru.practicum.yakovlev.dto.CommentRequestDto;
import ru.practicum.yakovlev.dto.CommentResponseDto;
import ru.practicum.yakovlev.model.Comment;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentMapper {

    CommentResponseDto toDto(Comment comment);

    List<CommentResponseDto> toDto(List<Comment> comments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postId", source = "postId")
    Comment toEntity(CommentRequestDto commentRequestDto, Long postId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postId", ignore = true)
    Comment update(@MappingTarget Comment target, CommentRequestDto source);

}
