-- Демо-данные на реальных фото/видео из prototype-v2/assets — чтобы
-- перенесённые в jte шаблоны можно было проверить визуально, а не
-- только "данные дошли до страницы". Пути вида /content/... отдаёт
-- Javalin из внешней директории (см. App.java, CONTENT_DIR) — в бою
-- это будет том с файлами, которые сгенерировал ImageProcessor.
--
-- Выполняется один раз: App.java запускает этот скрипт, только если
-- таблица lots пустая.

INSERT INTO categories (slug, title, cover_image, seo_text, created_at) VALUES
    ('girls', 'Платья для девочек', '/content/img/cat-girls.jpg',
     'Детские нарядные платья на заказ: выпускные, праздничные, вечерние модели Milastoria.',
     '2024-11-01T10:00:00Z'),
    ('women', 'Женское платье', '/content/img/cat-women.jpg',
     'Женские дизайнерские платья: торжественные, вечерние и коктейльные образы ателье Milastoria.',
     '2024-11-01T10:00:00Z'),
    ('family', 'Family Look', '/content/img/cat-family.jpg',
     'Family look платья для мамы и дочери — совместные образы на значимые семейные события.',
     '2024-11-01T10:00:00Z'),
    ('clients', 'Работы клиентов', '/content/img/cat-clients.jpg',
     'Фотографии гостей в платьях Milastoria — реальные образы на торжествах, выпускных и семейных событиях.',
     '2024-11-01T10:00:00Z');

-- ───────── lots: id 1..10 в порядке вставки (используется ниже в lot_images/lot_videos) ─────────

