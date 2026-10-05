# Milastoria — архитектура решения (as-built)

Документ описывает **текущую реализацию** сайта-лукбука Milastoria:
как устроено, как данные текут по слоям, какие границы заложены.
Про «почему выбрали Javalin, а не Spring» и планы вне v1 — в корневом
[`ARCHITECTURE.md`](../ARCHITECTURE.md); про запуск и VPS — в
[`DEPLOYMENT.md`](DEPLOYMENT.md).

Статус: реализовано и работает в проде/на VPS-контуре. Документ
фиксирует код, а не черновик.

---

## 1. Назначение системы

**Milastoria** — публичный сайт-лукбук ателье нарядных платьев
(детские, женские, family look, работы клиентов) + узкая админка для
наполнения контентом.

| Свойство | Значение |
|----------|----------|
| Аудитория | будущие клиенты ателье; нагрузка околонулевая |
| Публичные страницы | главная, галерея, карточка работы |
| Админка | один пользователь, HTML-формы, без SPA |
| Контент | ~50–200 карточек лотов; фото; часть — видео |
| SEO | SSR (HTML на сервере), sitemap, meta/OG, JSON-LD |
| Приватность | без внешних трекеров; аналитика своя |

Система **не** магазин: нет корзины, заказов, оплаты. Ранее — Tilda
(магазинная механика); сейчас — собственная галерея.

---

## 2. Общая схема

```
                     ┌─────────────────────────────────────────┐
                     │                 VPS                     │
                     │  Ubuntu + Docker Compose                │
                     │                                         │
  Браузер ──HTTPS──► │  Caddy :80/:443                         │
                     │    │ reverse_proxy app:8080             │
                     │    │ TLS Let's Encrypt                  │
                     │    ▼                                   │
                     │  milastoria-app (JVM, Javalin/Jetty)   │
                     │    │                                    │
                     │    ├─ /            SSR site             │
                     │    ├─ /gallery     SSR + фильтры/FTS    │
                     │    ├─ /work/{slug} SSR карточка         │
                     │    ├─ /sitemap.xml, /robots.txt         │
                     │    ├─ /content/**  EXTERNAL static      │
                     │    ├─ /assets,/css,/js  classpath       │
                     │    └─ /admin/**    админка (auth)       │
                     │    │                                    │
                     │    ├─ ImageMagick (ProcessBuilder)      │
                     │    └─ ffmpeg (медиатека)                │
                     │                                         │
                     │  volume milastoria-data → /app/data     │
                     │    milastoria.db   (SQLite)             │
                     │    branding/watermark.png               │
                     │    content/{img,video,originals,media}  │
                     └─────────────────────────────────────────┘
```

Три роли наружу:

1. **Публичный сайт** — только read SSR, фильтры, поиск, аналитика.
2. **Админка** — write CRUD + загрузка медиа + SEO/контакты/hero.
3. **Системные** — схема SQLite на старте, FTS-rebuild, watermark extract.

Внешних сервисов **нет** (нет S3, нет Redis, нет очередей, нет CMS
в облаке). Всё, что нужно — JVM в контейнере и Docker volume.

---

## 3. Стек и сборка

| Слой | Выбор | Роль |
|------|-------|------|
| HTTP | **Javalin 6** (Jetty) | роутинг, middleware, static files |
| Шаблоны | **jte** (precompiled, Html) | SSR; ошибки шаблона — на этапе сборки |
| Доступ к данным | **MyBatis 3** | SQL руками в XML-мапперах |
| БД | **SQLite** + sqlite-jdbc | один файл `milastoria.db` |
| Пул соединений | **HikariCP**, `maximumPoolSize=1` | SQLite пишет один поток за раз |
| JSON (admin API) | Jackson databind | `ctx.json()` в Javalin 6 |
| Логирование | slf4j-simple → stdout | Docker собирает |
| Образ | multi-stage Maven → temurin:21-jre-alpine | + imagemagick* + ffmpeg |
| Передний ход | **Caddy 2** | TLS, reverse proxy, security headers |

