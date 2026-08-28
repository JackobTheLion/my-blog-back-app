package ru.practicum.yakovlev.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Post {

    private Long id;

    private String title;

    private String text;

    private List<Tag> tags = List.of();

    private List<Comment> comments = List.of();

    private Long commentsCount = 0L;

    private Long likesCount = 0L;

    private String imagePath;

}
