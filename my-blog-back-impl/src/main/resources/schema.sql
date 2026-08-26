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
    name TEXT NOT NULL UNIQUE,
    CONSTRAINT chk_tags_name_normalized CHECK (name = LOWER(BTRIM(name)))
);

CREATE TABLE IF NOT EXISTS my_blog.post_tags (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,

    PRIMARY KEY (post_id, tag_id),

    CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES my_blog.posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tags_tag FOREIGN KEY (tag_id) REFERENCES my_blog.tags (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tags_normalized_name
    ON my_blog.tags (LOWER(BTRIM(name)));

CREATE INDEX IF NOT EXISTS idx_comments_post_id_id
    ON my_blog.comments (post_id, id);

CREATE INDEX IF NOT EXISTS idx_post_tags_tag_id_post_id
    ON my_blog.post_tags (tag_id, post_id);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_posts_title_trgm
    ON my_blog.posts USING GIN (LOWER(title) gin_trgm_ops);

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