Сборка (`backend/pom.xml`):

1. `maven-compiler-plugin` — Java 21.
2. **jte-maven-plugin** (`process-classes`, `precompile`) — `.jte` →
   Java-классы в `target/classes`.
3. **maven-shade-plugin** — один fat-jar `milastoria-app.jar`
   (main: `ru.milastoria.App`).
4. Docker: `-DskipTests` в контейнере; тесты — локально/в CI.

В runtime `CONVERT_BIN` по умолчанию `magick` (ImageMagick 7 в Alpine);
локально в `app.yml` может быть `convert` (IM 6).

---

## 4. Внутренняя архитектура приложения

Один модуль, один jar. Слои — без DI-фреймворка: объекты собираются
явно в `App.main`.

```
HTTP (Javalin)
    │
    ▼
web.*Controller          ← Context, redirect, cookie, multipart
    │  мапперы + view-record
    ▼
view.*                   ← неизменяемые модели для jte (HomeView, LotCard…)
    │
    ├──► mapper.*        ← MyBatis interface + XML SQL
    ├──► search.LotSearchIndex  ← FTS5 (единая точка записи)
    ├──► image.ImageProcessor   ← ProcessBuilder → convert/magick
    ├──► media.ExifReader       ← EXIF для медиатеки
    └──► analytics.AnalyticsTracker ← before-hook, анонимные визиты

jte.TemplateEngine       ← render(template, model) → HTML
classpath statics        ← resources/public
EXTERNAL statics          ← CONTENT_DIR (/content/**)
```

### 4.1. Точка входа — `App`

`backend/src/main/java/ru/milastoria/App.java` — «проводка», не бизнес:

| Шаг | Что делает |
|-----|------------|
| `AppConfig.load()` | YAML `APP_CONFIG` / `config/app.yml` + env |
| `createDataSource` | JDBC `sqlite:...`, PRAGMA foreign_keys, pool=1 |
| `initSchema` | `/schema.sql` + «наивные» `ADD COLUMN IF MISSING` + seed при пустом `lots` |
| `SqlSessionFactory` | мапперы; `mapUnderscoreToCamelCase=true` |
| dict/slogans seed | словари и H1-слоганы, если пусто |
| `LotSearchIndex.rebuildAll` | FTS пересобирается **каждый старт** |
| `ImageProcessor.extractBundledWatermark` | `branding/watermark.png` на диск рядом с БД |
| `Auth` | SHA-256 пароля + HMAC session_secret из config |
| Javalin statics | classpath `public` + EXTERNAL `/content` |
| маршруты | site / admin / media / sitemap |
| `app.before(analytics::track)` | публичные GET-визиты |
| `app.before("/admin", …)` и `/admin/*` | guard сессии (**оба** паттерна) |

Лимиты multipart (важно для iPad-фото): max file **50 МБ**, total request
**80 МБ**, in-memory до 4 МБ.

### 4.2. Контроллеры

| Класс | Зона ответственности |
|-------|----------------------|
| `web.SiteController` | `/`, `/gallery`, `/work/{slug}`; SEO/JSON-LD; view-record |
| `web.SitemapController` | `/sitemap.xml` по `site.base_url` |
| `web.AdminController` | логин, дашборд, CRUD лотов/разделов, фото+кроп, settings, analytics UI |
| `web.MediaAdminController` | медиатека: пачковая загрузка, EXIF, attach к лоту |
| `web.Auth` | не контроллер; проверка кредов и HMAC-сессия |

Паттерн публичной страницы (`SiteController`):

```text
open SqlSession
  → mapper queries (categories, lots, settings, slogans…)
  → build HomeView / GalleryView / WorkView (record-объекты)
  → templateEngine.render("gallery.jte", view)
close session
```

