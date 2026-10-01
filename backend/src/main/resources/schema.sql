-- Прямое отражение модели из ../ARCHITECTURE.md.
-- CREATE TABLE IF NOT EXISTS — безопасно выполнять при каждом старте приложения.

CREATE TABLE IF NOT EXISTS categories (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    slug        TEXT NOT NULL UNIQUE,
    title       TEXT NOT NULL,
    cover_image TEXT,
    seo_text    TEXT
);

CREATE TABLE IF NOT EXISTS lots (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    slug             TEXT NOT NULL UNIQUE,
    title            TEXT NOT NULL,
    category_id      INTEGER NOT NULL REFERENCES categories(id),
    annotation       TEXT,
    seo_text         TEXT,
    meta_title       TEXT,
    meta_description TEXT,
    occasion         TEXT,
    year             TEXT,
    fabric_notes     TEXT,
    client_name      TEXT,
    featured         INTEGER NOT NULL DEFAULT 0,
    status           TEXT NOT NULL DEFAULT 'draft',
    sort             INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS lot_images (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    lot_id     INTEGER NOT NULL REFERENCES lots(id),
    path_thumb TEXT NOT NULL,
    path_full  TEXT,
    alt        TEXT,
    caption    TEXT,
    sort       INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS lot_videos (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    lot_id      INTEGER NOT NULL REFERENCES lots(id),
    path        TEXT NOT NULL,
    poster_path TEXT,
    caption     TEXT
);

CREATE TABLE IF NOT EXISTS tags (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS lot_tags (
    lot_id INTEGER NOT NULL REFERENCES lots(id),
    tag_id INTEGER NOT NULL REFERENCES tags(id),
    PRIMARY KEY (lot_id, tag_id)
);
