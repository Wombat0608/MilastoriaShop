# Milastoria — деплой на прод (VPS)

Короткий runbook. **Не блуждать:** здесь только реальные пути и команды,
которые уже проверены. Общая картина Docker/Caddy — в [`DEPLOYMENT.md`](DEPLOYMENT.md).

---

## 0. Факты (запомнить один раз)

| Что | Значение |
|-----|----------|
| SSH | `ssh milastoria` → `root@201.51.3.67`, ключ `~/.ssh/milastoria_vps` |
| Домен | `https://milastoria.com` (www → 301 на apex) |
| Дерево на VPS | `/opt/milastoria` (**не git** — только rsync) |
| Конфиг | `/opt/milastoria/backend/config/app.yml` (mode **600**, секреты) |
| Tom | Docker volume **`milastoria_milastoria-data`** → `/app/data` |
| Бэкапы | `/var/backups/milastoria-*.tgz` на VPS |
| Local path | `/mnt/storage/Projects/MilaStoria_Tilda` |

**Никогда** не rsync/cp поверх `backend/config/app.yml` на VPS.

---

## 1. Код перед деплоем (локально)

```bash
cd /mnt/storage/Projects/MilaStoria_Tilda

# 1) схема: в schema.sql НЕ должно быть CREATE INDEX на новые колонки
#    (см. §5 — иначе прод упадёт в crash-loop)
grep -n 'CREATE INDEX' backend/src/main/resources/schema.sql

# 2) миграция колонок — только в App.initSchema, ПОСЛЕ runScript(schema.sql)
grep -n 'addColumnIfMissing\|CREATE INDEX' backend/src/main/java/ru/milastoria/App.java

# 3) сборка/тесты (JDK 21, не PATH java)
export JAVA_HOME=/usr/lib/java/jdk-21   # или vscode Temurin 21 с javac
export PATH="$JAVA_HOME/bin:$PATH"
cd backend && mvn -B test
```

Коммитить/пушить в git **не обязательно** для VPS (там нет `.git`).
Рекомендуется для истории, но деплой идёт по rsync рабочей копии.

---

## 2. Бэкап тома (обязательно перед rebuild)

```bash
ssh milastoria 'set -e
cd /opt/milastoria
TS=$(date +%Y%m%d-%H%M%S)
mkdir -p /var/backups
docker compose exec -T app tar -C /app/data -czf - . \
  > /var/backups/milastoria-pre-deploy-$TS.tgz
ls -lh /var/backups/milastoria-pre-deploy-$TS.tgz
'
```

Восстановление (если всё плохо):

```bash
ssh milastoria 'set -e
cd /opt/milastoria
docker compose stop app
docker compose run --rm -v milastoria_milastoria-data:/data alpine \
  sh -c "rm -rf /data/* && tar -xzf - -C /data" \
  < /var/backups/milastoria-pre-deploy-XXXX.tgz
docker compose start app
'
```

---

## 3. rsync кода на VPS

```bash
cd /mnt/storage/Projects/MilaStoria_Tilda

# backend (ИСКЛЮЧАЕМ app.yml, target, data, jar, .idea)
rsync -av \
  --exclude 'target/' \
  --exclude '.idea/' \
  --exclude 'data/' \
  --exclude 'config/app.yml' \
  --exclude 'milastoria-app.jar' \
  --exclude 'dependency-reduced-pom.xml' \
  --exclude '.git/' \
  --exclude '*.db' \
  backend/ milastoria:/opt/milastoria/backend/

# корень (compose + Caddy; app.yml в compose монтируется, не перезаписываем)
rsync -av docker-compose.yml Caddyfile milastoria:/opt/milastoria/
```

Проверка, что секреты целы:

```bash
ssh milastoria 'stat -c "%a %n" /opt/milastoria/backend/config/app.yml'
# ожидается: 600 .../app.yml
```

---

## 4. Пересборка и старт

```bash
ssh milastoria 'set -e
cd /opt/milastoria
docker compose up -d --build
sleep 3
docker compose ps
docker compose logs app --since 2m | tail -40
'
```

**Ждём в логах:**
- `Миграция: добавлена колонка ...` (если колонка новая)
- `HikariPool-1 - Start completed`
- `Javalin started`
- **нет** `SQLiteException` / `Exception in thread "main"` / restarting

---

## 5. Правила миграции БД (важно)

Схема: `App.initSchema` = `runScript(schema.sql)` → `addColumnIfMissing` → индексы.

