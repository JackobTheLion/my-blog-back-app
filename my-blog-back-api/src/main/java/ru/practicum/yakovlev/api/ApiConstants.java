package ru.practicum.yakovlev.api;

public class ApiConstants {

    public static final String BASE_PATH = "";

    public static final String POSTS_BASE_PATH = BASE_PATH + "/posts";

    public static final String COMMENTS_BASE_PATH = POSTS_BASE_PATH + "/{postId}/comments";

    private ApiConstants() {
    }


}
