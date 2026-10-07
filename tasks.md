# tasks.md — лог задач Milastoria

Формат: одна строка = задача. Статусы: `open` / `in_progress` / `done` / `blocked`.
Обновлять при каждом шаге. Деплой без длинного e2e (user rule).

---

## Активные / недавние

| ID | Статус | Задача | Детали |
|----|--------|--------|--------|
| T1 | in_progress | HEAD `/sitemap.xml` Content-Type | На проде `content-type: text/plain` + `content-length: 0`; GET уже XML. Фикс локально: `SitemapController.applyXmlContentType` + `app.head("/sitemap.xml")` в `App.java`. Шаги: commit → rsync `backend/` → `docker compose up -d --build` → curl `-I`. Запуск: 2026-10-07T11:06Z |

---

## Готово (кратко)

| ID | Дата | Задача | Commit / факт |
|----|------|--------|---------------|
| — | 2026-10-07 | Auto-attach после кропа | `d6fa20e` live |
| — | 2026-10-07 | Analytics junk-filter | `4592ab9`/`6fc2067`/`d5f89df` live |
| — | 2026-10-06 | Media soft-attach `applied` + radio + bulk delete | `1d2a279`/`d1d0990`/`ca5da5d` live |
| — | 2026-10-06 | Hero autoplay 15s + admin hamburger | `2312486`/`3ef9a85` live |
| — | 2026-10-05 | Media attach UI + crop/WM + compress + polish | `751077c`…`b8d1bdb` live |

---

## Отложено (не блокирует T1)

| ID | Статус | Задача |
|----|--------|--------|
| — | open | SEO head: canonical / OG / JSON-LD Product |
| — | open | Legacy redirects `/catalog` → `/gallery` |
| — | optional | `git push` — master был ~10 commits ahead of origin (деплой идёт rsync, не git) |

---

## Правила (из MEMORY)

- Общаться на русском.
- Деплой: rsync + rebuild + smoke; **не** полный e2e, если не просят.
- Никогда rsync поверх VPS `backend/config/app.yml`.
- PATH `java`/`javac` = Java 8; сборка через `/usr/lib/java/jdk-21` или Docker temurin-21.
- Порт локально: **8081**.
- VPS: `ssh milastoria` → `/opt/milastoria` (не git); domain `milastoria.com`.
- MEMORY «OPEN» проверять по git + VPS, не слепо.

---

## Журнал T1

- 2026-10-07T11:06Z — старт: HEAD prod `text/plain`; SSH OK; контейнеры up.
- 2026-10-07T11:0xZ — commit `SitemapController` + `App.java` (HEAD route).
- 2026-10-07T11:0xZ — backup тома + rsync + rebuild.
- 2026-10-07T11:0xZ — smoke: HEAD/GET `/sitemap.xml`.
