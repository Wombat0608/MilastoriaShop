# tasks.md — лог задач Milastoria

Формат: одна строка = задача. Статусы: `open` / `in_progress` / `done` / `blocked`.
Обновлять при каждом шаге. Деплой без длинного e2e (user rule).

---

## Активные / недавние

| ID | Статус | Задача | Детали |
|----|--------|--------|--------|
| T1 | done | HEAD `/sitemap.xml` Content-Type | На проде было `text/plain` + `content-length: 0`. Фикс: `SitemapController.applyXmlContentType` (`ctx.res()`) + `app.head("/sitemap.xml")`. Commits `c20246c` + `64f4d71`. Deployed 2026-10-07T11:18Z. **HEAD live:** `application/xml;charset=utf-8`, `content-length: 5337`, `cache-control: max-age=3600`. GET 200 XML OK. home/gallery 200, admin 302. |

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
- 2026-10-07T11:13Z — backup тома `/var/backups/milastoria-pre-deploy-20261007-111344.tgz` (808M); rsync OK; `app.yml` mode 600 цел.
- 2026-10-07T11:14Z — rebuild FAILED: `SitemapController` `ctx.res` — в Javalin 6 это метод `ctx.res()`.
- 2026-10-07T11:16Z — fix `ctx.res()` + charset; commits `c20246c`, `64f4d71`.
- 2026-10-07T11:16Z — rsync pitfall: `backend/src` без `/` → nested `src/src` на VPS; старый файл не обновился. Повторный rsync `backend/src/` + `--delete` + `rm -rf .../src/src`; дерево 140/140 совпало.
- 2026-10-07T11:18Z — rebuild SUCCESS; Javalin started; smoke OK.
- 2026-10-07T11:18Z — **T1 done:** HEAD `https://milastoria.com/sitemap.xml` → `application/xml;charset=utf-8` + `content-length: 5337`.