INSERT INTO lots (slug, title, category_id, annotation, seo_text, meta_title, meta_description,
                   occasion, year, fabric_notes, featured, status, sort, created_at) VALUES
    ('gold-01', 'Золотое платье Gold', 1,
     'Пышное платье с многослойным тюлем и золотистой отделкой.',
     'Нарядное детское платье Gold — пышный силуэт, мягкий тюль и золотистые детали. Подходит для выпускных, дней рождения и семейных праздников.',
     'Золотое детское платье Gold — нарядное платье на заказ | Milastoria',
     'Платье Gold: пышный тюль, золотистая отделка, пошив на заказ. Детское нарядное платье ателье Milastoria.',
     'Выпускной, праздник', '2024', 'Тюль, атласная подкладка, декоративная отделка', 1, 'published', 1,
     '2024-11-01T10:00:00Z'),

    ('zoloto-bant', 'Золото «Бант»', 1,
     'Элегантное платье с крупным бантом и мерцающей тканью.',
     'Платье «Золото Бант» — торжественный образ для важных дат. Крупный бант, мягкий драп и золотистая фактура ткани.',
     'Золотое платье «Бант» — детское торжественное платье | Milastoria',
     'Платье «Золото Бант»: крупный бант, золотистая ткань, пошив на заказ.',
     'Праздник, фотосессия', '2024', 'Бархатистая ткань, фатин', 1, 'published', 2,
     '2024-11-02T10:00:00Z'),

    ('cherno-serebro', 'Чёрное серебро', 1,
     'Контрастный вечерний образ с мерцающей отделкой.',
     'Детское платье «Чёрное серебро» — выразительный вечерний силуэт. Есть видео-фрагмент движения и фактуры ткани.',
     'Платье «Чёрное серебро» — детское вечернее платье | Milastoria',
     'Платье «Чёрное серебро»: вечерний образ, мерцающая отделка, фото и видео.',
     'Вечернее событие, сцена', '2025', 'Пайетки, мерцающая отделка', 1, 'published', 3,
     '2024-11-03T10:00:00Z'),

    ('zolotoy-kolokolchik', 'Золотой колокольчик', 1,
     'Праздничный золотой образ с пышной юбкой.',
     'Детское платье «Золотой колокольчик» — торжественный золотой силуэт для особенных дат.',
     'Платье «Золотой колокольчик» — нарядное детское платье | Milastoria',
     'Платье «Золотой колокольчик»: золотистый образ, пышная юбка, пошив на заказ.',
     'Выпускной, праздник', '2024', 'Тюль, золотистая отделка', 1, 'published', 4,
     '2024-11-04T10:00:00Z'),

    ('anemona-01', 'Анемона', 1,
     'Нежный образ с цветочным характером и воздушным силуэтом.',
     'Детское платье «Анемона» — лёгкий, воздушный силуэт для торжеств и фотосессий.',
     'Платье «Анемона» — нарядное детское платье | Milastoria',
     'Нарядное платье «Анемона»: воздушный силуэт, мягкие оттенки, пошив на заказ.',
     'Праздник, фотосессия', '2024', 'Тюль, кружевные вставки', 0, 'published', 5,
     '2024-11-05T10:00:00Z'),

    ('polina-01', 'Полина', 1,
     'Нарядное платье с лёгким движением ткани.',
     'Детское платье «Полина» — нарядный образ с акцентом на движение ткани. Есть видео-фрагмент платья.',
     'Платье «Полина» — нарядное детское платье | Milastoria',
     'Платье «Полина»: лёгкое движение ткани, нарядный силуэт, фото и видео.',
     'Праздник, фотосессия', '2024', 'Тюль, подкладка', 0, 'published', 6,
     '2024-11-06T10:00:00Z'),

    ('ad-01', 'Женское платье AD-01', 2,
     'Женский вечерний образ с мягкой линией плеча.',
     'Женское дизайнерское платье AD-01 — торжественный силуэт для важных событий.',
     'Женское нарядное платье AD-01 — вечернее платье | Milastoria',
     'Женское платье AD-01: вечерний силуэт, мягкая линия плеча, пошив на заказ.',
     'Торжество, событие', '2024', 'Плёнка, подкладка', 1, 'published', 7,
     '2024-11-07T10:00:00Z'),

    ('dr-11', 'Вечернее платье DR-11', 2,
     'Пышный вечерний силуэт с многослойной юбкой.',
     'Вечернее женское платье DR-11 — пышный силуэт и многослойная юбка.',
     'Вечернее женское платье DR-11 — нарядное платье | Milastoria',
     'Платье DR-11: пышная юбка, вечерний силуэт, пошив на заказ.',
     'Вечернее событие', '2024', 'Тюль, многослойная юбка', 1, 'published', 8,
     '2024-11-08T10:00:00Z'),

    ('fl-01', 'Family Look FL-01', 3,
     'Совместный образ мамы и дочери на значимое событие.',
     'Family look Milastoria — совместные платья для мамы и дочери. Единая палитра, разные силуэты, общий характер образа.',
     'Family Look — платья для мамы и дочери | Milastoria',
     'Family look платья для мамы и дочери: единый образ на праздники и семейные события.',
     'Семейное событие, праздник', '2025', 'Комплект для мамы и дочери', 1, 'published', 9,
     '2024-11-09T10:00:00Z'),

    ('klienty-fotogalereya', 'Галерея работ клиентов', 4,
     'Реальные образы гостей в платьях Milastoria.',
     'Фотогалерея клиентов Milastoria — платья в жизни: торжества, выпускные и семейные события.',
     'Работы клиентов — фотогалерея платьев | Milastoria',
     'Фотографии гостей в нарядных платьях Milastoria: торжества, выпускные, семейные события.',
     'Реальные события', '2024–2025', '', 1, 'published', 10,
     '2024-11-10T10:00:00Z');

-- ───────── картинки ─────────