| Действие | Где |
|----------|-----|
| `CREATE TABLE IF NOT EXISTS ...` (новые таблицы/колонки в **новой** БД) | `schema.sql` |
| Добавить колонку на **существующей** БД | **только** `addColumnIfMissing` в `App.java` |
| `CREATE INDEX` на новую колонку | **только** в `App.java` **после** `addColumnIfMissing` |

**Запрещено** в `schema.sql`:

```sql
-- ТАК НЕЛЬЗЯ: на старой БД колонки ещё нет, runScript падает, app не стартует
CREATE INDEX IF NOT EXISTS idx_x ON media_files(new_col);
```

В `schema.sql` комментарии **без `;`** — `runScript` режет SQL по точке с запятой.

**Откат при crash-loop** (лог: `no such column: ...`):

1. Временно `docker compose stop app` (сайт может 502).
2. Поправить `schema.sql` / `App.java` локально.
3. rsync исправленных файлов → `docker compose up -d --build`.
4. Бэкап §2 лежит на месте — при необходимости восстановить том.

---

## 6. Smoke после деплоя

```bash
# контейнер
ssh milastoria 'cd /opt/milastoria && docker compose ps'

# логи без exception
ssh milastoria 'cd /opt/milastoria && docker compose logs app 2>&1 | grep -E "SQLiteException|Exception in thread" | tail || echo OK'

# HTTPS
curl -sI https://milastoria.com | head -5
curl -s -o /dev/null -w "home:%{http_code}\n" https://milastoria.com/
curl -s -o /dev/null -w "gallery:%{http_code}\n" https://milastoria.com/gallery
curl -s -o /dev/null -w "admin:%{http_code}\n" https://milastoria.com/admin   # 302 → login

# схема (в контейнере нет sqlite3 — копируем БД на хост)
ssh milastoria 'docker compose -f /opt/milastoria/docker-compose.yml exec -T app cat /app/data/milastoria.db > /tmp/prod.db'
python3 - <<'PY'
import sqlite3
c = sqlite3.connect("/tmp/prod.db")
print([r[1] for r in c.execute("PRAGMA table_info(media_files)")])
print(c.execute("SELECT COUNT(*) FROM media_files").fetchone())
PY
```

В админке: `/admin/media` — загрузка файла растит счётчик; фильтр
**Не прикреплённые / Прикреплённые / Все**; attach помечает `applied=1`,
файл остаётся на диске.

---

## 7. Ежедневные команды

```bash
# деплой нового кода
cd /mnt/storage/Projects/MilaStoria_Tilda
# (§2 бэкап → §3 rsync → §4 rebuild → §6 smoke)

# логи
ssh milastoria 'cd /opt/milastoria && docker compose logs -f app'

# перезапуск без пересборки
ssh milastoria 'cd /opt/milastoria && docker compose restart app'

# остановить (том НЕ удаляется)
ssh milastoria 'cd /opt/milastoria && docker compose down'
# НЕ делать без понимания: docker compose down -v  (удалит БД и фото)
```

---

## 8. Типичные боли

| Симптом | Причина | Действие |
|---------|---------|----------|
| `app` restarting, в логах `no such column: applied` | в `schema.sql` `CREATE INDEX` на новую колонку до `ALTER` | §5: индекс только в `App.java` после `addColumnIfMissing` |
| 502 от Caddy | app не поднялся | `docker compose logs app`; backup §2; откат |
| Загрузка в медиатеке «не растёт» | старый jar / нет рестарта / ошибка insert | §4 rebuild; смотреть логи app; smoke §6 |
| rsync перёкрутил пароль админа | скопировали `app.yml` с локала | восстановить с VPS backup / правильный prod `app.yml` |
| SSH/HTTPS «не отвечают» | VPN уносит трафик | `docs/DEPLOYMENT.md` §8.1 (`ip rule pref 100` → `201.51.3.67`) |
| `mvn` пишет `release version 21 not supported` | PATH `java` = Java 8 | `JAVA_HOME=/usr/lib/java/jdk-21` или Docker Maven temurin-21 |

---

## 9. Связанные файлы

| Файл | Что |
|------|-----|
| `docs/DEPLOYMENT.md` | полная инструкция (первичная настройка, Caddy, DNS) |
| `docker-compose.yml` | сервисы `app` + `caddy`, volume `milastoria-data` |
| `backend/Dockerfile` | multi-stage Maven → temurin-21-jre + IM + ffmpeg |
| `backend/config/app.yml.example` | шаблон конфига (сам `app.yml` не в git) |
| `Caddyfile` | TLS + reverse proxy |
