-- Прямое отражение модели из ../ARCHITECTURE.md.
-- CREATE TABLE IF NOT EXISTS — безопасно выполнять при каждом старте приложения.
-- created_at добавлен позже: на существующих БД колонку достраивает App.initSchema.

CREATE TABLE IF NOT EXISTS categories (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    slug        TEXT NOT NULL UNIQUE,
    title       TEXT NOT NULL,
    cover_image TEXT,
    seo_text    TEXT,
    -- короткий зазывной под заголовком на плитке направления
    lead        TEXT,
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
-- Раздел «Галерея» (/gallery, без фильтров) — settings (gallery_*):
-- gallery_title, gallery_description
-- Шапка hero главной:
-- hero_lead — текст под H1 (общий для всех слайдов)
-- hero_slides — коллекция слайдов: пара ПК+моб (image) или видео
-- legacy: hero_image / hero_image_mobile — первый слайд при миграции
CREATE TABLE IF NOT EXISTS hero_slides (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    sort          INTEGER NOT NULL DEFAULT 0,
    kind          TEXT NOT NULL,              -- image | video
    desktop_path  TEXT NOT NULL,              -- /content/img/... или /content/video/...
    mobile_path   TEXT,                       -- image: мобильное фото. video: опц. мобильное видео
    alt           TEXT,
    created_at    TEXT
);

-- Аналитика сайта (серверная, без внешних трекеров).
-- visitor_id — анонимный id из подписанной cookie
-- session_id — маршрут за визит
-- created_at — ISO-8601 UTC. Статистика читается SQL, сырые логи не нужны наружу.
CREATE TABLE IF NOT EXISTS analytics_visits (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    visitor_id  TEXT NOT NULL,
    session_id  TEXT NOT NULL,
    path        TEXT NOT NULL,
    referrer    TEXT,
    user_agent  TEXT,
    created_at  TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_analytics_visits_created ON analytics_visits(created_at);
CREATE INDEX IF NOT EXISTS idx_analytics_visits_visitor ON analytics_visits(visitor_id);
CREATE INDEX IF NOT EXISTS idx_analytics_visits_session ON analytics_visits(session_id);
CREATE INDEX IF NOT EXISTS idx_analytics_visits_path ON analytics_visits(path);

-- Медиатека админки: исходники фото/видео с телефона до прикрепления к лоту.
-- kind: 'image' | 'video'.
-- exif_datetime — дата съёмки из EXIF (или fallback из имени файла/mtime).
-- Именно по ней по умолчанию сортируется список, null — fallback на uploaded_at.
-- exif_search — денормализованный lower-case blob для LIKE-поиска по EXIF/имени.
CREATE TABLE IF NOT EXISTS media_groups (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL,           -- grp-${id}
    created_at TEXT
);

CREATE TABLE IF NOT EXISTS media_files (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    kind           TEXT NOT NULL,
    original_name  TEXT NOT NULL,
    path           TEXT NOT NULL,
    thumb_path     TEXT,
    width          INTEGER,
    height         INTEGER,
    uploaded_at    TEXT NOT NULL,
    exif_datetime  TEXT,
    exif_make      TEXT,
    exif_model     TEXT,
    exif_orientation INTEGER,
    exif_json      TEXT,
    exif_search    TEXT,
    crop_x         INTEGER,
    crop_y         INTEGER,
    crop_w         INTEGER,
    crop_h         INTEGER,
    group_id       INTEGER,
    -- 1 = уже прикреплён к лоту. Файл и запись остаются, скрыты из общего списка
    applied        INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_media_files_exif_dt ON media_files(exif_datetime);
CREATE INDEX IF NOT EXISTS idx_media_files_uploaded ON media_files(uploaded_at);
CREATE INDEX IF NOT EXISTS idx_media_files_kind ON media_files(kind);
-- idx_media_files_group и idx_media_files_applied создаются в App.initSchema
-- после ADD COLUMN (на существующей БД колонки ещё нет в момент runScript)

