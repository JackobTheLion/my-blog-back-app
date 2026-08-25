package ru.practicum.yakovlev.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.yakovlev.dto.CreatePostRequest;
import ru.practicum.yakovlev.dto.FullPostResponseDto;
import ru.practicum.yakovlev.dto.PageResponse;
import ru.practicum.yakovlev.dto.UpdatePostRequest;
import ru.practicum.yakovlev.exception.ImageStorageException;
import ru.practicum.yakovlev.exception.PostNotFoundException;
import ru.practicum.yakovlev.mapper.PostMapper;
import ru.practicum.yakovlev.model.Post;
import ru.practicum.yakovlev.repository.PostRepository;
import ru.practicum.yakovlev.repository.SearchCriteria;
import ru.practicum.yakovlev.service.PostService;
import ru.practicum.yakovlev.storage.ImageStorage;
import ru.practicum.yakovlev.util.SearchCriteriaUtil;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final ImageStorage imageStorage;

    @Override
    public PageResponse getPosts(String searchRequest, Integer page, Integer size) {
        log.debug("Loading posts: page={}, size={}, searchApplied={}",
                page, size, searchRequest != null && !searchRequest.isBlank());
        SearchCriteria criteria = SearchCriteriaUtil.getSearchCriteria(searchRequest);
        List<FullPostResponseDto> posts = postRepository.findAll(criteria, page, size).stream()
                .map(postMapper::toTruncatePostResponseDto)
                .toList();
        long total = postRepository.count(criteria);
        long lastPage = total / size + (total % size == 0 ? 0 : 1);
        log.debug("Posts loaded: page={}, returned={}, total={}", page, posts.size(), total);
        return new PageResponse(posts, page > 1, page < lastPage, lastPage);
    }

    @Override
    @Transactional
    public FullPostResponseDto createPost(CreatePostRequest postRequest) {
        int tagCount = postRequest.tags().size();
        log.info("Creating post: tagCount={}", tagCount);
        Post entity = postMapper.toEntity(postRequest);
        Post saved = postRepository.save(entity);
        log.info("Post created: postId={}, tagCount={}", saved.getId(), tagCount);
        return postMapper.toFullPostResponseDto(saved);
    }

    @Override
    @Transactional
    public FullPostResponseDto updatePost(Long id, UpdatePostRequest postRequest) {
        if (!id.equals(postRequest.id())) {
            throw new IllegalArgumentException("Post id in path and body must match");
        }
        log.info("Updating post: postId={}", id);
        Post existing = getPostOrThrow(id);
        Post update = postMapper.update(existing, postRequest);
        update.setId(id);
        Post updated = postRepository.update(update);
        log.info("Post updated: postId={}", id);
        return postMapper.toFullPostResponseDto(updated);
    }

    @Override
    public FullPostResponseDto findPost(Long id) {
        log.debug("Loading post: postId={}", id);
        Post post = getPostOrThrow(id);
        return postMapper.toFullPostResponseDto(post);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        log.info("Deleting post: postId={}", id);
        Post post = getPostOrThrow(id);
        postRepository.deleteById(id);
        imageStorage.delete(post.getImagePath());
        log.info("Post deleted: postId={}", id);
    }

    @Override
    public Long likePost(Long id) {
        getPostOrThrow(id);
        Long likesCount = postRepository.incrementLikes(id);
        log.info("Post liked: postId={}, likesCount={}", id, likesCount);
        return likesCount;
    }

    @Override
    public void savePostImage(Long id, MultipartFile image) {
        log.info("Updating post image: postId={}, originalFilename={}, sizeBytes={}",
                id, image.getOriginalFilename(), image.getSize());
        Post post = getPostOrThrow(id);
        String oldPath = post.getImagePath();
        byte[] content;
        try {
            content = image.getBytes();
        } catch (IOException exception) {
            throw new ImageStorageException("Failed to read uploaded image for post %s. Reason: %s".formatted(id, exception.getMessage()),
                    exception);
        }

        String newPath = imageStorage.save(id, image.getOriginalFilename(), content);
        try {
            postRepository.updateImagePath(id, newPath);
        } catch (RuntimeException exception) {
            try {
                imageStorage.delete(newPath);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
        imageStorage.delete(oldPath);
        log.info("Post image updated: postId={}, path={}", id, newPath);
    }

    @Override
    public byte[] findPostImage(Long id) {
        log.debug("Loading post image: postId={}", id);
        String imagePath = postRepository.findImagePathByPostId(id)
                .orElseThrow(() -> new PostNotFoundException("Image for post " + id + " was not found"));
        return imageStorage.read(imagePath);
    }

    private Post getPostOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + id + " was not found"));
    }

}
