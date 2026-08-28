package ru.practicum.yakovlev.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.yakovlev.config.AllowedImageTypes;
import ru.practicum.yakovlev.dto.CreatePostRequest;
import ru.practicum.yakovlev.dto.FullPostResponseDto;
import ru.practicum.yakovlev.dto.PageResponse;
import ru.practicum.yakovlev.dto.UpdatePostRequest;
import ru.practicum.yakovlev.exception.ImageStorageException;
import ru.practicum.yakovlev.exception.PostNotFoundException;
import ru.practicum.yakovlev.mapper.PostMapper;
import ru.practicum.yakovlev.model.Post;
import ru.practicum.yakovlev.repository.ImageCleanupOutboxRepository;
import ru.practicum.yakovlev.repository.PostRepository;
import ru.practicum.yakovlev.repository.SearchCriteria;
import ru.practicum.yakovlev.service.PostImageUpdateService;
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
    private final ImageCleanupOutboxRepository outboxRepository;
    private final PostImageUpdateService postImageUpdateService;
    private final Tika tika;
    private final AllowedImageTypes allowedImageTypes;

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
        if (post.getImagePath() != null && !post.getImagePath().isBlank()) {
            outboxRepository.enqueueDelete(post.getImagePath());
        }
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
        validateImageNotEmpty(image);
        log.info("Updating post image: postId={}, originalFilename={}, sizeBytes={}",
                id, image.getOriginalFilename(), image.getSize());
        getPostOrThrow(id);

        byte[] content = readImageContent(id, image);
        validateImageType(content);
        String newPath = imageStorage.save(id, image.getOriginalFilename(), content);
        updateImagePath(id, newPath);

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

    private void validateImageNotEmpty(MultipartFile image) {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Uploaded image must not be empty");
        }
    }

    private byte[] readImageContent(Long postId, MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException exception) {
            throw new ImageStorageException(
                    "Failed to read uploaded image for post %s. Reason: %s"
                            .formatted(postId, exception.getMessage()),
                    exception
            );
        }
    }

    private void validateImageType(byte[] content) {
        String mediaType = tika.detect(content);
        if (!allowedImageTypes.contains(mediaType)) {
            throw new IllegalArgumentException("Image type is not allowed: " + mediaType);
        }
    }

    private void updateImagePath(Long postId, String newPath) {
        try {
            postImageUpdateService.updateImagePath(postId, newPath);
        } catch (RuntimeException exception) {
            deleteNewImage(newPath);
            throw exception;
        }
    }

    private void deleteNewImage(String path) {
        try {
            imageStorage.delete(path);
        } catch (RuntimeException cleanupException) {
            log.error("Failed to clean up a new image after update failure: path={}", path, cleanupException);
        }
    }

}
