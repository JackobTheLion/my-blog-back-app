package ru.practicum.yakovlev.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.yakovlev.model.Tag;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TagMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "text", source = "tag")
    Tag toEntity(String tag);

    default String toDto(Tag tag) {
        return tag == null ? null : tag.getText();
    }

}
