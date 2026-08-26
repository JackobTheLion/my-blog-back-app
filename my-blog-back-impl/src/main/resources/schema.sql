CREATE SCHEMA IF NOT EXISTS my_blog;

CREATE TABLE IF NOT EXISTS my_blog.posts (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    text TEXT NOT NULL,
    likes_count BIGINT NOT NULL DEFAULT 0,
    image_path TEXT
);

CREATE TABLE IF NOT EXISTS my_blog.comments (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    post_id BIGINT NOT NULL,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES my_blog.posts (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS my_blog.tags (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS my_blog.post_tags (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,

    PRIMARY KEY (post_id, tag_id),

    CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES my_blog.posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tags_tag FOREIGN KEY (tag_id) REFERENCES my_blog.tags (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS my_blog.image_cleanup_outbox (
    id BIGSERIAL PRIMARY KEY,
    image_path TEXT NOT NULL UNIQUE,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_image_cleanup_outbox_ready
    ON my_blog.image_cleanup_outbox (next_attempt_at, id);
