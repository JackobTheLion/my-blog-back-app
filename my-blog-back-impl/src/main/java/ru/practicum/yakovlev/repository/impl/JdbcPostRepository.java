package ru.practicum.yakovlev.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.practicum.yakovlev.model.Post;
import ru.practicum.yakovlev.model.Tag;
import ru.practicum.yakovlev.repository.PostRepository;
import ru.practicum.yakovlev.repository.SearchCriteria;
import ru.practicum.yakovlev.repository.TagRepository;
import ru.practicum.yakovlev.util.TagNormalizer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class JdbcPostRepository implements PostRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TagRepository tagRepository;

    @Override
    public List<Post> findAll(SearchCriteria searchCriteria, int pageNumber, int pageSize) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String whereClause = buildWhereClause(searchCriteria, parameters);
        parameters
                .addValue("limit", pageSize)
                .addValue("offset", (long) (pageNumber - 1) * pageSize);

        String postColumns = """
                SELECT p.id, p.title, p.text, p.likes_count, p.image_path,
                       COUNT(DISTINCT c.id) AS comments_count
                FROM posts p
                LEFT JOIN comments c ON c.post_id = p.id
                """;
        String sql = postColumns
                + whereClause
                + " GROUP BY p.id, p.title, p.text, p.likes_count, p.image_path"
                + " ORDER BY p.id DESC LIMIT :limit OFFSET :offset";

        List<Post> posts = jdbcTemplate.query(sql, parameters, this::mapPost);
        attachTags(posts);
        return posts;
    }

    @Override
    public long count(SearchCriteria criteria) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String whereClause = buildWhereClause(criteria, parameters);
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts p" + whereClause,
                parameters,
                Long.class
        );
        return result == null ? 0 : result;
    }

    @Override
    public Optional<Post> findById(Long id) {
        String findPostByIdSql = """
                SELECT p.id, p.title, p.text, p.likes_count, p.image_path,
                       (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comments_count
                FROM posts p
                WHERE p.id = :id
                """;
        return jdbcTemplate.query(findPostByIdSql, Map.of("id", id), resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }

            Post post = mapPost(resultSet, 0);
            List<Tag> tags = tagRepository.findAllByPostId(post.getId());
            post.setTags(List.copyOf(tags));
            return Optional.of(post);
        });
    }

    @Override
    public Post save(Post post) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("title", post.getTitle())
                .addValue("text", post.getText());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO posts (title, text)
                VALUES (:title, :text)
                """, parameters, keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return an id for the saved post");
        }
        long postId = key.longValue();
        List<Tag> persistedTags = tagRepository.addAllForPost(postId, post.getTags());
        post.setId(postId);
        post.setTags(persistedTags);
        return post;
    }

    @Override
    public Post update(Post post) {
        Long postId = post.getId();
        jdbcTemplate.update("""
                UPDATE posts
                SET title = :title, text = :text
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", postId)
                .addValue("title", post.getTitle())
                .addValue("text", post.getText()));
        tagRepository.replaceAllForPost(postId, post.getTags());
        return post;
    }

    @Override
    public boolean isPostExists(Long id) {
        String sql = """
                SELECT EXISTS (SELECT 1 FROM posts p WHERE p.id = :id)
                """;

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Map.of("id", id), Boolean.class));
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM posts WHERE id = :id", Map.of("id", id));
    }

    @Override
    public Long incrementLikes(Long id) {
        return jdbcTemplate.queryForObject("""
                        UPDATE posts SET likes_count = likes_count + 1 WHERE id = :id
                        RETURNING likes_count
                        """,
                Map.of("id", id),
                Long.class
        );
    }

    @Override
    public void updateImagePath(Long id, String relativePath) {
        jdbcTemplate.update(
                "UPDATE posts SET image_path = :imagePath WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("imagePath", relativePath)
        );
    }

    @Override
    public Optional<String> findImagePathByPostId(Long id) {
        return jdbcTemplate.query(
                "SELECT image_path FROM posts WHERE id = :id AND image_path IS NOT NULL",
                Map.of("id", id),
                resultSet -> resultSet.next()
                        ? Optional.of(resultSet.getString("image_path"))
                        : Optional.empty()
        );
    }

    private void attachTags(List<Post> posts) {
        if (posts.isEmpty()) {
            return;
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, List<Tag>> tagsByPost = tagRepository.findAllByPostIds(postIds);
        posts.forEach(post -> post.setTags(List.copyOf(tagsByPost.getOrDefault(post.getId(), List.of()))));
    }

    private Post mapPost(ResultSet resultSet, int rowNumber) throws SQLException {
        Post post = new Post();
        post.setId(resultSet.getLong("id"));
        post.setTitle(resultSet.getString("title"));
        post.setText(resultSet.getString("text"));
        post.setLikesCount(resultSet.getLong("likes_count"));
        post.setCommentsCount(resultSet.getLong("comments_count"));
        post.setImagePath(resultSet.getString("image_path"));
        return post;
    }

    private String buildWhereClause(SearchCriteria criteria, MapSqlParameterSource parameters) {
        List<String> conditions = new ArrayList<>();
        String titlePart = criteria.titlePart();
        if (titlePart != null && !titlePart.isEmpty()) {
            conditions.add("LOWER(p.title) LIKE :titlePattern");
            parameters.addValue(
                    "titlePattern",
                    "%" + titlePart.toLowerCase(Locale.ROOT) + "%"
            );
        }
        List<String> tags = criteria.tags() == null
                ? List.of()
                : criteria.tags().stream()
                .map(TagNormalizer::normalize)
                .distinct()
                .toList();
        if (!tags.isEmpty()) {
            conditions.add("""
                    p.id IN (
                        SELECT pt.post_id
                        FROM post_tags pt
                        JOIN tags t ON t.id = pt.tag_id
                        WHERE t.name IN (:tags)
                        GROUP BY pt.post_id
                        HAVING COUNT(DISTINCT t.name) = :tagCount
                    )
                    """);
            parameters.addValue("tags", tags);
            parameters.addValue("tagCount", tags.size());
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

}
