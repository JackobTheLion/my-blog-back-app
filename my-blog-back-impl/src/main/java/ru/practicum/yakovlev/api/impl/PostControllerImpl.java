package ru.practicum.yakovlev.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.yakovlev.api.PostController;
import ru.practicum.yakovlev.dto.FullPostResponseDto;
import ru.practicum.yakovlev.dto.PageResponse;
import ru.practicum.yakovlev.dto.PostRequestDto;
import ru.practicum.yakovlev.service.PostService;

@RestController
@RequiredArgsConstructor
public class PostControllerImpl implements PostController {

    private final PostService postService;

    @Override
    public PageResponse getPosts(String searchRequest, Integer page, Integer size) {
        return postService.getPosts(searchRequest, page, size);
    }

    @Override
    public FullPostResponseDto createPost(PostRequestDto postRequestDto) {
        return postService.createPost(postRequestDto);
    }

    @Override
    public FullPostResponseDto updatePost(Long id, PostRequestDto postRequestDto) {
        return postService.updatePost(id, postRequestDto);
    }

    @Override
    public FullPostResponseDto getPost(Long id) {
        return postService.findPost(id);
    }

    @Override
    public void deletePost(Long id) {
        postService.deletePost(id);
    }

    @Override
    public Long likePost(Long id) {
        return postService.likePost(id);
    }

    @Override
    public void postImage(Long id, MultipartFile image) {
        postService.savePostImage(id, image);
    }

    @Override
    public byte[] getPostImage(Long id) {
        return postService.findPostImage(id);
    }

}
