# Сборка и деплой Milastoria на продуктивной среде

Подробная инструкция по развертыванию сайта-лукбука **Milastoria** на VPS:
Docker-образ приложения + Caddy (HTTPS, Let's Encrypt).

Ориентирован на Ubuntu 22.04/24.04, регулярный VPS у российского
провайдера (Timeweb Cloud / Selectel / VK Cloud / Beget и т.п.).
Бюджет — в районе 750 ₽/мес: 1–2 vCPU, 2 ГБ RAM, 30 ГБ диска.

---

## Стек и схема развёртывания

```
Интернет
   │
   ▼
┌─────────────────────────────────────────────────────────┐
│  VPS (Ubuntu + Docker + Compose)                        │
│                                                         │
│  ┌──────────┐     HTTPS      ┌────────────────────────┐ │
│  │  Caddy   │ ─────────────► │  milastoria-app        │ │
│  │ :80/:443 │  reverse_proxy │  Javalin, порт 8080    │ │
│  └──────────┘                │  ImageMagick + ffmpeg  │ │
│      │                       └───────────┬────────────┘ │
│      │ ACME (Let's Encrypt)              │              │
│      ▼                                   ▼              │
│  caddy_data / caddy_config      volume milastoria-data  │
│                                /app/data                │
│                                ├── milastoria.db        │
│                                ├── branding/            │
│                                └── content/             │
│                                    ├── img/             │
│                                    ├── video/           │
│                                    ├── originals/       │
│                                    └── media/           │
└─────────────────────────────────────────────────────────┘
```

| Компонент | Роль | Где живёт |
|-----------|------|-----------|
| `backend/` | Java 21 + Javalin + MyBatis + jte + SQLite | multi-stage Docker-образ |
| `backend/config/app.yml` | секреты и пути | на сервере, **не в git** |
| `Caddyfile` | TLS + reverse proxy | корень репозитория |
| `docker-compose.yml` | сервисы `app` + `caddy` | корень репозитория |
| том `milastoria-data` | БД, контент, watermark | Docker volume на VPS |

Почему именно так:

- **Один jar, без Spring** — приложение стартует быстро, память в пределах
  200–400 МБ.
- **SQLite в volume** — бэкап = копия файла БД + каталога `content/`.
- **Caddy** — сертификат Let's Encrypt выпускается и продлевается сам;
  платный сертификат не нужен, ACME из РФ работает.

---

## 1. Требования

### Хостинг (VPS)

- ОС: Ubuntu 22.04 или 24.04.
- Ресурсы: от 1 vCPU / 1 ГБ RAM (комфортно — 2 vCPU / 2 ГБ).
- Диск: 30 ГБ+ (оригиналы фото и медиатека растут быстрее, чем кажется).
- Docker + Docker Compose plugin.
- Открытые порты: **80** и **443** (и **22** для SSH).

### Домен

- DNS-записи у регистратора (в проекте — Reg.ru):
  - `A  milastoria.com  → IP VPS`
  - `A  www.milastoria.com → IP VPS` (Caddy сделает permanent redirect на apex)
- **Домен должен резолвиться в IP VPS до** `docker compose up` — иначе Caddy
  не сможет получить сертификат Let's Encrypt (HTTP-01 challenge).

Если домен сейчас на Tilda и вы переезжаете: сначала поднимите сайт на
IP, убедитесь, что HTTPS и контент работают, **потом** меняете A-записи.

### Локальная машина (для сборки/публикации кода)

- Git, доступ к репозиторию.
- JDK 21 + Maven — только если хотите проверить jar локально **или**
  собрать образ вне Docker.
- ImageMagick (`convert` или `magick`) — нужен, если запускаете `mvn test`
  локально (тесты реально вызывают ImageMagick).

Сборка на VPS через Docker **не требует** Java/Maven на хосте: multi-stage
Dockerfile тянет `maven:3.9-eclipse-temurin-21` сам.

---

## 2. Первоначальная настройка VPS

### 2.1. Скрипт установки Docker

В репозитории есть `scripts/setup-server.sh` (важно: каталог `scripts/`
в `.gitignore` — скрипт может отсутствовать в git-клоне; возьмите его
из рабочей копии или выполните шаги вручную).

```bash
# на сервере от root
# если скрипта нет в git — выполните его содержимое вручную:
#   apt-get update && apt-get install -y ca-certificates curl gnupg git
#   ... (см. setup-server.sh: Docker CE + compose plugin)
bash /path/to/scripts/setup-server.sh
```

Проверка:

```bash
docker --version
docker compose version
```

### 2.2. Каталог проекта и git-доступ

```bash
mkdir -p /opt/milastoria
# клонируйте репозиторий (SSH-ключ уже добавлен в GitHub/GitLab)
git clone git@github.com:<org>/<repo>.git /opt/milastoria
cd /opt/milastoria
```

Если git-контур пока нет — можно раскопировать рабочую копию
без `target/`, `Сырье/`, `.idea/` и локального `backend/config/app.yml`
через `rsync`/`scp`. В git-сборке достаточно репозитория.

### 2.3. Фаервол

Убедитесь, что 80/443 открыты (Docker сам поднимает правила, но
`ufw`/провайдерский firewall может ограничивать).

```bash
# при использовании ufw
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
ufw status
```

---

## 3. Конфигурация (`app.yml`)

Главный файл — **`backend/config/app.yml`**. В git лежит только
`app.yml.example`; сам `app.yml` в `.gitignore`.

```bash
cd /opt/milastoria
cp backend/config/app.yml.example backend/config/app.yml
```

Отредактируйте:

```yaml
admin:
  user: admin
  password: <СЛОЖНЫЙ_ПАРОЛЬ_АДМИНА>
  session_secret: <ДЛИННАЯ_СЛУЧАЙНАЯ_СТРОКА_32+_БАЙТА>

server:
  port: 8080

storage:
  content_dir: /app/data/content
  db_path: /app/data/milastoria.db

site:
  base_url: https://milastoria.com   # ваш боевой домен

image:
  convert: magick                     # в Docker уже переопределяется ENV
```

### Правила генерации секретов

```bash
# пароль администратора
openssl rand -base64 24

# session_secret (минимум 32 байта, лучше 48–64)
openssl rand -base64 48 | tr -d '\n'
```

При старте приложение пишет WARN, если `password=admin` или
`session_secret=dev-only-insecure-secret-change-me`. **Прод нельзя
запускать с этими значениями.**

### Приоритет значений

```
переменные окружения  >  app.yml  >  дефолты в коде
```

В `docker-compose.yml` для сервиса `app` уже заданы:

| ENV | Значение в compose | Зачем |
|-----|--------------------|-------|
| `APP_CONFIG` | `/app/config/app.yml` | путь к YAML внутри контейнера |
| `PORT` | `8080` | порт Jetty |
| `SITE_BASE_URL` | `https://milastoria.com` | sitemap/canonical |
| `CONTENT_DIR` | `/app/data/content` | img/video/originals/media |
| `DB_PATH` | `/app/data/milastoria.db` | SQLite |
| `CONVERT_BIN` | `magick` (из Dockerfile) | ImageMagick v7 |

Секреты **можно** держать только в `app.yml` (смонтирован read-only) —
обычно так и делают. Если вынесете в ENV — уберите/закомментируйте
строки в compose, чтобы не дублировать.

### Структура тома `milastoria-data` → `/app/data`

| Путь | Что лежит | Откуда |
|------|-----------|--------|
| `milastoria.db` | SQLite | схема+seed при первом старте; далее данные админки |
| `branding/watermark.png` | watermark | распаковывается из jar при старте |
| `content/img/` | thumb/full, обложки | админка + перенос из прототипа |
| `content/video/` | видео | админка |
| `content/originals/` | нетронутые оригиналы | админка |
| `content/media/` | медиатека (до attach) | админка |

Директория `content_dir` — **настоящая папка**, не симлинк
(Jetty молча 404-ит через symlink).

---

## 4. Сборка и запуск

### 4.1. Первый запуск

```bash
cd /opt/milastoria
# проверьте app.yml (секреты, base_url)
docker compose up -d --build
```

Что происходит:

1. Слой `build`: Maven собирает fat-jar `milastoria-app.jar`
   (`-DskipTests` внутри Dockerfile).
2. Слой `runtime`: `eclipse-temurin:21-jre-alpine` + ImageMagick
   (jpeg/heic/webp/tiff) + ffmpeg.
3. `app` монтирует `backend/config/app.yml` → `/app/config/app.yml:ro`
   и том `milastoria-data` → `/app/data`.
4. `caddy` берёт `Caddyfile`, стартует на 80/443, сам запрашивает
   сертификат по HTTP-01 на `milastoria.com`.

### 4.2. Логи и статус

```bash
docker compose ps
docker compose logs -f app
docker compose logs -f caddy
```

В логах `app` при успехе видно старт Jetty; при проблемах с секретами —
WARN про dev-пароль; при отсутствии `convert`/`magick` — ошибки
ImageProcessor при загрузке фото.

### 4.3. Проверка после старта

```bash
# HTTPS на домене (не через IP — для сертификата нужен hostname)
curl -I https://milastoria.com
curl -I https://www.milastoria.com   # должен быть 301 → https://milastoria.com

# внутренний контур (на VPS, без TLS)
docker compose exec app wget -qO- http://127.0.0.1:8080/ | head
```

В браузере:

| URL | Ожидание |
|-----|----------|
| `https://milastoria.com/` | главная, hero, категории |
| `https://milastoria.com/gallery` | сетка лотов |
| `https://milastoria.com/sitemap.xml` | sitemap с боевым доменом |
| `https://milastoria.com/admin` | redirect на `/admin/login` |
| `https://milastoria.com/admin/login` | форма входа |

Войдите в админку **боевым** логином/паролем из `app.yml`.
`admin`/`admin` — только dev.

### 4.4. Ошибки HTTPS и Let's Encrypt

| Симптом | Причина | Действие |
|---------|---------|----------|
| Caddy: `no certificate available` / connection refused | DNS ещё не указывает на VPS | ждём делегирования/распространения DNS, `dig +short milastoria.com` |
| `403 Forbidden` от challenge | порт 80 закрыт / firewall | открыть 80 |
| cert выдан, но браузер ругается | старый DNS кэш / не тот VPS | проверить `dig` с публичного DNS |
| `caddy:5` / rate limit ACME | слишком частые старты | подождать; не спамить `up --build` |

Диагностика Caddy:

```bash
docker compose logs caddy | tail -50
docker compose exec caddy caddy version
# данные сертификатов
docker volume inspect milastoria_caddy_data
```

---

## 5. Контент: демо vs боевые данные

### 5.1. Seed-данные

При **пустой** таблице `lots` приложение выполняет `seed.sql`:
демо-категории и ~10 лотов с путями `/content/...`.

- **Прод для разработчика / preview** — seed удобен, чтобы сразу увидеть
  вёрстку.
- **Боевой публичный сайт** — демо-фото и демо-тексты **не должны**
  остаться на публичной странице. Варианты:
  1. Залейте настоящий контент через `/admin` и поменяйте/удалите демо-лоты
     (удаление лотов в админке пока не реализовано — см. `backend/README.md`);
  2. либо начните с **чистой** БД: не разрешайте seed и сразу заводите
     реальные разделы/лоты в админке.

Порядок seed-данных (реальные пути `path_thumb`/`path_full`) ссылаются на
`/content/img/...` — файлы должны лежать в томе. Если картинок нет,
карточки будут с битыми превью. Переносите демо-картинки **в том** до того,
как сеете БД:

```bash
# пример: скопировать демо-ассеты прототипа в контейнер
# (на dev-машине, где есть prototype/assets)
rsync -a prototype/assets/ /opt/milastoria-data-preview/content/

# внутри запущенного контейнера (если volume уже создан)
docker compose exec app sh -c 'mkdir -p /app/data/content && cp -a /app/data/content/. /tmp/x 2>/dev/null; true'
```

Чище — на VPS сразу в том:

```bash
# имя volume
docker volume ls | grep milastoria

# получить путь host'а к volume (нужен root)
docker volume inspect milastoria_milastoria-data --format '{{.Mountpoint}}'
# → обычно /var/lib/docker/volumes/milastoria_milastoria-data/_data

sudo rsync -a /path/from/local/prototype/assets/ \
  /var/lib/docker/volumes/milastoria_milastoria-data/_data/content/
```

Альтернатива без volume: залейте через админку (оригиналы + кроп) —
это **правильный** прод-путь: оригиналы остаются в `originals/`, thumb/full
пересчитываются ImageMagick.

### 5.2. Наполнение сайта в проде

1. Войдите в `/admin`.
2. Разделы → создать боевые категории (slug, title, обложка).
3. Разделы/другое → hero, about, contacts, слоганы.
4. Лоты → создать работу, загрузить фото (iOS: «Библиотека фото» /
   «Сделать фото»), кроп на весь экран, «Готово».
5. Медиатека → прикрепить фото/видео к лоту.
6. Водяной знак → настроить размер/положение (есть отдельный экран).
7. SEO: title/description/alt на лотах и категориях.
8. Публикация: `status = published` (черновики не отдаются наружу).

Лимиты загрузки в коде (из `App.java`):

- максимальный файл: **50 МБ**
- максимальный запрос: **80 МБ**
- до 4 МБultipart держится в памяти, остальное на диск

---

## 6. Обновление приложения

```bash
cd /opt/milastoria
git pull
docker compose up -d --build
docker compose logs -f app   # дождитесь старта
```

Рекомендуемый порядок (БД без миграционного движка на v1):

1. **Бэкап тома** (см. §7) до `git pull` / rebuild.
2. `git pull` + `up -d --build`.
3. Проверить `/`, `/gallery`, `/admin`, загрузку фото.
4. При сбое — откат кода: `git checkout <тег>` + rebuild + восстановить
   backup БД, если schema.sql менялся несовместимо.

В `App.java` схема применяется `CREATE TABLE IF NOT EXISTS` + ручные
`ALTER TABLE ... ADD COLUMN IF NOT EXISTS`-проверки. Простые добавления
колонок переживают обновление; **смены типов/переименования** требуют
ручной миграции файла БД.

---

## 7. Бэкапы

Всё важное живёт в томе `milastoria-data`.

### 7.1. Ручная копия

```bash
cd /opt/milastoria
docker compose exec app tar -C /app/data -czf - . > backup-$(date +%Y%m%d-%H%M%S).tgz
```

Восстановление:

```bash
docker compose stop app
docker compose run --rm -v milastoria_milastoria-data:/data alpine \
  sh -c 'rm -rf /data/* && tar -xzf - -C /data' < backup-XXXX.tgz
# либо на хосте, снимая mountpoint volume
docker compose start app
```

### 7.2. Cron + Яндекс Object Storage (rclone)

Пример: ежедневно в 03:30.

```bash
apt-get install -y rclone
rclone config   # remote: yandex, тип S3-совместимый, ключи Storage
```

`/root/backup-milastoria.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail
TS=$(date +%Y%m%d-%H%M%S)
ARCHIVE=/var/backups/milastoria-$TS.tgz
mkdir -p /var/backups
docker compose -f /opt/milastoria/docker-compose.yml \
  exec -T app tar -C /app/data -czf - . > "$ARCHIVE"
rclone copy "$ARCHIVE" yandex:milastoria-backups/
find /var/backups -name 'milastoria-*.tgz' -mtime +7 -delete
```

`crontab -e`:

```cron
30 3 * * * /root/backup-milastoria.sh >> /var/log/milastoria-backup.log 2>&1
```

### 7.3. Что бэкапить всегда

- `milastoria.db`
- `content/**` (img, video, originals, media)
- `branding/` (watermark — есть и в jar, но удобно и тут)

**Не** путайте бэкап с кодом: `docker compose build` восстанавливает
jar, но не БД и не загруженные оригиналы.

---

## 8. Мониторинг и эксплуатация

```bash
# ресурсы контейнеров
docker stats

# диск
df -h

# здоровье app без TLS
docker compose exec app wget -qO- http://127.0.0.1:8080/ >/dev/null && echo OK

# последний backup
ls -lh /var/backups/milastoria-*.tgz | tail
```

В админке есть `/admin/analytics` (серверная статистика посещений
без внешних трекеров).

Перезапуск:

```bash
docker compose restart app
# полный cold start
docker compose down && docker compose up -d
```

`docker compose down` **не** удаляет volume; `docker compose down -v` —
удаляет (в т.ч. БД и фото). Делать только осознанно.

### 8.1. Доступ к проду, если открыты VPN-туннели

На рабочей машине могут быть два VPN сразу: корпоративный (например
`vpn0`) и частный full-tunnel (например `tun0`, policy routing
`lookup 880`). Частный туннель часто уносит **весь** интернет,
в том числе IP прода Milastoria, поэтому `https://milastoria.com`
и SSH на VPS «ломаются», хотя на VPS всё ок.

Подставляйте боевой IP прода, если он отличается (сейчас в проекте —
`milastoria.com`):

| Домен | IP (приблизительно) |
|-------|---------------------|
| `milastoria.com` / `www` | `201.51.3.67` |
| `milastoria.ru` | `149.62.51.146` |

**Добавить временный обход** (маршрут в `main`, приоритет `pref 100` —
тот же паттерн, что уже может стоять для других IP в вашей сети):

```bash
# 201.51.3.67 — milastoria.com
sudo ip rule add to 201.51.3.67/32 lookup main pref 100
sudo ip route add 201.51.3.67 via 192.168.68.1 dev enp6s0 metric 50

# 149.62.51.146 — milastoria.ru (если нужно)
sudo ip rule add to 149.62.51.146/32 lookup main pref 100
sudo ip route add 149.62.51.146 via 192.168.68.1 dev enp6s0 metric 50
```

`via` / `dev` / `src` — шлюз и интерфейс **локальной** сети (у типичного
десктопа — default-маршрут, например `192.168.68.1` и `enp6s0`).
Запустите `ip route` и возьмите первый `default via ... dev ...`.

**Проверка:**

```bash
ip rule show | grep -E 'milastoria|201\.51\.3\.67|149\.62\.51\.146' || true
ip route get 201.51.3.67
# ожидается: via <шлюз> dev <enp...> — НЕ dev tun0
curl -sI https://milastoria.com | head -5
```

**Убрать обход** (вернуть трафик туннелю):

```bash
sudo ip rule del to 201.51.3.67/32 lookup main pref 100
sudo ip route del 201.51.3.67 via 192.168.68.1 dev enp6s0 metric 50
sudo ip rule del to 149.62.51.146/32 lookup main pref 100
sudo ip route del 149.62.51.146 via 192.168.68.1 dev enp6s0 metric 50
```

Замечания:

- правила **временные**: после reboot или пересоздания VPN-routing
  их нужно добавить заново;
- они не влияют на трафик **с других** машин и на то, что ходит через
  туннель «как раньше» (корп/приватные IP);
- если VPS сменит IP — обновите эти два адреса;
- если интерфейс/шлюз на вашей машине другие — подставьте свои.

---

## 9. Проверочный чек-лист продакшена

Перед открытием трафика:

- [ ] `docker compose ps` — `app` и `caddy` healthy/up
- [ ] `https://milastoria.com` — 200, сертификат Let's Encrypt
- [ ] `https://www.milastoria.com` → 301 на apex
- [ ] `/` и `/gallery` отдают HTML (не пустой JS-каркас)
- [ ] `/sitemap.xml` содержит `https://milastoria.com`
- [ ] `/admin` без логина → redirect на login
- [ ] Логин/пароль — **не** `admin`/`admin`; session_secret не dev-значение
- [ ] В логах `app` **нет** WARN про dev-пароль
- [ ] На сайте **нет** демо-лотов/пустых карточек, если это боевой публичный URL
- [ ] Загрузка фото через `/admin` работает (появляются thumb/full в томе)
- [ ] Водяной знак подставляется на new кроп
- [ ] Бэкап снят и лежит вне тома / в объектном хранилище
- [ ] SSH-доступ ограничен ключом, парольный вход отключён (рекомендуется)
- [ ] DNS TTL проверен; домен указывает на IP VPS

---

## 10. Локальная сборка (опционально)

Без Docker — когда нужно быстро запустить jar:

```bash
cd backend
mvn -B package
java -jar target/milastoria-app.jar
```

`config/app.yml` локально уже с dev-путями:

- `content_dir: ../prototype/assets`
- `db_path: data/milastoria.db`
- `port: 8081` (если 8080 занят)
- `convert: convert` (ImageMagick 6 на машине) или `magick`

Локальный HTTP: `http://localhost:8081/`

Тесты (нужен ImageMagick в PATH):

```bash
cd backend && mvn -B test
```

Сборка образа локально и запуск без Caddy (TLS-слой только на VPS):

```bash
cd backend
docker build -t milastoria-app .
docker run -p 8080:8080 \
  -e APP_CONFIG=/app/config/app.yml \
  -v "$PWD/config/app.yml:/app/config/app.yml:ro" \
  -v milastoria-data:/app/data \
  milastoria-app
```

---

## 11. Частые проблемы

| Проблема | Что проверить |
|----------|----------------|
| 502 от Caddy | `docker compose logs app`; порт 8080; `APP_CONFIG`/том смонтирован |
| Пустая галерея | seed не выполнился (БД не пуста) или контент не в `/app/data/content` |
| 404 на `/content/img/...` | файлы не лежат в томе; `CONTENT_DIR` != фактический путь |
| Не декодируется jpg/heic при кропе | в образе нет subpackages ImageMagick — пересоберите `docker compose build app` |
| Ошибка при загрузке фото | лимит 50/80 МБ; `magick` в контейнере; права на том |
| FTS/поиск пуст | пересборка индекса на старте; смотреть логи `LotSearchIndex` |
| Сессия админки «слетает» | разный `session_secret` между рестартами/копиями `app.yml` |
| Падение `mvn package` на jte | смотрите `jte-maven-plugin` в `backend/pom.xml` (sourceDirectory/targetDirectory) |
| Jetty 404 на статике из тома | `content_dir` — симлинк; замените на реальную директорию |
| Вложенные SQL-дедлок | Hikari pool=1; все SQL в одном запросе — один `SqlSession` |

---

## 12. Сводка команд «на каждый день»

```bash
# деплой нового кода
cd /opt/milastoria && git pull && docker compose up -d --build

# логи
docker compose logs -f app

# бэкап
docker compose exec app tar -C /app/data -czf - . > backup-$(date +%F).tgz

# перезапуск
docker compose restart app

# остановка (без удаления данных)
docker compose down
```

---

## Связанные файлы

| Файл | Назначение |
|------|------------|
| [`docker-compose.yml`](../docker-compose.yml) | сервисы `app` + `caddy`, volume |
| [`Caddyfile`](../Caddyfile) | TLS, reverse proxy, security headers |
| [`backend/Dockerfile`](../backend/Dockerfile) | multi-stage сборка + ImageMagick + ffmpeg |
| [`backend/config/app.yml.example`](../backend/config/app.yml.example) | шаблон конфигурации |
| [`backend/README.md`](../backend/README.md) | архитектура бэкенда, запуск локально |
| [`ARCHITECTURE.md`](../ARCHITECTURE.md) | зафиксированные решения (Javalin, SQLite, SSR, кроп) |
| [`scripts/setup-server.sh`](../scripts/setup-server.sh) | первичная установка Docker на VPS (не в git) |