INSERT INTO lot_images (lot_id, path_thumb, path_full, alt, caption, sort) VALUES
    (1, '/content/img/gold-01__view_01.jpg', '/content/img/gold-01__view_01__full.jpg', 'Золотое детское платье Gold — вид спереди', 'Вид спереди', 1),
    (1, '/content/img/gold-01__view_02.jpg', '/content/img/gold-01__view_02__full.jpg', 'Золотое детское платье Gold — ракурс 3/4', 'Ракурс 3/4', 2),
    (1, '/content/img/gold-01__view_03.jpg', '/content/img/gold-01__view_03__full.jpg', 'Золотое детское платье Gold — детали', 'Детали кроя', 3),

    (2, '/content/img/zoloto-bant__view_01.jpg', '/content/img/zoloto-bant__view_01__full.jpg', 'Платье Золото Бант — вид спереди', 'Вид спереди', 1),
    (2, '/content/img/zoloto-bant__view_02.jpg', '/content/img/zoloto-bant__view_02__full.jpg', 'Платье Золото Бант — ракурс 3/4', 'Ракурс 3/4', 2),
    (2, '/content/img/zoloto-bant__view_03.jpg', '/content/img/zoloto-bant__view_03__full.jpg', 'Платье Золото Бант — сбоку', 'Профиль', 3),

    (3, '/content/img/cherno-serebro__view_01.jpg', '/content/img/cherno-serebro__view_01__full.jpg', 'Платье Чёрное серебро — вид спереди', 'Вид спереди', 1),
    (3, '/content/img/cherno-serebro__view_02.jpg', '/content/img/cherno-serebro__view_02__full.jpg', 'Платье Чёрное серебро — ракурс 3/4', 'Ракурс 3/4', 2),

    (4, '/content/img/zolotoy-kolokolchik__view_01.jpg', '/content/img/zolotoy-kolokolchik__view_01__full.jpg', 'Платье Золотой колокольчик — вид спереди', 'Вид спереди', 1),
    (4, '/content/img/zolotoy-kolokolchik__view_02.jpg', '/content/img/zolotoy-kolokolchik__view_02__full.jpg', 'Платье Золотой колокольчик — ракурс 3/4', 'Ракурс 3/4', 2),
    (4, '/content/img/zolotoy-kolokolchik__view_03.jpg', '/content/img/zolotoy-kolokolchik__view_03__full.jpg', 'Платье Золотой колокольчик — детали', 'Детали', 3),

    (5, '/content/img/anemona-01__view_00.jpg', '/content/img/anemona-01__view_00__full.jpg', 'Платье Анемона — общий вид', 'Общий вид', 1),
    (5, '/content/img/anemona-01__view_01.jpg', '/content/img/anemona-01__view_01__full.jpg', 'Платье Анемона — вид спереди', 'Вид спереди', 2),
    (5, '/content/img/anemona-01__view_02.jpg', '/content/img/anemona-01__view_02__full.jpg', 'Платье Анемона — детали', 'Детали', 3),

    (6, '/content/img/polina-01__view_01.jpg', '/content/img/polina-01__view_01__full.jpg', 'Платье Полина — вид спереди', 'Вид спереди', 1),
    (6, '/content/img/polina-01__view_02.jpg', '/content/img/polina-01__view_02__full.jpg', 'Платье Полина — ракурс 3/4', 'Ракурс 3/4', 2),

    (7, '/content/img/ad-01__view_01.jpg', '/content/img/ad-01__view_01__full.jpg', 'Женское платье AD-01 — вид спереди', 'Вид спереди', 1),
    (7, '/content/img/ad-01__view_02.jpg', '/content/img/ad-01__view_02__full.jpg', 'Женское платье AD-01 — ракурс 3/4', 'Ракурс 3/4', 2),
    (7, '/content/img/ad-01__view_03.jpg', '/content/img/ad-01__view_03__full.jpg', 'Женское платье AD-01 — детали', 'Детали', 3),

    (8, '/content/img/dr-11__view_01.jpg', '/content/img/dr-11__view_01__full.jpg', 'Платье DR-11 — вид спереди', 'Вид спереди', 1),
    (8, '/content/img/dr-11__view_02.jpg', '/content/img/dr-11__view_02__full.jpg', 'Платье DR-11 — ракурс 3/4', 'Ракурс 3/4', 2),
    (8, '/content/img/dr-11__view_03.jpg', '/content/img/dr-11__view_03__full.jpg', 'Платье DR-11 — детали', 'Детали', 3),

    (9, '/content/img/fl-01__view_01.jpg', '/content/img/fl-01__view_01__full.jpg', 'Family Look FL-01 — вид спереди', 'Вид спереди', 1),
    (9, '/content/img/fl-01__view_02.jpg', '/content/img/fl-01__view_02__full.jpg', 'Family Look FL-01 — ракурс 3/4', 'Ракурс 3/4', 2),

    (10, '/content/img/client-01.jpg', '/content/img/client-01.jpg', 'Клиент Milastoria в нарядном платье', 'Образ гостя', 1),
    (10, '/content/img/client-02.jpg', '/content/img/client-02.jpg', 'Клиент Milastoria — фото с торжества', 'С торжества', 2),
    (10, '/content/img/client-03.jpg', '/content/img/client-03.jpg', 'Клиент Milastoria — семейное событие', 'Семейное событие', 3),
    (10, '/content/img/client-04.jpg', '/content/img/client-04.jpg', 'Клиент Milastoria — выпускной', 'Выпускной', 4),
    (10, '/content/img/client-05.jpg', '/content/img/client-05.jpg', 'Клиент Milastoria — праздничный образ', 'Праздничный образ', 5);

