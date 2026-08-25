package ru.practicum.yakovlev.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import ru.practicum.yakovlev.dto.CreatePostRequest;
import ru.practicum.yakovlev.dto.FullPostResponseDto;
import ru.practicum.yakovlev.dto.UpdatePostRequest;
import ru.practicum.yakovlev.model.Post;
import ru.practicum.yakovlev.model.Tag;

import java.util.ArrayList;
import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {TagMapper.class, CommentMapper.class}
)
public abstract class PostMapper {

    private static final int POST_PREVIEW_LENGTH = 128;

    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "commentsCount", ignore = true)
    @Mapping(target = "likesCount", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tags", expression = "java(toTags(postRequest.tags()))")
    public abstract Post toEntity(CreatePostRequest postRequest);

    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "commentsCount", ignore = true)
    @Mapping(target = "likesCount", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tags", expression = "java(toTags(postRequest.tags()))")
    public abstract Post update(@MappingTarget Post post, UpdatePostRequest postRequest);

    public abstract FullPostResponseDto toFullPostResponseDto(Post post);

    @Mapping(target = "text", qualifiedByName = "truncateText")
    public abstract FullPostResponseDto toTruncatePostResponseDto(Post post);

    @Named("truncateText")
    protected String truncateText(String text) {
        if (text != null && text.length() > POST_PREVIEW_LENGTH) {
            return text.substring(0, POST_PREVIEW_LENGTH) + '…';
        }
        return text;
    }

    protected List<Tag> toTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        return tags.stream()
                .map(tag -> new Tag(null, tag))
                .toList();
    }

}
