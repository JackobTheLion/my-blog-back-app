package ru.practicum.yakovlev.service;

import org.springframework.web.multipart.MultipartFile;
import ru.practicum.yakovlev.dto.FullPostResponseDto;
import ru.practicum.yakovlev.dto.PageResponse;
import ru.practicum.yakovlev.dto.PostRequestDto;

public interface PostService {
    PageResponse getPosts(String searchRequest, Integer page, Integer size);

    FullPostResponseDto createPost(PostRequestDto postRequestDto);

    FullPostResponseDto updatePost(Long id, PostRequestDto postRequestDto);

    FullPostResponseDto findPost(Long id);

    void deletePost(Long id);

    Long likePost(Long id);

    void savePostImage(Long id, MultipartFile image);

    byte[] findPostImage(Long id);
}
