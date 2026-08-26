package ru.practicum.yakovlev.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.practicum.yakovlev.model.Post;
import ru.practicum.yakovlev.model.Tag;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JdbcTagRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Test
    @DisplayName("Adds and returns tags for a post")
    void shouldAddAndReturnTagsForPost() {
        long postId = savedPostId();

        List<Tag> persisted = tagRepository.addAllForPost(postId, List.of(tag("#spring"), tag("#java")));

        assertEquals(2, persisted.size());
        assertTrue(persisted.stream().allMatch(value -> value.getId() != null));
        assertEquals(List.of("#java", "#spring"), tagTexts(tagRepository.findAllByPostId(postId)));
    }

    @Test
    @DisplayName("Ignores duplicate tags when adding them to a post")
    void shouldIgnoreDuplicateTagsForPost() {
        long postId = savedPostId();

        List<Tag> persisted = tagRepository.addAllForPost(
                postId,
                List.of(tag("#java"), tag(" #JAVA "), tag("#spring"), tag("#SPRING"))
        );

        assertEquals(List.of("#java", "#spring"), tagTexts(persisted));
        assertEquals(List.of("#java", "#spring"), tagTexts(tagRepository.findAllByPostId(postId)));
    }

    @Test
    @DisplayName("Reuses an existing tag for another post")
    void shouldReuseExistingTagForAnotherPost() {
        long firstPostId = savedPostId();
        long secondPostId = savedPostId();
        Tag first = tagRepository.addAllForPost(firstPostId, List.of(tag(" #SHARED "))).getFirst();

        Tag second = tagRepository.addAllForPost(secondPostId, List.of(tag("#shared"))).getFirst();

        assertEquals(first.getId(), second.getId());
        assertEquals("#shared", second.getText());
    }

    @Test
    @DisplayName("Groups tags by post and omits posts without tags")
    void shouldGroupTagsByPostAndOmitPostsWithoutTags() {
        long firstPostId = savedPostId();
        long secondPostId = savedPostId();
        long emptyPostId = savedPostId();
        tagRepository.addAllForPost(firstPostId, List.of(tag("#one"), tag("#shared")));
        tagRepository.addAllForPost(secondPostId, List.of(tag("#shared")));

        Map<Long, List<Tag>> result = tagRepository.findAllByPostIds(List.of(firstPostId, secondPostId, emptyPostId));

        assertEquals(List.of("#one", "#shared"), tagTexts(result.get(firstPostId)));
        assertEquals(List.of("#shared"), tagTexts(result.get(secondPostId)));
        assertFalse(result.containsKey(emptyPostId));
        assertEquals(Map.of(), tagRepository.findAllByPostIds(List.of()));
    }

    @Test
    @DisplayName("Replaces all tag relations for a post")
    void shouldReplaceAllTagRelationsForPost() {
        long postId = savedPostId();
        tagRepository.addAllForPost(postId, List.of(tag("#old")));

        tagRepository.replaceAllForPost(postId, List.of(tag("#new")));

        assertEquals(List.of("#new"), tagTexts(tagRepository.findAllByPostId(postId)));
    }

    private static Tag tag(String text) {
        return new Tag(null, text);
    }

    private static List<String> tagTexts(List<Tag> tags) {
        return tags.stream().map(Tag::getText).sorted().toList();
    }

    private long savedPostId() {
        Post post = new Post();
        post.setTitle("post");
        post.setText("text");
        post.setTags(List.of());
        return postRepository.save(post).getId();
    }
}
