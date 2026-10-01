-- Прямое отражение модели из ../ARCHITECTURE.md.
-- CREATE TABLE IF NOT EXISTS — безопасно выполнять при каждом старте приложения.
-- created_at добавлен позже: на существующих БД колонку достраивает App.initSchema.

CREATE TABLE IF NOT EXISTS categories (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    slug        TEXT NOT NULL UNIQUE,
    title       TEXT NOT NULL,
    cover_image TEXT,
    seo_text    TEXT,
    created_at  TEXT,
    sort        INTEGER NOT NULL DEFAULT 0
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
    sort             INTEGER NOT NULL DEFAULT 0,
    created_at       TEXT
);

CREATE TABLE IF NOT EXISTS lot_images (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    lot_id     INTEGER NOT NULL REFERENCES lots(id),
    path_thumb TEXT NOT NULL,
    path_full  TEXT,
    alt        TEXT,
    caption    TEXT,
    sort       INTEGER NOT NULL DEFAULT 0,
    -- watermark: доли выходного кропа (null = default SE из settings)
    wm_x       REAL,
    wm_y       REAL,
    wm_width   REAL,
    wm_opacity REAL
);

CREATE TABLE IF NOT EXISTS settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
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

-- Словари админки: повод / материалы / теги.
-- kind: 'occasion' | 'fabric' | 'tag'. lot_dict — значения лота.
-- Теги дублируются в tags/lot_tags (фильтр галереи, FTS) — синхронизация
-- в AdminController при сохранении лота.
CREATE TABLE IF NOT EXISTS dict_values (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    kind TEXT NOT NULL,
    name TEXT NOT NULL,
    UNIQUE (kind, name)
);

CREATE TABLE IF NOT EXISTS lot_dict (
    lot_id  INTEGER NOT NULL REFERENCES lots(id),
    dict_id INTEGER NOT NULL REFERENCES dict_values(id),
    PRIMARY KEY (lot_id, dict_id)
);

-- Полнотекстовый индекс лотов. rowid не используется как FK —
-- lot_id лежит отдельной UNINDEXED-колонкой, чтобы JOIN к lots был
-- тривиальным. Теги денормализованы в tags: при INSERT/UPDATE лота
-- индекс пересобирает LotSearchIndex (одно место синхронизации).
-- Синтаксис запроса — обычный MATCH FTS5, из Java поднимает Fts.toMatchExpression.
CREATE VIRTUAL TABLE IF NOT EXISTS lots_fts USING fts5(
    lot_id UNINDEXED,
    title,
    annotation,
    seo_text,
    meta_title,
    meta_description,
    tags,
    tokenize = 'unicode61 remove_diacritics 2'
);

-- Слоганы H1 на главной: сервер выбирает случайный enabled при каждом запросе
CREATE TABLE IF NOT EXISTS slogans (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    text    TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    sort    INTEGER NOT NULL DEFAULT 0
);

-- Раздел «О нас» на главной хранится в settings (about_*)
-- Блок «Контакты» на главной — settings (contacts_*):
-- contacts_title, contacts_lead, contacts_image, contacts_phone,
-- contacts_email, contacts_address, contacts_messengers ("Имя|url" по строке)