Один контроллер — один запрос — один `SqlSession` (вложенные сессии
для SQLite deadlock'ят — правило зафиксировано в `backend/README.md`).

Слой **Service отсутствует** сознательно: где логики нет —
Controller → Mapper напрямую; где логика (кроп, индекс, seed словарей)
— небольшие static-классы в своих пакетах (`search`, `image`).

### 4.3. View-record и шаблоны

| Пакет | Что |
|-------|-----|
| `view.*` | `HomeView`, `GalleryView`, `WorkView`, `LotCard`, `CategoryTile`, `VideoTile`, admin-view-модели |
| `view.SiteSeo` | title/description, canonical, default OG image, JSON-LD |
| `view.SiteContacts` / settings-хелперы | hero/about/contacts из `settings` |

Шаблоны jte:

| Путь | Назначение |
|------|------------|
| `jte/layout.jte` | общий HTML: head, OG/Twitter, nav, footer, `gg.jte.Content content` |
| `jte/home.jte`, `gallery.jte`, `work.jte` | публичные страницы |
| `jte/partials/*.jte` | `catTile`, `lotCard`, `videoCard` |
| `jte/admin/*.jte` | `layout`, `login`, `dashboard`, формы, media, analytics… |

Клиентский JS/CSS (`/css`, `/js`, `/assets`) — progressive enhancement
поверх готового HTML (лайтбокс, мобильное меню). **JS не строит
сетку из data.js** — в отличие от старого `prototype-v2`.

### 4.4. Конфигурация

`AppConfig` — свой плоский YAML-парсер (без SnakeYAML):

```
приоритет: ENV → app.yml → дефолт в коде
```

| YAML ключ | ENV | Смысл |
|-----------|-----|-------|
| `admin.user` | `ADMIN_USER` | логин |
| `admin.password` | `ADMIN_PASSWORD` | пароль (текст, хешируется в памяти) |
| `admin.session_secret` | `SESSION_SECRET` | HMAC сессии/аналитики |
| `server.port` | `PORT` | HTTP |
| `storage.content_dir` | `CONTENT_DIR` | папка img/video/originals/media |
| `storage.db_path` | `DB_PATH` | файл SQLite |
| `site.base_url` | `SITE_BASE_URL` | sitemap/canonical |
| `image.convert` | `CONVERT_BIN` | бинарь ImageMagick |

Путь: `APP_CONFIG` (в Docker — `/app/config/app.yml`), иначе
`config/app.yml` относительно cwd. Dev-значения не должны попадать в прод.

---

## 5. Модель данных

SQLite. Схема: `backend/src/main/resources/schema.sql`
(применяется на каждом старте, `CREATE TABLE IF NOT EXISTS`).
Миграции — точечные `ALTER TABLE ... ADD COLUMN` в `App.initSchema`,
не Flyway.

### 5.1. Краткий ER

```
categories 1───* lots 1───* lot_images
                 │  1───* lot_videos
                 │  *───* tags          (lot_tags)
                 │  *───* dict_values  (lot_dict; kind: tag|occasion|fabric)

settings     key/value (hero_*, about_*, contacts_*, gallery_*)
slogans      enabled H1-слоганы
media_files  медиатека до attach (kind image|video, EXIF)
lots_fts     FTS5 (title, annotation, seo_*, tags) + lot_id UNINDEXED
analytics_visits  visitor_id, session_id, path, referrer, ua, created_at
```

### 5.2. Сущности домена (`domain/`)

| Тип | Ключевые поля |
|-----|----------------|
| `Category` | slug, title, cover_image, seo_text, lead, sort |
| `Lot` | slug, title, category_id, annotation, seo_*, occasion, year, fabric_notes, client_name, featured, **status** (`draft`/`published`), sort, created_at |
| `LotImage` | lot_id, path_thumb, path_full, alt, caption, sort, **wm_*** (доли watermark) |
| `LotVideo` | lot_id, path, poster_path, caption |
| `MediaFile` | kind, original_name, path, thumb_path, size, EXIF-поля, crop_* |
| `Slogan` | text, enabled, sort |

### 5.3. Правила публикации

- Публичные выборки (`findFeatured`, `findPublished` и др.) фильтруют
  `status = 'published'`.
- Черновик — обычный `status='draft'` + переключатель в админке;
  отдельной таблицы версий нет.
- Похожие работы (`findRelated`): та же категория + score по общим
  occasion/tag из `dict_values`, затем sort, id; `excludeId` текущего.

### 5.4. Статика vs данные

| Тип | Где | Кто пишет |
|-----|-----|-----------|
| Брендовые ассеты (logo, css, js, favicon) | jar → classpath `public` | разработчик |
| Контент сайта (img/video) | volume `/app/data/content` | админка / перенос |
| Оригиналы | `content/originals/` | админка (не пережимается) |
| Медиатека | `content/media/` (+ thumb) | админка до attach |
| Watermark | `data/branding/watermark.png` | extract из jar при старте |
| SQL схема/seed | jar resources | `App.initSchema` |

Ссылки в БД — **пути** (`/content/img/...`), не бинарные blob.

---

## 6. Публичный сайт

### 6.1. Маршруты

| Метод + путь | Обработчик | Назначение |
|--------------|------------|------------|
| `GET /` | `SiteController.home` | hero, категории, слоган, about, contacts |
| `GET /gallery` | `SiteController.gallery` | сетка; `?category=` `?tag=` `?q=` |
| `GET /work/{slug}` | `SiteController.work` | карточка: галерея, видео, SEO, related |
| `GET /sitemap.xml` | `SitemapController` | индексируемые URL |
| `GET /robots.txt` | classpath static | allow site, disallow `/admin` |
| `GET /catalog`, `/catalog/` | redirect 301 | → `/gallery` (старый Tilda) |
| `GET /profile`, `/about`, `/contacts` | redirect 301 | → `/#about` / `/#contacts` |
| `GET /assets/**`, `/css/**`, `/js/**` | Javalin classpath | брендинг |
| `GET /content/**` | EXTERNAL `CONTENT_DIR` | thumb/full/video |

Фильтры галереи — **query-параметры**, не сегменты пути: для каждого
набора — свой indexable URL, dynamic SQL в MyBatis проще, чем
сегментные роуты.

### 6.2. SEO

- SSR: crawлер получает готовый HTML без JS.
- `layout.jte`: title, description, canonical, robots, OG/Twitter,
  theme-color.
- `SiteSeo`: absolute URL от `site.base_url`; JSON-LD на карточках
  (в т.ч. Product, см. код view).
- Sitemap: published-лоты и категории.
- `robots.txt`: `Disallow: /admin`, `Sitemap: https://.../sitemap.xml`.

### 6.3. Клиентский слой

Прогрессивное улучшение:

- лайтбокс, меню, переключение миниатюр — на `data-*` и готовом HTML;
- без отдельной модели данных в браузере;
- сидированные/загруженные файлы отдаются напрямую с тома через
  Javalin EXTERNAL static.

---

## 7. Поиск

| Компонент | Роль |
|-----------|------|
| `util.Fts` | сборка `MATCH`-выражения (OR по словам) |
| `search.LotSearchIndex` | **единственная** точка записи `lots_fts` |
| `lots_fts` (FTS5) | title, annotation, seo_text, meta_*, tags; tokenizing `unicode61 remove_diacritics 2` |
| `LotMapper.findPublished(..., ftsQuery)` | публичная галерея |
| `LotMapper.searchAdmin` | админка, тот же механизм |

Поток:

```
старт app ──► rebuildAll (clear + reindex all lots + tags)
админка save lot ──► indexLot / insert/update
публичный ?q= ──► Fts.toMatchExpression ──► FIND ... MATCH
```

Теги денормализованы в поле `tags` индекса при пересборке строки.
Hikari=1 + один SqlSession — против deadlock на SQLite.

---

## 8. Админка

### 8.1. Авторизация (`Auth`)

| Аспект | Реализация |
|--------|------------|
| Пользователь | один; креды из config (`admin.user` / `password`) |
| Пароль | SHA-256 в памяти при старте; сравнение constant-time |
| Сессия | cookie `admin_session` = `expiry.hmac(payload)`; **без** таблицы сессий |
| TTL | 7 дней |
| Guard | `before` на `/admin` **и** `/admin/*` |
| Смена секрета | иначе все куки инвалидируются (ручной re-login) |

Это не multi-user security-модель: один админ, HTTPS, VPS. SHA-256
без соли — осознанный компромисс (см. комментарий в `Auth.java`).

### 8.2. Экраны и маршруты (основное)

| Область | Маршруты |
|---------|----------|
| Login/logout | `GET/POST /admin/login`, `POST /admin/logout` |
| Дашборд | `GET /admin` |
| Лоты | `GET /admin/lots/new`, `POST /admin/lots`, `GET .../{id}/edit`, `POST .../{id}`, reorder |
| Фото лота | `GET/POST /admin/lots/{id}/photos`, reorder, delete |
| Разделы | CRUD + reorder `/admin/categories...` |
| Медиатека | `GET /admin/media`, upload, delete, attach form/submit |
| Watermark | `GET/POST /admin/watermark`, `GET .../watermark.png` |
| Контакты/other | contacts, slogans, hero, about, gallery-section |
| Аналитика UI | `GET /admin/analytics` |

Формы — обычный HTML + POST multipart. Кроп фото — **Cropper.js** (CDN,
мажор 1.x): клиент шлёт файл + координаты `x,y,width,height` в пикселях
**оригинала**; сервер режет через ImageMagick заново.

Оригинал всегда сохраняется отдельно (`originals/`) — правка кропа не
теряет качество на повторном пережатии.

### 8.3. Медиатека

Сценарий под iPad: «сняли пачку → загрузили в медиатеку → прикрепили
к лоту».

```
upload ──► content/media/ (+ thumb) + media_files (EXIF, dates)
attach photo ──► ImageProcessor → content/img/{slug}__{sort}[.thumb]
attach video ──► content/video/ (как есть; постер через ffmpeg)
              ──► запись в lot_images / lot_videos
              ──► физически удаляется из media_files + файлов
```

Сортировка списка медиатеки — по `exif_datetime` (fallback
`uploaded_at`).

### 8.4. Что админка умеет / не умеет

| Есть | Нет (v1) |
|------|----------|
| Создание/правка лотов, разделов | Удаление лотов/разделов |
| Загрузка/кроп фото, watermark | Массовые операции пачками на лоты |
| Медиатека + attach | Роли/многопользовательность |
| SEO-поля, hero/about/contacts | Версии/история черновиков |
| FTS в админке | Импорт из Tilda (механически — seed/demo) |
| Аналитика посещений | A/B, внешние дашборды |

---

## 9. Пайплайн изображений

Пакет `ru.milastoria.image`.

### 9.1. Контракт входа

Клиент (админка) отдаёт:

- файл-оригинал (или готовый путь в медиатеке),
- прямоугольник кропа в пикселях оригинала `CropRect(x, y, w, h)`,
- опционально `WatermarkPlacement` (доли + opacity; по умолчанию SE).

Оригинал не пережимается в canvas браузером — **не** тот вариант, что
«canvas-blob сразу в thumbnail».

### 9.2. Команда ImageMagick (важный порядок)

```
magick ORIGINAL -auto-orient
  -crop WxH+X+Y +repage
  -resize {maxEdge}x{maxEdge}\>
  -strip -interlace Plane -quality {q}
  ( watermark.png -resize {wmWidth}x [opacity ops] )
  -gravity SouthEast|NorthWest -geometry +dx+dy
  -composite
  OUTPUT.jpg
```

| Шаг | Зачем |
|-----|--------|
| `-auto-orient` **до** crop/strip | EXIF-ориентация применяется к пикселям |
| `-crop +repage` | кроп по выбранному прямоугольнику |
| `-resize WxH>` | **не** увеличивать, если уже меньше |
| `-strip` | выбросить EXIF/GPS (часто дети на снимке) |
| `-interlace Plane` | прогрессивный JPEG |
| watermark `-composite` | в конце; размер/opacity — долями выходного кропа |

### 9.3. Варианты вывода

| Variant | maxEdge | JPEG q | Назначение |
|---------|---------|--------|------------|
| `full` | 1800 | 84 | лайтбокс, страница лота |
| `thumb` | 900 | 82 | сетка галереи |
| `media_thumb` | 480 | 82 | превью медиатеки, **без** watermark |

Одна команда = один внешний процесс `ProcessBuilder` (argv, без shell —
скобки для `-composite` не экранируются).

Watermark по умолчанию: `extractBundledWatermark(dataDir/branding)`;
админ может менять размер/положение/прозрачность (сохраняется в
`settings` и `lot_images.wm_*`).

---

## 10. Аналитика

`analytics.AnalyticsTracker` — server-side, **без** GA/Яндекс.Метрики
как зависимости.

| Параметр | Решение |
|----------|---------|
| `visitor_id` | анонимный UUID, cookie `mila_vid`, подпись тем же `SESSION_SECRET`, TTL ~1 год |
| `session_id` | cookie `mila_sid`, TTL ~2 часа |
| `path`, referrer, UA | записываются в `analytics_visits` |
| IP | **не** хранится |
| Боты | фильтр по User-Agent |
| Пропуск | `/admin`, static, sitemap, robots |
| Ошибки трекера | не валят HTTP-ответ |

UI: `GET /admin/analytics` (дневные/пути статы SQL-агрегатами).

Cookie-секрет — тот же `session_secret`: если его сменить, все visitor-id
пересоздадутся (аналитика «сломается» как непрерывность, данные останутся).

---

## 11. Инфраструктура и деплой (кратко)

Детали — [`DEPLOYMENT.md`](DEPLOYMENT.md).

| Артефакт | Роль |
|----------|------|
| `backend/Dockerfile` | build (Maven) → runtime JRE + ImageMagick + ffmpeg |
| `docker-compose.yml` | `app` (8080) + `caddy` (80/443), volumes |
| `Caddyfile` | host milastoria.com, reverse_proxy `app:8080`, HSTS, nosniff |
| volume `milastoria-data` | БД + контент + branding |
| volume caddy_data/config | сертификаты ACME |

Бэкап = tar тома (`/app/data`). БД — один файл; restore = положить
файл + content обратно.

---

## 12. Критические инварианты

Нарушение любого из них ломает поведение незаметно:

1. **Один `SqlSession` на HTTP-запрос** (в т.ч. на весь контроллер-метод).
2. **Hikari pool = 1** — не «повышайте» для SQLite.
3. **`content_dir` — реальная директория**, не симлинк (Jetty 404).
4. **Порядок `-auto-orient` до `-strip`** — иначе EXIF-ориентация
   может не примениться / GPS уцелеет.
5. **FTS пишется только через `LotSearchIndex`** — не руками UPDATE.
6. **Auth guard: и `/admin`, и `/admin/*`** — без второго дашборд
   открыт без логина (баг был пойман).
7. **ENV > YAML > default** в конфиге — в compose/`Dockerfile` нельзя
   случайно оставить dev-`APP_CONFIG`.
8. **`seed.sql` только при пустом `lots`** — на проде это демо-контент;
   для боевого URL контролируйте первый volume/БД.
9. **Удаление лотов не реализовано** — демо-карточки не исчезнут сами.
10. **session_secret и admin password не в git** (`backend/config/app.yml`
    в `.gitignore`).

---

## 13. Потоки данных (реальные)

### 13.1. Первая публикация нового лота

```
Админ (iPad/браузер)
  POST /admin/lots          → lots (status=draft)
  POST /admin/lots/{id}/photos (file + crop + wm)
       ├─ оригинал → content/originals/...
       ├─ magick → content/img/{slug}__N__full.jpg
       ├─ magick → content/img/{slug}__N.jpg
       └─ INSERT lot_images + indexLot
  POST /admin/lots/{id}     → publish: status=published
Посетитель
  GET /gallery?q=...  → FTS MATCH → HTML SSR
  GET /work/slug      → JOIN images/videos/settings → layout.jte
  GET /content/img/... → EXTERNAL static
```

### 13.2. Смена кропа

```
Админ снова POST photos с новыми x,y,w,h
  → originals/ тот же файл (без потери)
  → пережатие только thumb/full
  → path_* в БД обновляются
```

### 13.3. Старт процесса

```
java -jar app.jar
  → AppConfig
  → SQLite open + schema + seed?
  → dicts/slogans seed?
  → FTS rebuild
  → watermark extract
  → bind PORT (default 8080)
  → Caddy начинает принимать HTTPS после ACME
```

---

## 14. Тесты

| Тест | Что проверяет |
|------|----------------|
| `ImageProcessorTest` | реальный `convert`/`magick` на sample.jpg; размеры/вес выхода |
| `ExifReaderTest` | парсинг EXIF медиатеки |
| `FtsTest` | сборка MATCH-выражений |
| `AppConfigTest` | YAML-parse, приоритет ключей |
| `MediaLibraryViewTest` | view-модель медиатеки |

Для тестов ImageMagick **обязателен** в PATH. В Docker-сборке
`-DskipTests`; критерий качества — локальный/CI `mvn test`.

---

## 15. Что сознательно вне рамок

(Повтор и уточнение плана из корневого ARCHITECTURE.md — чтобы
новый разработчик не «доращивал» систему без нужды.)

- Многопользовательская админка, роли, OAuth.
- Объектное хранилище (S3) вместо volume.
- Очереди/ async-обработка видео (видео отдаётся как есть).
- CI/CD (на старте — `docker compose build && up -d` руками).
- Оффлайн-билд/пакетирование на VPS без Docker (не нужно).
- Миграционный движок (Flyway/Liquibase) — только если schema
  начнёт болеть от ручных ALTER.

---

## 16. Карта файлов

```
Milastoria_Tilda/
├── ARCHITECTURE.md          # черновик решений («почему»)
├── docs/
│   ├── DEPLOYMENT.md        # сборка и прод-запуск
│   └── ARCHITECTURE.md      # этот файл (as-built)
├── Caddyfile
├── docker-compose.yml
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── config/app.yml.example
│   └── src/main/
│       ├── java/ru/milastoria/
│       │   ├── App.java              # composition root
│       │   ├── AppConfig.java
│       │   ├── domain/               # Lot, Category, ...
│       │   ├── mapper/ + resources/.../mapper/*.xml
│       │   ├── web/                  # controllers + Auth
│       │   ├── view/                 # view-records + SiteSeo
│       │   ├── image/                # ImageProcessor, CropRect, wm
│       │   ├── media/                # ExifReader
│       │   ├── search/               # LotSearchIndex
│       │   ├── analytics/            # AnalyticsTracker
│       │   └── util/Fts.java
│       ├── jte/                      # публичные + admin шаблоны
│       └── resources/
│           ├── schema.sql
│           ├── seed.sql
│           ├── branding/watermark.png
│           └── public/               # css, js, assets, robots.txt
├── prototype-v2/                     # старый статический прототип
└── scripts/setup-server.sh           # VPS bootstrap (может не быть в git)
```

---

## 17. Сводка решений в одном абзаце

Milastoria — маленький SSR-сайт на **Javalin + jte + MyBatis + SQLite**
в одном Java 21 jar: публичная галерея и узкая HTML-админка стоят на
**одном** процессе, данные живут в **одном** Docker volume (БД +
файлы), картинки режет **ImageMagick** из неизменяемых оригиналов,
HTTPS держит **Caddy**, аналитика — своя без внешних трекеров. Слои
плоские (Controller → Mapper / view), сложность держится в домене
контента и пайплайне фото, а не в инфраструктуре.
