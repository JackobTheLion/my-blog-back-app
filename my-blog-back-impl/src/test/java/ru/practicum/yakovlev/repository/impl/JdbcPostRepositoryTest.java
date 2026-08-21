package ru.practicum.yakovlev.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.practicum.yakovlev.model.Comment;
import ru.practicum.yakovlev.model.Post;
import ru.practicum.yakovlev.model.Tag;
import ru.practicum.yakovlev.repository.SearchCriteria;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class JdbcPostRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Test
    @DisplayName("Saves and returns a post with its tags")
    void shouldSaveAndReturnPostWithTags() {
        Post saved = postRepository.save(newPost("First post", "Full text", "#java", "#spring"));

        Post found = postRepository.findById(saved.getId()).orElseThrow();

        assertAll(
                () -> assertEquals(1L, saved.getId()),
                () -> assertEquals("First post", found.getTitle()),
                () -> assertEquals("Full text", found.getText()),
                () -> assertEquals(0L, found.getLikesCount()),
                () -> assertEquals(0L, found.getCommentsCount()),
                () -> assertEquals(List.of("#java", "#spring"), tagTexts(found))
        );
    }

    @Test
    @DisplayName("Returns posts in descending order with pagination")
    void shouldReturnPostsInDescendingOrderWithPagination() {
        postRepository.save(newPost("one", "text"));
        Post second = postRepository.save(newPost("two", "text"));
        Post third = postRepository.save(newPost("three", "text"));

        List<Post> firstPage = postRepository.findAll(emptyCriteria(), 1, 2);
        List<Post> secondPage = postRepository.findAll(emptyCriteria(), 2, 2);

        assertEquals(List.of(third.getId(), second.getId()), firstPage.stream().map(Post::getId).toList());
        assertEquals(List.of(1L), secondPage.stream().map(Post::getId).toList());
        assertEquals(3L, postRepository.count(emptyCriteria()));
    }

    @Test
    @DisplayName("Filters posts by title without regard to case and applies pagination")
    void shouldFilterPostsByTitleIgnoringCaseAndApplyPagination() {
        postRepository.save(newPost("unrelated", "text"));
        Post firstMatch = postRepository.save(newPost("SPRING basics", "text"));
        Post secondMatch = postRepository.save(newPost("Advanced Spring", "text"));
        Post thirdMatch = postRepository.save(newPost("spring data", "text"));
        SearchCriteria criteria = criteria("sPrInG");

        List<Post> firstPage = postRepository.findAll(criteria, 1, 2);
        List<Post> secondPage = postRepository.findAll(criteria, 2, 2);

        assertAll(
                () -> assertEquals(List.of(thirdMatch.getId(), secondMatch.getId()), postIds(firstPage)),
                () -> assertEquals(List.of(firstMatch.getId()), postIds(secondPage)),
                () -> assertEquals(3L, postRepository.count(criteria))
        );
    }

    @Test
    @DisplayName("Filters posts by one tag without regard to case")
    void shouldFilterPostsBySingleTagIgnoringCase() {
        Post matching = postRepository.save(newPost("matching", "text", "#JAVA"));
        postRepository.save(newPost("other", "text", "#spring"));
        SearchCriteria criteria = criteria("", "#java");

        List<Post> result = postRepository.findAll(criteria, 1, 10);

        assertAll(
                () -> assertEquals(List.of(matching.getId()), postIds(result)),
                () -> assertEquals(List.of("#JAVA"), tagTexts(result.getFirst())),
                () -> assertEquals(1L, postRepository.count(criteria))
        );
    }

    @Test
    @DisplayName("Returns a post only when all requested tags match")
    void shouldRequireAllRequestedTags() {
        Post matching = postRepository.save(newPost("matching", "text", "#java", "#sql", "#spring"));
        postRepository.save(newPost("java only", "text", "#java"));
        postRepository.save(newPost("sql only", "text", "#sql"));
        SearchCriteria criteria = criteria("", "#java", "#sql");

        List<Post> result = postRepository.findAll(criteria, 1, 10);

        assertAll(
                () -> assertEquals(List.of(matching.getId()), postIds(result)),
                () -> assertEquals(List.of("#java", "#spring", "#sql"), tagTexts(result.getFirst())),
                () -> assertEquals(1L, postRepository.count(criteria))
        );
    }

    @Test
    @DisplayName("Treats duplicate search tags as one criterion")
    void shouldIgnoreDuplicateTagsInSearchCriteria() {
        Post matching = postRepository.save(newPost("matching", "text", "#java", "#sql"));
        postRepository.save(newPost("partial", "text", "#java"));
        SearchCriteria criteria = criteria("", "#java", "#java", "#sql", "#sql");

        List<Post> result = postRepository.findAll(criteria, 1, 10);

        assertEquals(List.of(matching.getId()), postIds(result));
        assertEquals(1L, postRepository.count(criteria));
    }

    @Test
    @DisplayName("Combines title and tag criteria using AND")
    void shouldCombineTitleAndTagCriteria() {
        Post matching = postRepository.save(newPost("Spring JDBC", "text", "#java", "#sql"));
        postRepository.save(newPost("Spring MVC", "text", "#java"));
        postRepository.save(newPost("JDBC reference", "text", "#java", "#sql"));
        SearchCriteria criteria = criteria("spring", "#java", "#sql");

        List<Post> result = postRepository.findAll(criteria, 1, 10);

        assertEquals(List.of(matching.getId()), postIds(result));
        assertEquals(1L, postRepository.count(criteria));
    }

    @Test
    @DisplayName("Returns an empty page and zero count when search has no matches")
    void shouldReturnEmptyResultWhenSearchHasNoMatches() {
        postRepository.save(newPost("Spring JDBC", "text", "#java"));
        SearchCriteria criteria = criteria("missing", "#sql");

        List<Post> result = postRepository.findAll(criteria, 1, 10);

        assertTrue(result.isEmpty());
        assertEquals(0L, postRepository.count(criteria));
    }

    @Test
    @DisplayName("Updates post content and replaces tags")
    void shouldUpdatePostContentAndReplaceTags() {
        Post saved = postRepository.save(newPost("old", "old text", "#old"));
        saved.setTitle("new");
        saved.setText("new text");
        saved.setTags(List.of(new Tag(null, "#new")));

        postRepository.update(saved);

        Post found = postRepository.findById(saved.getId()).orElseThrow();
        assertEquals("new", found.getTitle());
        assertEquals("new text", found.getText());
        assertEquals(List.of("#new"), tagTexts(found));
    }

    @Test
    @DisplayName("Updates likes and the image path")
    void shouldUpdateLikesAndImagePath() {
        Post saved = postRepository.save(newPost("post", "text"));

        assertEquals(1L, postRepository.incrementLikes(saved.getId()));
        assertEquals(2L, postRepository.incrementLikes(saved.getId()));
        postRepository.updateImagePath(saved.getId(), "1/image.png");

        assertEquals(2L, postRepository.findById(saved.getId()).orElseThrow().getLikesCount());
        assertEquals("1/image.png", postRepository.findImagePathByPostId(saved.getId()).orElseThrow());
    }

    @Test
    @DisplayName("Returns empty results for missing posts and images")
    void shouldReturnEmptyForMissingPostAndImage() {
        Post saved = postRepository.save(newPost("post", "text"));

        assertTrue(postRepository.findById(404L).isEmpty());
        assertTrue(postRepository.findImagePathByPostId(saved.getId()).isEmpty());
        assertTrue(postRepository.findImagePathByPostId(404L).isEmpty());
        assertFalse(postRepository.isPostExists(404L));
    }

    @Test
    @DisplayName("Deletes a post and cascades comments and tag relations")
    void shouldDeletePostAndCascadeRelatedData() {
        Post saved = postRepository.save(newPost("post", "text", "#tag"));
        commentRepository.save(new Comment(null, "comment", saved.getId()));

        assertTrue(postRepository.isPostExists(saved.getId()));
        postRepository.deleteById(saved.getId());

        assertFalse(postRepository.isPostExists(saved.getId()));
        assertTrue(commentRepository.findAllByPostId(saved.getId()).isEmpty());
        assertTrue(tagRepository.findAllByPostId(saved.getId()).isEmpty());
    }

    private Post newPost(String title, String text, String... tags) {
        Post post = new Post();
        post.setTitle(title);
        post.setText(text);
        post.setTags(Stream.of(tags).map(tag -> new Tag(null, tag)).toList());
        return post;
    }

    private SearchCriteria emptyCriteria() {
        return criteria("");
    }

    private SearchCriteria criteria(String titlePart, String... tags) {
        return new SearchCriteria(titlePart, new LinkedList<>(List.of(tags)));
    }

    private List<String> tagTexts(Post post) {
        return post.getTags().stream().map(Tag::getText).sorted().toList();
    }

    private List<Long> postIds(List<Post> posts) {
        return posts.stream().map(Post::getId).toList();
    }
}
