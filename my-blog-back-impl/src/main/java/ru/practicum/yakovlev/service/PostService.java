package ru.practicum.yakovlev.service;

import org.springframework.web.multipart.MultipartFile;
import ru.practicum.yakovlev.dto.CreatePostRequest;
import ru.practicum.yakovlev.dto.FullPostResponseDto;
import ru.practicum.yakovlev.dto.PageResponse;
import ru.practicum.yakovlev.dto.UpdatePostRequest;

public interface PostService {
    PageResponse getPosts(String searchRequest, Integer page, Integer size);

    FullPostResponseDto createPost(CreatePostRequest postRequest);

    FullPostResponseDto updatePost(Long id, UpdatePostRequest postRequest);

    FullPostResponseDto findPost(Long id);

    void deletePost(Long id);

    Long likePost(Long id);

    void savePostImage(Long id, MultipartFile image);

    byte[] findPostImage(Long id);
}