-- ───────── видео ─────────

INSERT INTO lot_videos (lot_id, path, poster_path, caption) VALUES
    (2, '/content/video/zoloto-bant.mp4', '/content/img/poster__zoloto-bant-video.jpg', 'Видео-фрагмент: платье «Золото Бант»'),
    (3, '/content/video/cherno-serebro.mp4', '/content/img/cherno-serebro__view_01.jpg', 'Видео-фрагмент: платье «Чёрное серебро»'),
    (6, '/content/video/polina.mp4', '/content/img/polina-01__view_01.jpg', 'Видео-фрагмент: платье «Полина»'),
    (9, '/content/video/family-look.mp4', '/content/img/poster__carousel-adult-1.jpg', 'Видео-фрагмент: Family Look');

-- ───────── теги ─────────

INSERT INTO tags (name) VALUES
    ('золотое платье'), ('детское платье'), ('нарядное платье'), ('выпускной'),
    ('золото'), ('бантик'), ('торжественное платье'),
    ('чёрное серебро'), ('вечернее детское платье'), ('мерцающее платье'),
    ('золотой колокольчик'), ('золотое детское платье'), ('торжественное'),
    ('анемона'), ('нежное платье'), ('детское нарядное платье'),
    ('полина'), ('нарядное детское платье'), ('праздничное платье'),
    ('женское платье'), ('вечернее платье'),
    ('пышное платье'), ('женское нарядное платье'),
    ('family look'), ('платье для мамы и дочери'), ('семейный образ'),
    ('работы клиентов'), ('фотогалерея'), ('нарядные платья'), ('отзывы образов');

INSERT INTO lot_tags (lot_id, tag_id) VALUES
    (1, (SELECT id FROM tags WHERE name = 'золотое платье')),
    (1, (SELECT id FROM tags WHERE name = 'детское платье')),
    (1, (SELECT id FROM tags WHERE name = 'нарядное платье')),
    (1, (SELECT id FROM tags WHERE name = 'выпускной')),

    (2, (SELECT id FROM tags WHERE name = 'золото')),
    (2, (SELECT id FROM tags WHERE name = 'бантик')),
    (2, (SELECT id FROM tags WHERE name = 'детское платье')),
    (2, (SELECT id FROM tags WHERE name = 'торжественное платье')),

    (3, (SELECT id FROM tags WHERE name = 'чёрное серебро')),
    (3, (SELECT id FROM tags WHERE name = 'вечернее детское платье')),
    (3, (SELECT id FROM tags WHERE name = 'мерцающее платье')),

    (4, (SELECT id FROM tags WHERE name = 'золотой колокольчик')),
    (4, (SELECT id FROM tags WHERE name = 'золотое детское платье')),
    (4, (SELECT id FROM tags WHERE name = 'торжественное')),

    (5, (SELECT id FROM tags WHERE name = 'анемона')),
    (5, (SELECT id FROM tags WHERE name = 'нежное платье')),
    (5, (SELECT id FROM tags WHERE name = 'детское нарядное платье')),

    (6, (SELECT id FROM tags WHERE name = 'полина')),
    (6, (SELECT id FROM tags WHERE name = 'нарядное детское платье')),
    (6, (SELECT id FROM tags WHERE name = 'праздничное платье')),

    (7, (SELECT id FROM tags WHERE name = 'женское платье')),
    (7, (SELECT id FROM tags WHERE name = 'вечернее платье')),
    (7, (SELECT id FROM tags WHERE name = 'нарядное платье')),

    (8, (SELECT id FROM tags WHERE name = 'вечернее платье')),
    (8, (SELECT id FROM tags WHERE name = 'пышное платье')),
    (8, (SELECT id FROM tags WHERE name = 'женское нарядное платье')),

    (9, (SELECT id FROM tags WHERE name = 'family look')),
    (9, (SELECT id FROM tags WHERE name = 'платье для мамы и дочери')),
    (9, (SELECT id FROM tags WHERE name = 'семейный образ')),

    (10, (SELECT id FROM tags WHERE name = 'работы клиентов')),
    (10, (SELECT id FROM tags WHERE name = 'фотогалерея')),
    (10, (SELECT id FROM tags WHERE name = 'нарядные платья')),
    (10, (SELECT id FROM tags WHERE name = 'отзывы образов'));
