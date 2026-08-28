package ru.practicum.yakovlev.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.yakovlev.exception.PostNotFoundException;
import ru.practicum.yakovlev.model.Post;
import ru.practicum.yakovlev.repository.ImageCleanupOutboxRepository;
import ru.practicum.yakovlev.repository.PostRepository;
import ru.practicum.yakovlev.service.PostImageUpdateService;

@Service
@RequiredArgsConstructor
public class PostImageUpdateServiceImpl implements PostImageUpdateService {

    private final PostRepository postRepository;
    private final ImageCleanupOutboxRepository outboxRepository;

    @Override
    @Transactional
    public void updateImagePath(Long postId, String newImagePath) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " was not found"));
        String oldImagePath = post.getImagePath();
        postRepository.updateImagePath(postId, newImagePath);
        if (oldImagePath != null && !oldImagePath.isBlank()) {
            outboxRepository.enqueueDelete(oldImagePath);
        }
    }
}
