package ru.practicum.yakovlev.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.practicum.yakovlev.model.Tag;
import ru.practicum.yakovlev.repository.TagRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class JdbcTagRepository implements TagRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<Tag> findAllByPostId(Long postId) {
        return jdbcTemplate.query("""
                        SELECT t.id, t.name
                        FROM tags t
                        JOIN post_tags pt ON pt.tag_id = t.id
                        WHERE pt.post_id = :postId
                        ORDER BY t.name
                        """,
                Map.of("postId", postId),
                this::toTag);
    }

    @Override
    public Map<Long, List<Tag>> findAllByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Tag>> postIdToTag = new HashMap<>();
        jdbcTemplate.query("""
                        SELECT pt.post_id, t.id, t.name
                        FROM post_tags pt
                        JOIN tags t ON t.id = pt.tag_id
                        WHERE pt.post_id IN (:postIds)
                        ORDER BY pt.post_id, t.name
                        """,
                Map.of("postIds", postIds),
                resultSet -> {
                    long postId = resultSet.getLong("post_id");
                    postIdToTag.computeIfAbsent(postId, ignored -> new ArrayList<>())
                            .add(toTag(resultSet, 0));
                });
        postIdToTag.replaceAll((postId, tags) -> List.copyOf(tags));
        return Map.copyOf(postIdToTag);
    }

    @Override
    public List<Tag> addAllForPost(Long postId, List<Tag> tags) {
        if (tags.isEmpty()) {
            return List.of();
        }

        List<String> tagNames = tags.stream()
                .map(Tag::getText)
                .toList();

        SqlParameterSource[] tagParameters = tags.stream()
                .map(Tag::getText)
                .map(name -> new MapSqlParameterSource("name", name))
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate("""
                        INSERT INTO tags (name)
                        VALUES (:name)
                        ON CONFLICT (name) DO NOTHING
                        """,
                tagParameters);

        List<Tag> persistedTags = jdbcTemplate.query(
                "SELECT id, name FROM tags WHERE name IN (:names)",
                Map.of("names", tagNames),
                this::toTag
        );

        SqlParameterSource[] relationParameters = persistedTags.stream()
                .map(tag -> new MapSqlParameterSource()
                        .addValue("postId", postId, Types.BIGINT)
                        .addValue("tagId", tag.getId(), Types.BIGINT))
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate("""
                        INSERT INTO post_tags (post_id, tag_id)
                        VALUES (:postId, :tagId)
                        """,
                relationParameters
        );
        return List.copyOf(persistedTags);
    }

    @Override
    public void replaceAllForPost(Long postId, List<Tag> tags) {
        jdbcTemplate.update("DELETE FROM post_tags WHERE post_id = :postId",
                Map.of("postId", postId)
        );
        addAllForPost(postId, tags);
    }

    private Tag toTag(ResultSet resultSet, int ignore) throws SQLException {
        return new Tag(resultSet.getLong("id"), resultSet.getString("name"));
    }

}
