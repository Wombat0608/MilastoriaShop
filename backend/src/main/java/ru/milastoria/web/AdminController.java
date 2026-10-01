package ru.milastoria.web;

import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.milastoria.domain.Category;
import ru.milastoria.domain.Lot;
import ru.milastoria.domain.LotImage;
import ru.milastoria.image.CropRect;
import ru.milastoria.image.ImageProcessor;
import ru.milastoria.mapper.CategoryMapper;
import ru.milastoria.mapper.DictMapper;
import ru.milastoria.mapper.LotMapper;
import ru.milastoria.mapper.SettingsMapper;
import ru.milastoria.mapper.SloganMapper;
import ru.milastoria.image.WatermarkPlacement;
import ru.milastoria.search.LotSearchIndex;
import ru.milastoria.util.Fts;
import ru.milastoria.view.AdminLotsView;
import ru.milastoria.view.AdminLotRow;
import ru.milastoria.view.AdminPhotosView;
import ru.milastoria.view.CategoriesView;
import ru.milastoria.view.CategoryFormView;
import ru.milastoria.view.ContactsView;
import ru.milastoria.view.LotFormView;
import ru.milastoria.view.SlogansView;
import ru.milastoria.view.WatermarkView;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Админка: логин, список лотов + поиск, CRUD лотов и разделов,
 * загрузка/кроп/удаление фото. Поиск — тот же FTS5, что и на сайте.
 */
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private static final String SESSION_COOKIE = "admin_session";

    private final SqlSessionFactory sqlSessionFactory;
    private final TemplateEngine templateEngine;
    private final ImageProcessor imageProcessor;
    private final Auth auth;
    private final Path contentDir;
    private final Path originalsDir;

    public AdminController(SqlSessionFactory sqlSessionFactory,
                            TemplateEngine templateEngine,
                            ImageProcessor imageProcessor,
                            Auth auth,
                            Path contentDir,
                            Path originalsDir) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.templateEngine = templateEngine;
        this.imageProcessor = imageProcessor;
        this.auth = auth;
        this.contentDir = contentDir;
        this.originalsDir = originalsDir;
    }

    public boolean isAuthenticated(Context ctx) {
        return auth.isValidSession(ctx.cookie(SESSION_COOKIE));
    }

    public void loginForm(Context ctx) {
        render(ctx, "admin/login.jte", (String) null);
    }

    public void login(Context ctx) {
        String user = ctx.formParam("username");
        String pass = ctx.formParam("password");
        if (auth.checkCredentials(user, pass)) {
            ctx.cookie(SESSION_COOKIE, auth.issueSessionCookie(), 60 * 60 * 24 * 7);
            ctx.redirect("/admin");
        } else {
            render(ctx, "admin/login.jte", "Неверный логин или пароль");
        }
    }

    public void logout(Context ctx) {
        ctx.removeCookie(SESSION_COOKIE);
        ctx.redirect("/admin/login");
    }

    // ─────────────── лоты: список + поиск ───────────────

    public void dashboard(Context ctx) {
        String rawQuery = normalize(ctx.queryParam("q"));
        String notice = ctx.queryParam("notice");
        String ftsQuery = Fts.toMatchExpression(rawQuery);
        List<Long> categoryIds = parseCategoryIds(ctx.queryParams("categories"));

        try (SqlSession session = sqlSessionFactory.openSession()) {
            LotMapper mapper = session.getMapper(LotMapper.class);
            CategoryMapper categoryMapper = session.getMapper(CategoryMapper.class);
            List<Category> categories = categoryMapper.findAll();
            List<AdminLotRow> lots = (ftsQuery == null)
                    ? mapper.findAllAdmin(categoryIds)
                    : mapper.searchAdmin(ftsQuery, categoryIds);
            render(ctx, "admin/dashboard.jte",
                    new AdminLotsView(lots, rawQuery, categories, categoryIds, notice));
        }
    }

    /** ?categories=1,2,3 или повторяющиеся параметры — id разделов. */
    private static List<Long> parseCategoryIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            for (String part : value.split(",")) {
                String raw = part.trim();
                if (raw.isEmpty()) {
                    continue;
                }
                try {
                    long id = Long.parseLong(raw);
                    if (id > 0 && !ids.contains(id)) {
                        ids.add(id);
                    }
                } catch (NumberFormatException ignored) {
                    // мусор в URL — пропускаем
                }
            }
        }
        return ids;
    }

    // ─────────────── лоты: форма create/edit ───────────────

    public void lotFormNew(Context ctx) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            render(ctx, "admin/lot-form.jte", emptyLotForm(session, null, null));
        }
    }

    public void lotFormEdit(Context ctx) {
        long lotId = Long.parseLong(ctx.pathParam("id"));
        String error = ctx.queryParam("error");
        String notice = ctx.queryParam("notice");

        try (SqlSession session = sqlSessionFactory.openSession()) {
            LotMapper lotMapper = session.getMapper(LotMapper.class);
            Lot lot = lotMapper.findById(lotId);
            if (lot == null) {
                ctx.status(404).result("Лот не найден");
                return;
            }
            DictMapper dict = session.getMapper(DictMapper.class);
            render(ctx, "admin/lot-form.jte", LotFormView.edit(
                    lot,
                    session.getMapper(CategoryMapper.class).findAll(),
                    dict.findNamesByLot(lotId, "occasion"),
                    dict.findNamesByLot(lotId, "fabric"),
                    dict.findNamesByLot(lotId, "tag"),
                    dict.findNamesByKind("occasion"),
                    dict.findNamesByKind("fabric"),
                    dict.findNamesByKind("tag"),
                    error, notice));
        }
    }

    public void createLot(Context ctx) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String error = validateLotForm(session, ctx, null);
            if (error != null) {
                render(ctx, "admin/lot-form.jte", emptyLotForm(session, ctx, error));
                return;
            }

            LotMapper mapper = session.getMapper(LotMapper.class);
            DictSelection dictSel = readDictForm(ctx);
            Lot lot = lotFromForm(ctx, null, dictSel);
            lot.setCreatedAt(Instant.now().toString());
            if (lot.getSort() == 0) {
                lot.setSort(mapper.maxSort() + 1);
            }
            mapper.insert(lot);
            saveDicts(session, lot.getId(), dictSel);
            LotSearchIndex.indexLot(session, lot, mapper.findTagNamesByLotId(lot.getId()));
        }
        ctx.redirect("/admin?notice=" + urlEncode("Лот «" + formParam(ctx, "title") + "» создан"));
    }

    public void updateLot(Context ctx) {
        long lotId = Long.parseLong(ctx.pathParam("id"));
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            LotMapper mapper = session.getMapper(LotMapper.class);
            Lot existing = mapper.findById(lotId);
            if (existing == null) {
                ctx.status(404).result("Лот не найден");
                return;
            }
            // Валидация — в той же сессии: HikariCP с pool=1, вторая сессия deadlock'ит
            String error = validateLotForm(session, ctx, lotId);
            DictSelection dictSel = readDictForm(ctx);
            if (error != null) {
                render(ctx, "admin/lot-form.jte", draftLotForm(session, ctx, existing, dictSel, error));
                return;
            }
            Lot lot = lotFromForm(ctx, lotId, dictSel);
            lot.setCreatedAt(existing.getCreatedAt());
            if (lot.getSort() == 0) {
                lot.setSort(existing.getSort());
            }
            mapper.update(lot);
            saveDicts(session, lotId, dictSel);
            LotSearchIndex.indexLot(session, lot, mapper.findTagNamesByLotId(lotId));
        }
        ctx.redirect("/admin?notice=" + urlEncode("Лот обновлён"));
    }

    // ─────────────── разделы ───────────────

    public void categoriesPage(Context ctx) {
        String error = ctx.queryParam("error");
        String notice = ctx.queryParam("notice");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<Category> categories = session.getMapper(CategoryMapper.class).findAll();
            render(ctx, "admin/categories.jte", new CategoriesView(categories, error, notice));
        }
    }

    /** Пересортировка разделов: formParam order = "id1,id2,..." */
    public void reorderCategories(Context ctx) {
        String order = formParam(ctx, "order");
        if (order.isBlank()) {
            ctx.status(400).result("Пустой порядок");
            return;
        }
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            CategoryMapper mapper = session.getMapper(CategoryMapper.class);
            var owned = new java.util.HashSet<Long>();
            for (Category cat : mapper.findAll()) {
                owned.add(cat.getId());
            }
            int sort = 1;
            for (String part : order.split(",")) {
                String raw = part.trim();
                if (raw.isEmpty()) {
                    continue;
                }
                long id;
                try {
                    id = Long.parseLong(raw);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (!owned.contains(id)) {
                    continue;
                }
                mapper.updateSort(id, sort++);
            }
        }
        ctx.status(204);
    }

    public void categoryFormNew(Context ctx) {
        render(ctx, "admin/category-form.jte", new CategoryFormView(null, null, null));
    }

    public void categoryFormEdit(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Category category = session.getMapper(CategoryMapper.class).findById(id);
            if (category == null) {
                ctx.status(404).result("Раздел не найден");
                return;
            }
            render(ctx, "admin/category-form.jte", new CategoryFormView(
                    category, ctx.queryParam("error"), ctx.queryParam("notice")));
        }
    }

    public void createCategory(Context ctx) {
        String title = formParam(ctx, "title");
        String slug = formParam(ctx, "slug");
        if (slug.isBlank()) {
            slug = Fts.slugify(title);
        } else {
            slug = Fts.slugify(slug);
        }
        String seoText = nullIfBlank(formParam(ctx, "seo_text"));

        if (title.isBlank()) {
            ctx.redirect("/admin/categories/new?error=" + urlEncode("Название раздела обязательно"));
            return;
        }
        if (!Fts.isValidSlug(slug)) {
            ctx.redirect("/admin/categories/new?error=" + urlEncode("Не удалось построить slug — заполните латиницей"));
            return;
        }

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            CategoryMapper mapper = session.getMapper(CategoryMapper.class);
            if (mapper.existsSlug(slug)) {
                ctx.redirect("/admin/categories/new?error=" + urlEncode("Раздел со slug «" + slug + "» уже есть"));
                return;
            }
            Category category = new Category();
            category.setTitle(title);
            category.setSlug(slug);
            category.setSeoText(seoText);
            category.setCreatedAt(Instant.now().toString());
            category.setSort(mapper.maxSort() + 1);
            String coverPath = processCategoryCover(ctx, slug);
            category.setCoverImage(coverPath);
            mapper.insert(category);
        }
        ctx.redirect("/admin/categories?notice=" + urlEncode("Раздел «" + title + "» создан"));
    }

    public void updateCategory(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        String title = formParam(ctx, "title");
        String slug = formParam(ctx, "slug");
        if (slug.isBlank()) {
            slug = Fts.slugify(title);
        } else {
            slug = Fts.slugify(slug);
        }
        String seoText = nullIfBlank(formParam(ctx, "seo_text"));

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            CategoryMapper mapper = session.getMapper(CategoryMapper.class);
            Category existing = mapper.findById(id);
            if (existing == null) {
                ctx.status(404).result("Раздел не найден");
                return;
            }
            if (title.isBlank()) {
                ctx.redirect("/admin/categories/" + id + "/edit?error=" + urlEncode("Название раздела обязательно"));
                return;
            }
            if (!Fts.isValidSlug(slug)) {
                ctx.redirect("/admin/categories/" + id + "/edit?error=" + urlEncode("Slug должен быть латиницей: a-z, 0-9, дефисы"));
                return;
            }
            if (!slug.equals(existing.getSlug()) && mapper.existsSlug(slug)) {
                ctx.redirect("/admin/categories/" + id + "/edit?error=" + urlEncode("Раздел со slug «" + slug + "» уже есть"));
                return;
            }
            Category category = new Category();
            category.setId(id);
            category.setTitle(title);
            category.setSlug(slug);
            category.setSeoText(seoText);
            category.setCoverImage(existing.getCoverImage());
            String coverPath = processCategoryCover(ctx, slug);
            if (coverPath != null) {
                category.setCoverImage(coverPath);
            }
            mapper.update(category);
        }
        ctx.redirect("/admin/categories?notice=" + urlEncode("Раздел обновлён"));
    }

    // ─────────────── фото лота ───────────────

    public void photosPage(Context ctx) {
        long lotId = Long.parseLong(ctx.pathParam("id"));
        try (SqlSession session = sqlSessionFactory.openSession()) {
            LotMapper mapper = session.getMapper(LotMapper.class);
            Lot lot = mapper.findById(lotId);
            if (lot == null) {
                ctx.status(404).result("Лот не найден");
                return;
            }
            double[] wm = loadWmDefaults(session);
            render(ctx, "admin/photos.jte",
                    new AdminPhotosView(lot, mapper.findImagesByLotId(lotId), null, wm[0], wm[1]));
        }
    }

    /** Страница настроек watermark: файл + размер по умолчанию. */
    public void watermarkPage(Context ctx) {
        String error = ctx.queryParam("error");
        String notice = ctx.queryParam("notice");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            double[] wm = loadWmDefaults(session);
            boolean hasFile = Files.exists(imageProcessor.watermarkFile());
            render(ctx, "admin/watermark.jte", new WatermarkView(
                    String.valueOf(Math.round(wm[0] * 100)),
                    String.valueOf(Math.round(wm[1] * 100)),
                    hasFile, error, notice));
        }
    }

    public void saveWatermarkSettings(Context ctx) {
        double widthPct = parsePercent(ctx.formParam("width_percent"), 18);
        double marginPct = parsePercent(ctx.formParam("margin_percent"), 2);
        UploadedFile uploaded = ctx.uploadedFile("watermark");
        String notice = "Настройки сохранены";

        try {
            if (uploaded != null && uploaded.content() != null) {
                Files.createDirectories(imageProcessor.watermarkFile().getParent());
                try (var in = uploaded.content()) {
                    Files.copy(in, imageProcessor.watermarkFile(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                notice = "Watermark обновлён";
            }
        } catch (IOException e) {
            ctx.redirect("/admin/watermark?error=" + urlEncode("Не удалось сохранить файл: " + e.getMessage()));
            return;
        }

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            SettingsMapper settings = session.getMapper(SettingsMapper.class);
            settings.put("watermark_width_frac", String.valueOf(widthPct / 100.0));
            settings.put("watermark_margin_frac", String.valueOf(marginPct / 100.0));
        }
        ctx.redirect("/admin/watermark?notice=" + urlEncode(notice));
    }

    // ─────────────── Другое: слоганы + «О нас» ───────────────

    public void otherPage(Context ctx) {
        String error = ctx.queryParam("error");
        String notice = ctx.queryParam("notice");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SloganMapper slogans = session.getMapper(SloganMapper.class);
            SettingsMapper settings = session.getMapper(SettingsMapper.class);
            render(ctx, "admin/other.jte", new SlogansView(
                    slogans.findAll(),
                    nvl(settings.get("about_title"), ""),
                    nvl(settings.get("about_html"), ""),
                    nvl(settings.get("about_image"), ""),
                    error, notice));
        }
    }

    public void addSlogan(Context ctx) {
        String text = formParam(ctx, "text");
        if (text.isBlank()) {
            ctx.redirect("/admin/other?error=" + urlEncode("Текст слогана пуст"));
            return;
        }
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.getMapper(SloganMapper.class).insert(text);
        }
        ctx.redirect("/admin/other?notice=" + urlEncode("Слоган добавлен"));
    }

    public void deleteSlogan(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.getMapper(SloganMapper.class).deleteById(id);
        }
        ctx.redirect("/admin/other?notice=" + urlEncode("Слоган удалён"));
    }

    public void toggleSlogan(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            SloganMapper mapper = session.getMapper(SloganMapper.class);
            var slogan = mapper.findById(id);
            if (slogan != null) {
                mapper.setEnabled(id, slogan.isEnabled() ? 0 : 1);
            }
        }
        ctx.redirect("/admin/other?notice=" + urlEncode("Слоган обновлён"));
    }

    public void saveAbout(Context ctx) {
        String title = formParam(ctx, "about_title");
        String html = ctx.formParam("about_html");
        if (html != null) {
            html = html.trim();
        } else {
            html = "";
        }
        UploadedFile uploaded = ctx.uploadedFile("about_image");
        String notice = "Раздел «О нас» сохранён";

        // Сначала фото (если есть): пустой/битый файл — сразу ошибка,
        // текст не «уезжает» в БД молча без картинки.
        String imagePath = null;
        if (hasImageUpload(uploaded)) {
            try {
                imagePath = saveContentImage("about", uploaded, null);
                notice = "Раздел «О нас» и фото сохранены";
            } catch (IOException | InterruptedException e) {
                ctx.redirect("/admin/other?error=" + urlEncode("Не удалось сохранить фото: " + humanImageError(e)));
                return;
            }
        }

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            SettingsMapper settings = session.getMapper(SettingsMapper.class);
            settings.put("about_title", title);
            settings.put("about_html", html);
            if (imagePath != null) {
                settings.put("about_image", imagePath);
            }
        }
        ctx.redirect("/admin/other?notice=" + urlEncode(notice));
    }

    // ─────────────── Контакты (главная #contacts) ───────────────

    public void contactsPage(Context ctx) {
        String error = ctx.queryParam("error");
        String notice = ctx.queryParam("notice");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SettingsMapper settings = session.getMapper(SettingsMapper.class);
            render(ctx, "admin/contacts.jte", new ContactsView(
                    nvl(settings.get("contacts_title"), ""),
                    nvl(settings.get("contacts_lead"), ""),
                    nvl(settings.get("contacts_image"), ""),
                    nvl(settings.get("contacts_phone"), ""),
                    nvl(settings.get("contacts_email"), ""),
                    nvl(settings.get("contacts_address"), ""),
                    nvl(settings.get("contacts_messengers"), ""),
                    error, notice));
        }
    }

    public void saveContacts(Context ctx) {
        String title = formParam(ctx, "contacts_title");
        String lead = formParam(ctx, "contacts_lead");
        String phone = formParam(ctx, "contacts_phone");
        String email = formParam(ctx, "contacts_email");
        String address = formParam(ctx, "contacts_address");
        String messengers = formParam(ctx, "contacts_messengers");
        UploadedFile uploaded = ctx.uploadedFile("contacts_image");
        String notice = "Контакты сохранены";

        // Фото — до записи настроек: пустой multipart не должен ронять convert.
        String imagePath = null;
        if (hasImageUpload(uploaded)) {
            try {
                imagePath = saveContentImage("contacts", uploaded, null);
                notice = "Контакты и фото сохранены";
            } catch (IOException | InterruptedException e) {
                ctx.redirect("/admin/contacts?error=" + urlEncode("Не удалось сохранить фото: " + humanImageError(e)));
                return;
            }
        }

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            SettingsMapper settings = session.getMapper(SettingsMapper.class);
            settings.put("contacts_title", title);
            settings.put("contacts_lead", lead);
            settings.put("contacts_phone", phone);
            settings.put("contacts_email", email);
            settings.put("contacts_address", address);
            settings.put("contacts_messengers", messengers);
            if (imagePath != null) {
                settings.put("contacts_image", imagePath);
            }
        }
        ctx.redirect("/admin/contacts?notice=" + urlEncode(notice));
    }

    /** true, если пользователь реально выбрал непустой файл. */
    private static boolean hasImageUpload(UploadedFile uploaded) {
        return uploaded != null && uploaded.size() > 0;
    }

    /** Короткое сообщение об ошибке картинки без простыни stderr ImageMagick. */
    private static String humanImageError(Exception e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (msg.contains("0 байт") || msg.contains("пустой") || msg.contains("insufficient image data")) {
            return "файл пустой или не докачался — выберите фото заново";
        }
        if (msg.contains("not appear") || msg.contains("не похож")) {
            return msg;
        }
        // convert говорит про формат, а не про «битый JPEG» — упрощаем
        if (msg.contains("ImageMagick") && msg.contains("insufficient")) {
            return "файл не распознан как изображение (JPEG/PNG/HEIC) — попробуйте другой снимок";
        }
        return msg.length() > 300 ? msg.substring(0, 300) + "…" : msg;
    }

    /**
     * Сохранение изображения в CONTENT_DIR/img/{name}[__full].jpg.
     * Оригинал кладётся в originals/ с расширением по magic bytes
     * (не по имени файла — HEIC с «.jpg» и т.п.).
     */
    private String saveContentImage(String name, UploadedFile uploaded, CropRect crop) throws IOException, InterruptedException {
        if (uploaded == null) {
            throw new IOException("Файл не выбран");
        }
        if (uploaded.size() <= 0) {
            throw new IOException("Файл пустой (0 байт) — выберите фото ещё раз");
        }
        Files.createDirectories(originalsDir);
        String token = name + "_" + UUID.randomUUID();
        Path staging = originalsDir.resolve(token + ".upload");
        long copied;
        try (var in = uploaded.content()) {
            copied = Files.copy(in, staging);
        }
        if (copied <= 0 || Files.size(staging) <= 0) {
            Files.deleteIfExists(staging);
            throw new IOException("Не удалось прочитать загруженный файл (0 байт)");
        }
        String ext = detectImageExtension(staging, uploaded.filename());
        Path original = originalsDir.resolve(token + ext);
        Files.move(staging, original, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        if (!looksLikeImage(original)) {
            Files.deleteIfExists(original);
            throw new IOException("Загруженный файл не похож на изображение "
                    + "(ожидается JPEG, PNG, HEIC/HEIF, WebP или GIF)");
        }

        Path fullOut = contentDir.resolve("img").resolve(name + "__full.jpg");
        Path thumbOut = contentDir.resolve("img").resolve(name + ".jpg");
        try {
            if (crop != null) {
                imageProcessor.render(original, crop, ImageProcessor.FULL, fullOut);
                imageProcessor.render(original, crop, ImageProcessor.THUMB, thumbOut);
            } else {
                imageProcessor.renderPlain(original, null, ImageProcessor.FULL, fullOut);
                imageProcessor.renderPlain(original, null, ImageProcessor.THUMB, thumbOut);
            }
        } catch (IOException | InterruptedException e) {
            Files.deleteIfExists(original);
            throw e;
        }
        return "/content/img/" + name + "__full.jpg";
    }

    /** Расширение по magic bytes; иначе — по имени файла. */
    private static String detectImageExtension(Path file, String filename) {
        byte[] head = readFileHead(file);
        if (head != null) {
            int n = head.length;
            if (n >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
                return ".jpg";
            }
            if (n >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
                return ".png";
            }
            if (n >= 6 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F') {
                return ".gif";
            }
            if (n >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
                return ".webp";
            }
            if (n >= 12 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') {
                String brand = new String(head, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
                if (brand.startsWith("avif") || brand.startsWith("avis")) {
                    return ".avif";
                }
                return ".heic";
            }
            if (n >= 2 && head[0] == 'B' && head[1] == 'M') {
                return ".bmp";
            }
        }
        return extensionOf(filename);
    }

    private static boolean looksLikeImage(Path file) {
        byte[] head = readFileHead(file);
        if (head == null || head.length < 4) {
            return false;
        }
        int n = head.length;
        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8) {
            return true; // JPEG
        }
        if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return true; // PNG
        }
        if (head[0] == 'G' && head[1] == 'I' && head[2] == 'F') {
            return true;
        }
        if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F') {
            return true; // WebP/RIFF
        }
        return n >= 12 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p';
    }

    /** Первые байты файла для определения формата; null при ошибке чтения. */
    private static byte[] readFileHead(Path file) {
        try (var in = Files.newInputStream(file)) {
            byte[] buf = new byte[16];
            int read = in.readNBytes(buf, 0, buf.length);
            if (read <= 0) {
                return null;
            }
            byte[] head = new byte[read];
            System.arraycopy(buf, 0, head, 0, read);
            return head;
        } catch (IOException e) {
            return null;
        }
    }

    private static String nvl(String value, String fallback) {
        return value == null ? fallback : value;
    }

    /** Watermark для превью в кропе (та же PNG, что ImageMagick кладёт в thumb/full). */
    public void watermarkPng(Context ctx) {
        try {
            byte[] bytes = Files.readAllBytes(imageProcessor.watermarkFile());
            ctx.contentType("image/png");
            ctx.header("Cache-Control", "max-age=3600");
            ctx.result(bytes);
        } catch (IOException e) {
            ctx.status(404).result("watermark.png not found");
        }
    }

    public void uploadPhoto(Context ctx) {
        long lotId = Long.parseLong(ctx.pathParam("id"));
        UploadedFile uploaded = ctx.uploadedFile("photo");

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            LotMapper mapper = session.getMapper(LotMapper.class);
            Lot lot = mapper.findById(lotId);
            if (lot == null) {
                ctx.status(404).result("Лот не найден");
                return;
            }

            if (uploaded == null) {
                double[] wm = loadWmDefaults(session);
                render(ctx, "admin/photos.jte",
                        new AdminPhotosView(lot, mapper.findImagesByLotId(lotId), "Файл не выбран", wm[0], wm[1]));
                return;
            }

            CropRect crop;
            try {
                crop = new CropRect(
                        Integer.parseInt(ctx.formParam("x")),
                        Integer.parseInt(ctx.formParam("y")),
                        Integer.parseInt(ctx.formParam("width")),
                        Integer.parseInt(ctx.formParam("height"))
                );
            } catch (NumberFormatException e) {
                double[] wm = loadWmDefaults(session);
                render(ctx, "admin/photos.jte",
                        new AdminPhotosView(lot, mapper.findImagesByLotId(lotId), "Не удалось прочитать область кропа", wm[0], wm[1]));
                return;
            }

            WatermarkPlacement placement = readWmPlacement(ctx, loadWmDefaults(session));

            try {
                Path original = saveOriginal(lotId, uploaded);
                int nextSort = mapper.findImagesByLotId(lotId).stream()
                        .mapToInt(LotImage::getSort).max().orElse(0) + 1;

                String base = lot.getSlug() + "__" + nextSort;
                Path fullOut = contentDir.resolve("img").resolve(base + "__full.jpg");
                Path thumbOut = contentDir.resolve("img").resolve(base + ".jpg");

                imageProcessor.render(original, crop, ImageProcessor.FULL, fullOut, placement);
                imageProcessor.render(original, crop, ImageProcessor.THUMB, thumbOut, placement);

                LotImage image = new LotImage();
                image.setLotId(lotId);
                image.setPathThumb("/content/img/" + base + ".jpg");
                image.setPathFull("/content/img/" + base + "__full.jpg");
                image.setAlt(lot.getTitle());
                image.setSort(nextSort);
                if (!placement.isDefaultSe()) {
                    image.setWmX(placement.xFrac());
                    image.setWmY(placement.yFrac());
                    image.setWmWidth(placement.widthFrac());
                } else {
                    image.setWmWidth(placement.widthFrac());
                }
                image.setWmOpacity(placement.opacity());
                mapper.insertImage(image);

                ctx.redirect("/admin/lots/" + lotId + "/photos");
            } catch (IOException | InterruptedException e) {
                log.error("Не удалось обработать загруженное фото для лота {}", lotId, e);
                double[] wm = loadWmDefaults(session);
                render(ctx, "admin/photos.jte",
                        new AdminPhotosView(lot, mapper.findImagesByLotId(lotId),
                                "Не получилось обработать фото: " + e.getMessage(), wm[0], wm[1]));
            }
        }
    }

    private static double[] loadWmDefaults(SqlSession session) {
        SettingsMapper settings = session.getMapper(SettingsMapper.class);
        double width = parseDouble(settings.get("watermark_width_frac"), 0.18);
        double margin = parseDouble(settings.get("watermark_margin_frac"), 0.02);
        return new double[]{width, margin};
    }

    private static WatermarkPlacement readWmPlacement(Context ctx, double[] defaults) {
        String x = formParam(ctx, "wm_x");
        String y = formParam(ctx, "wm_y");
        String w = formParam(ctx, "wm_width");
        double opacity = parseDouble(formParam(ctx, "wm_opacity"), 1.0);
        if (x.isBlank() || y.isBlank() || w.isBlank()) {
            return WatermarkPlacement.defaults(defaults[0], defaults[1], opacity);
        }
        try {
            return WatermarkPlacement.at(
                    Double.parseDouble(x),
                    Double.parseDouble(y),
                    Double.parseDouble(w),
                    opacity);
        } catch (NumberFormatException e) {
            return WatermarkPlacement.defaults(defaults[0], defaults[1], opacity);
        }
    }

    private static double parsePercent(String raw, double fallbackPercent) {
        try {
            double v = Double.parseDouble(raw.trim());
            if (v < 1 || v > 90) {
                return fallbackPercent;
            }
            return v;
        } catch (Exception e) {
            return fallbackPercent;
        }
    }

    private static double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void deletePhoto(Context ctx) {
        long lotId = Long.parseLong(ctx.pathParam("id"));
        long imageId = Long.parseLong(ctx.pathParam("imageId"));

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            LotMapper mapper = session.getMapper(LotMapper.class);
            LotImage image = mapper.findImageById(imageId);
            if (image != null && image.getLotId() == lotId) {
                deleteContentFile(image.getPathThumb());
                deleteContentFile(image.getPathFull());
                mapper.deleteImageById(imageId);
            }
        }
        ctx.redirect("/admin/lots/" + lotId + "/photos");
    }

    /** Пересортировка фото: formParam order = "id1,id2,id3" в новом порядке. */
    public void reorderPhotos(Context ctx) {
        long lotId = Long.parseLong(ctx.pathParam("id"));
        String order = formParam(ctx, "order");
        if (order.isBlank()) {
            ctx.status(400).result("Пустой порядок");
            return;
        }
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            LotMapper mapper = session.getMapper(LotMapper.class);
            Lot lot = mapper.findById(lotId);
            if (lot == null) {
                ctx.status(404).result("Лот не найден");
                return;
            }
            var owned = new java.util.HashSet<Long>();
            for (LotImage img : mapper.findImagesByLotId(lotId)) {
                owned.add(img.getId());
            }
            int sort = 1;
            for (String part : order.split(",")) {
                String raw = part.trim();
                if (raw.isEmpty()) {
                    continue;
                }
                long imageId;
                try {
                    imageId = Long.parseLong(raw);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (!owned.contains(imageId)) {
                    continue;
                }
                mapper.updateImageSort(imageId, sort++);
            }
        }
        // fetch без редиректа — тихий success
        ctx.status(204);
    }

    // ─────────────── helpers ───────────────

    private String validateLotForm(SqlSession session, Context ctx, Long lotId) {
        String title = formParam(ctx, "title");
        String slug = formParam(ctx, "slug");
        if (slug.isBlank()) {
            slug = Fts.slugify(title);
        } else {
            slug = Fts.slugify(slug);
        }
        if (title.isBlank()) {
            return "Название лота обязательно";
        }
        if (!Fts.isValidSlug(slug)) {
            return "Не удалось построить slug — заполните латиницей или оставьте поле пустым, чтобы он собрался из названия";
        }
        String categoryId = formParam(ctx, "category_id");
        if (categoryId.isBlank()) {
            return "Выберите раздел";
        }
        try {
            Long.parseLong(categoryId);
        } catch (NumberFormatException e) {
            return "Раздел выбран некорректно";
        }

        LotMapper mapper = session.getMapper(LotMapper.class);
        if (mapper.existsSlug(slug) && !slugEqualsLot(mapper, slug, lotId)) {
            return "Лот со slug «" + slug + "» уже существует";
        }
        return null;
    }

    private boolean slugEqualsLot(LotMapper mapper, String slug, Long lotId) {
        if (lotId == null) {
            return false;
        }
        Lot bySlug = mapper.findBySlug(slug);
        return bySlug != null && bySlug.getId() == lotId;
    }

    private Lot lotFromForm(Context ctx, Long lotId, DictSelection dictSel) {
        Lot lot = new Lot();
        if (lotId != null) {
            lot.setId(lotId);
        }
        lot.setTitle(formParam(ctx, "title"));
        String slug = formParam(ctx, "slug");
        lot.setSlug(slug.isBlank() ? Fts.slugify(lot.getTitle()) : Fts.slugify(slug));
        lot.setCategoryId(Long.parseLong(formParam(ctx, "category_id")));
        lot.setAnnotation(nullIfBlank(formParam(ctx, "annotation")));
        lot.setSeoText(nullIfBlank(formParam(ctx, "seo_text")));
        lot.setMetaTitle(nullIfBlank(formParam(ctx, "meta_title")));
        lot.setMetaDescription(nullIfBlank(formParam(ctx, "meta_description")));
        // legacy-колонки — для FTS/старых запросов; источник истины — lot_dict
        lot.setOccasion(dictSel.occasions().isEmpty() ? null : String.join(", ", dictSel.occasions()));
        lot.setYear(nullIfBlank(formParam(ctx, "year")));
        lot.setFabricNotes(dictSel.fabrics().isEmpty() ? null : String.join(", ", dictSel.fabrics()));
        lot.setClientName(nullIfBlank(formParam(ctx, "client_name")));
        lot.setFeatured(ctx.formParam("featured") != null);
        String status = formParam(ctx, "status");
        lot.setStatus(status.isBlank() ? "draft" : status);
        String sort = formParam(ctx, "sort");
        lot.setSort(sort.isBlank() ? 0 : Integer.parseInt(sort));
        return lot;
    }

    private record DictSelection(List<String> occasions, List<String> fabrics, List<String> tags) {
        static DictSelection empty() {
            return new DictSelection(List.of(), List.of(), List.of());
        }
    }

    /** hidden-поля occasions/fabrics/tags — значения через «|». */
    private DictSelection readDictForm(Context ctx) {
        return new DictSelection(
                splitPipe(formParam(ctx, "occasions")),
                splitPipe(formParam(ctx, "fabrics")),
                splitPipe(formParam(ctx, "tags"))
        );
    }

    private static List<String> splitPipe(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("\\|")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                out.add(value);
            }
        }
        return out;
    }

    private void saveDicts(SqlSession session, long lotId, DictSelection sel) {
        DictMapper dict = session.getMapper(DictMapper.class);
        LotMapper lotMapper = session.getMapper(LotMapper.class);
        for (String kind : List.of("occasion", "fabric", "tag")) {
            List<String> names = switch (kind) {
                case "occasion" -> sel.occasions();
                case "fabric" -> sel.fabrics();
                default -> sel.tags();
            };
            dict.replaceLotDict(lotId, kind);
            for (String name : names) {
                dict.upsertValue(kind, name);
                Long id = dict.findId(kind, name);
                if (id != null) {
                    dict.linkLotDict(lotId, id);
                }
            }
        }
        // tags/lot_tags — фильтр галереи и FTS
        replaceTags(session, lotMapper, lotId, String.join(", ", sel.tags()));
    }

    private LotFormView emptyLotForm(SqlSession session, Context ctx, String error) {
        DictMapper dict = session.getMapper(DictMapper.class);
        List<String> occasions = List.of();
        List<String> fabrics = List.of();
        List<String> tags = List.of();
        if (ctx != null) {
            DictSelection sel = readDictForm(ctx);
            occasions = sel.occasions();
            fabrics = sel.fabrics();
            tags = sel.tags();
        }
        return new LotFormView(
                null,
                session.getMapper(CategoryMapper.class).findAll(),
                occasions, fabrics, tags,
                dict.findNamesByKind("occasion"),
                dict.findNamesByKind("fabric"),
                dict.findNamesByKind("tag"),
                error, null);
    }

    private LotFormView draftLotForm(SqlSession session, Context ctx, Lot draft, DictSelection sel, String error) {
        DictMapper dict = session.getMapper(DictMapper.class);
        return LotFormView.edit(
                draft,
                session.getMapper(CategoryMapper.class).findAll(),
                sel.occasions(), sel.fabrics(), sel.tags(),
                dict.findNamesByKind("occasion"),
                dict.findNamesByKind("fabric"),
                dict.findNamesByKind("tag"),
                error, null);
    }

    private void replaceTags(SqlSession session, LotMapper mapper, long lotId, String tagsRaw) {
        mapper.deleteTagsByLotId(lotId);
        if (tagsRaw == null || tagsRaw.isBlank()) {
            return;
        }
        for (String part : tagsRaw.split("[,;]+")) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            mapper.upsertTag(name);
            Long tagId = mapper.findTagIdByName(name);
            if (tagId != null) {
                mapper.linkTag(lotId, tagId);
            }
        }
    }

    /** Обложка раздела: файл + опциональный кроп (без watermark). */
    private String processCategoryCover(Context ctx, String slug) {
        UploadedFile uploaded = ctx.uploadedFile("cover");
        if (uploaded == null || uploaded.size() <= 0) {
            return null;
        }
        CropRect crop = null;
        String x = formParam(ctx, "cover_x");
        String y = formParam(ctx, "cover_y");
        String w = formParam(ctx, "cover_w");
        String h = formParam(ctx, "cover_h");
        if (!x.isBlank() && !y.isBlank() && !w.isBlank() && !h.isBlank()) {
            try {
                crop = new CropRect(
                        Integer.parseInt(x), Integer.parseInt(y),
                        Integer.parseInt(w), Integer.parseInt(h));
            } catch (NumberFormatException ignored) {
                crop = null;
            }
        }
        try {
            Files.createDirectories(originalsDir);
            String token = "cat_" + slug + "_" + UUID.randomUUID();
            Path staging = originalsDir.resolve(token + ".upload");
            try (var in = uploaded.content()) {
                Files.copy(in, staging);
            }
            if (Files.size(staging) <= 0) {
                Files.deleteIfExists(staging);
                throw new IOException("Файл пустой (0 байт)");
            }
            String ext = detectImageExtension(staging, uploaded.filename());
            Path original = originalsDir.resolve(token + ext);
            Files.move(staging, original, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            if (!looksLikeImage(original)) {
                Files.deleteIfExists(original);
                throw new IOException("Файл не похож на изображение");
            }
            Path fullOut = contentDir.resolve("img").resolve("cat-" + slug + "__full.jpg");
            Path thumbOut = contentDir.resolve("img").resolve("cat-" + slug + ".jpg");
            imageProcessor.renderPlain(original, crop, ImageProcessor.FULL, fullOut);
            imageProcessor.renderPlain(original, crop, ImageProcessor.THUMB, thumbOut);
            return "/content/img/cat-" + slug + "__full.jpg";
        } catch (IOException | InterruptedException e) {
            log.error("Не удалось обработать обложку раздела {}", slug, e);
            return null;
        }
    }

    private Path saveOriginal(long lotId, UploadedFile uploaded) throws IOException {
        if (uploaded == null || uploaded.size() <= 0) {
            throw new IOException("Файл пустой (0 байт) — выберите фото ещё раз");
        }
        Files.createDirectories(originalsDir);
        String token = lotId + "_" + UUID.randomUUID();
        Path staging = originalsDir.resolve(token + ".upload");
        long copied;
        try (var in = uploaded.content()) {
            copied = Files.copy(in, staging);
        }
        if (copied <= 0 || Files.size(staging) <= 0) {
            Files.deleteIfExists(staging);
            throw new IOException("Не удалось прочитать загруженный файл (0 байт)");
        }
        String ext = detectImageExtension(staging, uploaded.filename());
        Path target = originalsDir.resolve(token + ext);
        Files.move(staging, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        if (!looksLikeImage(target)) {
            Files.deleteIfExists(target);
            throw new IOException("Загруженный файл не похож на изображение");
        }
        return target;
    }

    /**
     * Белый список, а не просто "всё после последней точки": имя файла
     * приходит от клиента, и расширение в итоге попадает в Path.resolve()
     * на диске. Сейчас "../" там физически не может получиться (lastIndexOf
     * на точке упирается в точки самого ".." раньше, чем до них доходит
     * слэш) — но это везение конкретной реализации, а не осознанная
     * защита, и полагаться на такую случайность нельзя.
     */
    private static String extensionOf(String filename) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0) {
                String candidate = filename.substring(dot).toLowerCase(java.util.Locale.ROOT);
                if (candidate.matches("\\.[a-z0-9]{1,5}")) {
                    return candidate;
                }
            }
        }
        return ".jpg";
    }

    private void deleteContentFile(String publicPath) {
        if (publicPath == null || !publicPath.startsWith("/content/")) {
            return;
        }
        Path file = contentDir.resolve(publicPath.substring("/content/".length()));
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Не удалось удалить файл {}", file, e);
        }
    }

    private static String formParam(Context ctx, String name) {
        String value = ctx.formParam(name);
        return value == null ? "" : value.trim();
    }

    private static String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void render(Context ctx, String template, Object model) {
        StringOutput output = new StringOutput();
        templateEngine.render(template, model, output);
        ctx.html(output.toString());
    }
}
