package ru.practicum.yakovlev.api;

public class ApiConstants {

    public static final String BASE_PATH = "";

    public static final String POSTS_BASE_PATH = BASE_PATH + "/posts";

    public static final String COMMENTS_BASE_PATH = POSTS_BASE_PATH + "/{postId}/comments";

    public static final int MIN_PAGE_NUMBER = 1;

    public static final int MIN_PAGE_SIZE = 1;

    public static final int MAX_PAGE_SIZE = 100;

    private ApiConstants() {
    }


}
