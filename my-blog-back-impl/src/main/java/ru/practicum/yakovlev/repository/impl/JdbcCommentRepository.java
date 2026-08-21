package ru.practicum.yakovlev.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.practicum.yakovlev.model.Comment;
import ru.practicum.yakovlev.repository.CommentRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcCommentRepository implements CommentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<Comment> findAllByPostId(Long postId) {
        return jdbcTemplate.query("""
                        SELECT id, text, post_id
                        FROM comments
                        WHERE post_id = :postId
                        ORDER BY id
                        """,
                Map.of("postId", postId),
                this::mapComment
        );
    }

    @Override
    public Optional<Comment> findByIdAndPostId(Long postId, Long commentId) {
        return jdbcTemplate.query("""
                        SELECT id, text, post_id
                        FROM comments
                        WHERE id = :id AND post_id = :postId
                        """,
                new MapSqlParameterSource()
                        .addValue("postId", postId)
                        .addValue("id", commentId),
                resultSet -> resultSet.next()
                        ? Optional.of(mapComment(resultSet, 0))
                        : Optional.empty()
        );
    }

    @Override
    public Comment save(Comment comment) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("text", comment.getText())
                .addValue("postId", comment.getPostId());
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update("""
                        INSERT INTO comments (text, post_id)
                        VALUES (:text, :postId)
                        """,
                parameters,
                keyHolder,
                new String[]{"id"}
        );

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return an id for the saved comment");
        }
        comment.setId(key.longValue());
        return comment;
    }

    @Override
    public Comment update(Comment comment) {
        jdbcTemplate.update("""
                        UPDATE comments
                        SET text = :text
                        WHERE id = :id AND post_id = :postId
                        """,
                new MapSqlParameterSource()
                        .addValue("id", comment.getId())
                        .addValue("text", comment.getText())
                        .addValue("postId", comment.getPostId())
        );
        return comment;
    }

    @Override
    public void deleteByIdAndPostId(Long postId, Long commentId) {
        jdbcTemplate.update("""
                        DELETE FROM comments
                        WHERE id = :id AND post_id = :postId
                        """,
                new MapSqlParameterSource()
                        .addValue("postId", postId)
                        .addValue("id", commentId)
        );
    }

    private Comment mapComment(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Comment(
                resultSet.getLong("id"),
                resultSet.getString("text"),
                resultSet.getLong("post_id")
        );
    }

}
