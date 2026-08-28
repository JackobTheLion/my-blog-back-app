package ru.practicum.yakovlev.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Comment {

    private Long id;

    private String text;

    private Long postId;

}
