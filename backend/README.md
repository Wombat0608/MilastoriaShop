# milastoria-app — каркас бэкенда

Реализация решений из [`../ARCHITECTURE.md`](../ARCHITECTURE.md):
Javalin + MyBatis + jte + SQLite, один jar, без Spring.

## Что здесь есть

- Три страницы, перенесённые из `prototype-v2` на сервер: `/` (главная),
  `/gallery` (фильтры по категории/тегу/поиску через query-параметры —
  крawлер видит их как разные URL), `/work/{slug}` (карточка лота с
  лайтбоксом, видео, похожими работами). Вёрстка и CSS — те же, что в
  прототипе; отличие в том, что HTML теперь собирает jte из данных в
  SQLite, а не JS в браузере из `data.js`.
- `SiteController` — три обработчика, каждый: мапперы → view-record
  (`HomeView`/`GalleryView`/`WorkView`) → `templateEngine.render(...)`.
- Мапперы (`CategoryMapper`, `LotMapper`) — фильтрация галереи через
  MyBatis dynamic SQL (`<if>` в XML), а не через Spring Data
  query-derivation: SQL, который реально выполнится, целиком виден в
  файле.
- Сид-данные (`seed.sql`) — 10 лотов на 4 категории, с картинками,
  тегами и 4 видео, взятые из реальных фото ателье (`prototype/assets`)
  — этого хватает, чтобы проверить каждый визуальный кейс (masonry,
  bento категорий, видео-лента, похожие работы).
- Статика: `/assets`, `/css`, `/js` — из classpath (брендовые файлы,
  в `resources/public`); `/content/**` — из внешней директории
  (`CONTENT_DIR`), в проде это будет volume с результатами
  `ImageProcessor`, сейчас по умолчанию — `../prototype/assets`.
- `ru.milastoria.image.ImageProcessor` — кроп/ресайз/водяной знак через
  ImageMagick (`ProcessBuilder`), с тестом на реальном пайплайне
  (`ImageProcessorTest`). Подробности решения и проверка на настоящем
  RAW-кадре — в `../ARCHITECTURE.md`, раздел «Обработка изображений».
- **Админка** (`/admin`): логин по паролю (без Spring Security, см.
  `Auth`), список лотов (иконка, slug, id, дата создания, статус),
  создание/правка лотов и разделов, экран загрузки+кропа фото
  (`AdminController`, `admin/*.jte`). Выбор файла — нативный `<input
  type=file>`, кроп — Cropper.js на весь экран, один жест, кнопка
  «Готово» — ровно то, что зафиксировано в ARCHITECTURE.md под
  требование «просто до безобразия и удобно на iPad». Оригинал
  сохраняется нетронутым отдельно от опубликованных thumb/full — можно
  пересчитать кроп позже без потери качества.
- **Полнотекстовый поиск** (FTS5): таблица `lots_fts` + `Fts.toMatchExpression`
  (OR по словам) + `LotSearchIndex` (пересборка на старте, переиндекс
  при INSERT/UPDATE лота). Один и тот же механизм на `/gallery?q=` и в
  админке `?q=`; теги денормализованы в индекс. Hikari pool = 1 — под
  SQLite; **все** SQL в одном запросе/контроллере идут через один
  `SqlSession`, вложенные сессии deadlock'ят.

## Чего здесь сознательно нет

- Удаления лотов/разделов (только создание и правка) — добавим, когда
  понадобится.
- Реального контента — фото и тексты в `seed.sql` взяты из демо-кадров
  ателье для проверки вёрстки, это не то, что должно попасть в прод.

## Конфигурация: config/app.yml

Основной файл — **`backend/config/app.yml`** (путь меняется через env `APP_CONFIG`).

**Приоритет:** переменные окружения → YAML → дефолты в коде.

| YAML | ENV | Назначение |
|------|-----|------------|
| `admin.user` | `ADMIN_USER` | логин в `/admin` |
| `admin.password` | `ADMIN_PASSWORD` | пароль администратора (текст, хешируется в памяти) |
| `admin.session_secret` | `SESSION_SECRET` | ключ HMAC сессии |
| `server.port` | `PORT` | порт HTTP |
| `storage.content_dir` | `CONTENT_DIR` | **папка графических ресурсов** (`img/`, `video/`, `originals/`) |
| `storage.db_path` | `DB_PATH` | файл SQLite |
| `site.base_url` | `SITE_BASE_URL` | хост для sitemap |
| `image.convert` | `CONVERT_BIN` | бинарь ImageMagick |

В git — только `config/app.yml.example`. Файл `config/app.yml` с секретами
в git не коммитить (уже в `.gitignore`).

```bash
# локально (dev-значения уже в app.yml)
cd backend && java -jar target/milastoria-app.jar

# прод / свой путь к контенту
cp config/app.yml.example config/app.yml
# отредактируйте admin.password, admin.session_secret, storage.content_dir
APP_CONFIG=/etc/milastoria/app.yml java -jar target/milastoria-app.jar
```

**Важно про `storage.content_dir`:** настоящая директория, не симлинк
(Jetty молча 404 через symlink). Dev — `../prototype/assets`; prod —
чистый volume. Сюда пишутся thumb/full, обложки разделов, originals.

**Важно про пароль:** `admin`/`admin` — только локальная разработка;
при старте пишется WARN, если пароль/секрет не переопределены.

## Запуск локально

Нужны JDK 21 и Maven.

```bash
cd backend
mvn package
java -jar target/milastoria-app.jar
```

Откроется на `http://localhost:8080/`. Файл БД — `data/milastoria.db`
(см. `storage.db_path` в `config/app.yml`).

## Запуск в Docker

```bash
docker build -t milastoria-app .
docker run -p 8080:8080 -v milastoria-data:/app/data milastoria-app
```

Том `milastoria-data` — это и есть весь бэкап: файл БД плюс (в будущем)
загруженные фото.

## VPS + Caddy (прод)

В корне репозитория: `docker-compose.yml` + `Caddyfile`.

1. VPS: 1–2 vCPU, 1–2 ГБ RAM, Ubuntu 22.04/24.04, Docker + Compose.
2. DNS на Reg.ru: `A milastoria.ru → IP VPS` (+ `www` → тот же IP).
3. `cp backend/config/app.yml.example backend/config/app.yml` — пароль и session_secret.
4. На сервере: `git clone … && cd Milastoria_Tilda && docker compose up -d --build`.
5. Caddy сам выпустит HTTPS (Let's Encrypt) — платный сертификат не нужен.

Бэкап: `docker compose exec app tar -C /app/data -czf - . > backup.tgz` (БД + контент).

## Тесты

```bash
mvn test
```

`ImageProcessorTest` реально запускает `convert` (не мок) на
синтетическом тестовом фото (`src/test/resources/sample.jpg` — сгенерированный
градиент, не настоящий кадр ателье) и проверяет итоговые размеры и вес
файлов. **Для запуска тестов нужен установленный ImageMagick** (бинарь
`convert` в PATH) — на dev-машине он уже есть, в Docker-образе ставится
через `apk add imagemagick`.

## О конфигурации jte-maven-plugin

Плагин генерирует классы шаблонов из `src/main/jte/*.jte` на фазе
`process-classes` (автоматически внутри `mvn package`/`mvn test`). Ему
нужна явная конфигурация — без неё сборка падает с
`PluginParameterException` на `sourceDirectory`/`targetDirectory`/
`contentType`. Значения, которые сейчас в `pom.xml`, подобраны и
проверены сборкой; если после обновления версии jte снова появится
эта ошибка — здесь первое место для проверки.
