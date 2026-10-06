package ru.milastoria;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.staticfiles.Location;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.milastoria.image.ImageProcessor;
import ru.milastoria.analytics.AnalyticsTracker;
import ru.milastoria.mapper.AnalyticsMapper;
import ru.milastoria.mapper.CategoryMapper;
import ru.milastoria.mapper.DictMapper;
import ru.milastoria.mapper.LotMapper;
import ru.milastoria.mapper.MediaMapper;
import ru.milastoria.mapper.SettingsMapper;
import ru.milastoria.mapper.SloganMapper;
import ru.milastoria.search.LotSearchIndex;
import ru.milastoria.web.AdminController;
import ru.milastoria.web.Auth;
import ru.milastoria.web.MediaAdminController;
import ru.milastoria.web.SiteController;
import ru.milastoria.web.SitemapController;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Точка входа: собирает зависимости (БД, MyBatis, jte) и регистрирует
 * маршруты. Сами страницы — в {@link SiteController}; здесь только
 * проводка.
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        AppConfig cfg = AppConfig.load();

        String dbPath = cfg.get("storage.db_path", "DB_PATH", "data/milastoria.db");
        int port = Integer.parseInt(cfg.get("server.port", "PORT", "8080"));
        // Графические ресурсы сайта (img/video/originals) — CONTENT_DIR / storage.content_dir
        Path contentDirPath = Path.of(cfg.get("storage.content_dir", "CONTENT_DIR", "../prototype/assets"));
        Files.createDirectories(contentDirPath);
        // Jetty резолвит внешние статические директории надёжно только от
        // абсолютного пути — относительный "../prototype-v2/assets" тихо
        // не находил файлы, несмотря на верный cwd процесса.
        String contentDir = contentDirPath.toAbsolutePath().normalize().toString();

        DataSource dataSource = createDataSource(dbPath);
        initSchema(dataSource);

        SqlSessionFactory sqlSessionFactory = createSqlSessionFactory(dataSource);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            // Словари (повод/материалы/теги) — перенос из tags и free-text lots, если пусто
            DictMapper dictMapper = session.getMapper(DictMapper.class);
            if (dictMapper.isDictEmpty()) {
                dictMapper.seedTagsFromLegacy();
                dictMapper.seedOccasionsFromLegacy();
                dictMapper.seedFabricsFromLegacy();
                dictMapper.seedLinksFromTags();
                dictMapper.seedLinksFromOccasions();
                dictMapper.seedLinksFromFabrics();
                log.info("Словари dict_values перенесены из tags/lots");
            }
            SloganMapper sloganMapper = session.getMapper(SloganMapper.class);
            if (sloganMapper.findAll().isEmpty()) {
                sloganMapper.insert("Платье — начало вашей <em>истории</em>");
                sloganMapper.insert("Нарядное платье — история вашей семьи");
                log.info("Добавлены стартовые слоганы H1");
            }
            // FTS-индекс пересобираем при каждом старте: на объёме ~сотни
            // лотов это мгновенно и снимает расхождения после крашей/ручного SQL.
            LotSearchIndex.rebuildAll(session);
        }

        TemplateEngine templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);
        String baseUrl = cfg.get("site.base_url", "SITE_BASE_URL", "https://milastoria.com");
        SiteController site = new SiteController(sqlSessionFactory, templateEngine, baseUrl);
        SitemapController sitemap = new SitemapController(sqlSessionFactory, baseUrl);

        Path dataDir = Path.of(dbPath).toAbsolutePath().normalize().getParent();
        Path watermark = ImageProcessor.extractBundledWatermark(dataDir.resolve("branding"));
        ImageProcessor imageProcessor = new ImageProcessor(
                cfg.get("image.convert", "CONVERT_BIN", "convert"), watermark);

        Auth auth = createAuth(cfg);
        String sessionSecret = cfg.get("admin.session_secret", "SESSION_SECRET",
                "dev-only-insecure-secret-change-me");
        AnalyticsTracker analytics = new AnalyticsTracker(sqlSessionFactory, sessionSecret);
        AdminController admin = new AdminController(
                sqlSessionFactory, templateEngine, imageProcessor, auth,
                contentDirPath.toAbsolutePath().normalize(),
                contentDirPath.toAbsolutePath().normalize().resolve("originals")
        );
        MediaAdminController mediaAdmin = new MediaAdminController(
                sqlSessionFactory, templateEngine, imageProcessor,
                contentDirPath.toAbsolutePath().normalize()
        );

        Javalin app = Javalin.create(config -> {
            // Фото с iPad/камеры — десятки МБ; без явного лимита Jetty
            // может отдать multipart-файл «пустым» (0 байт) в convert.
            config.http.maxRequestSize = 80L * 1024 * 1024;
            config.jetty.multipartConfig.maxFileSize(50, io.javalin.config.SizeUnit.MB);
            config.jetty.multipartConfig.maxTotalRequestSize(80, io.javalin.config.SizeUnit.MB);
            config.jetty.multipartConfig.maxInMemoryFileSize(4, io.javalin.config.SizeUnit.MB);
            config.staticFiles.add("public"); // брендовые ассеты (логотип, шрифты, css) — из classpath
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/content";
                staticFiles.directory = contentDir;
                staticFiles.location = Location.EXTERNAL;
            });
        });

        app.get("/", site::home);
        app.get("/gallery", site::gallery);
        app.get("/work/{slug}", site::work);
        // Старые URL прежнего сайта — permanent redirect (иначе Google держит 404)
        app.get("/catalog", ctx -> ctx.redirect("/gallery", io.javalin.http.HttpStatus.MOVED_PERMANENTLY));
        app.get("/catalog/", ctx -> ctx.redirect("/gallery", io.javalin.http.HttpStatus.MOVED_PERMANENTLY));
        app.get("/profile", ctx -> ctx.redirect("/#about", io.javalin.http.HttpStatus.MOVED_PERMANENTLY));
        app.get("/about", ctx -> ctx.redirect("/#about", io.javalin.http.HttpStatus.MOVED_PERMANENTLY));
        app.get("/contacts", ctx -> ctx.redirect("/#contacts", io.javalin.http.HttpStatus.MOVED_PERMANENTLY));
        // robots.txt — статика из classpath (public/robots.txt)
        app.get("/sitemap.xml", sitemap::sitemap);

        // Публичные страницы сайта — анонимная аналитика (visitor/session cookie).
        // Пропускает /admin, статику, ботов; ошибки трекера не валят ответ.
        app.before(analytics::track);

        // Всё под /admin, кроме самой формы входа, требует валидной сессии.
        // "/admin/*" НЕ матчит голый "/admin" без хвоста — регистрируем
        // обработчик на оба варианта явно, а не полагаемся на один паттерн.
        Handler requireAuth = ctx -> {
            boolean isLoginRoute = ctx.path().equals("/admin/login");
            if (!isLoginRoute && !admin.isAuthenticated(ctx)) {
                ctx.redirect("/admin/login");
            }
        };
        app.before("/admin", requireAuth);
        app.before("/admin/*", requireAuth);

        app.get("/admin/login", admin::loginForm);
        app.post("/admin/login", admin::login);
        app.post("/admin/logout", admin::logout);
        app.get("/admin", admin::dashboard);

        app.get("/admin/lots/new", admin::lotFormNew);
        app.post("/admin/lots", admin::createLot);
        app.post("/admin/lots/reorder", admin::reorderLots);
        app.get("/admin/lots/{id}/edit", admin::lotFormEdit);
        app.post("/admin/lots/{id}", admin::updateLot);

        app.get("/admin/categories", admin::categoriesPage);
        app.post("/admin/categories/reorder", admin::reorderCategories);
        app.get("/admin/categories/new", admin::categoryFormNew);
        app.post("/admin/categories", admin::createCategory);
        app.get("/admin/categories/{id}/edit", admin::categoryFormEdit);
        app.post("/admin/categories/{id}", admin::updateCategory);

        app.get("/admin/lots/{id}/photos", admin::photosPage);
        app.get("/admin/media", mediaAdmin::listPage);
        app.get("/admin/media/lots", mediaAdmin::lotPickerPage);
        app.get("/admin/media/attach-queue", mediaAdmin::attachQueueStart);
        app.get("/admin/media/lots/{lotId}/pick", mediaAdmin::pickLot);
        app.post("/admin/media/lots/{lotId}/attach-selected", mediaAdmin::attachBatch);
        app.post("/admin/media/attach-batch", mediaAdmin::attachBatch);
        app.post("/admin/media/delete-batch", mediaAdmin::deleteBatch);
        app.post("/admin/media/compress-settings", mediaAdmin::saveCompressSettings);
        app.post("/admin/media/upload", mediaAdmin::upload);
        app.get("/admin/media/{id}/download", mediaAdmin::download);
        app.post("/admin/media/{id}/delete", mediaAdmin::delete);
        app.get("/admin/media/{id}/attach", mediaAdmin::attachForm);
        app.post("/admin/media/{id}/attach", mediaAdmin::attachSubmit);
        app.get("/admin/watermark", admin::watermarkPage);
        app.post("/admin/watermark", admin::saveWatermarkSettings);
        app.get("/admin/contacts", admin::contactsPage);
        app.post("/admin/contacts", admin::saveContacts);
        app.get("/admin/other", admin::otherPage);
        app.post("/admin/other/slogans", admin::addSlogan);
        app.post("/admin/other/slogans/{id}/delete", admin::deleteSlogan);
        app.post("/admin/other/slogans/{id}/toggle", admin::toggleSlogan);
        app.post("/admin/other/hero", admin::saveHero);
        app.post("/admin/other/hero/slides", admin::addHeroSlideImage);
        app.post("/admin/other/hero/slides/video", admin::addHeroSlideVideo);
        app.post("/admin/other/hero/slides/reorder", admin::reorderHeroSlides);
        app.post("/admin/other/hero/slides/{id}/delete", admin::deleteHeroSlide);
        app.post("/admin/other/about", admin::saveAbout);
        app.post("/admin/other/gallery", admin::saveGallerySection);
        app.get("/admin/analytics", admin::analyticsPage);
        app.get("/admin/branding/watermark.png", admin::watermarkPng);
        app.post("/admin/lots/{id}/photos", admin::uploadPhoto);
        app.post("/admin/lots/{id}/photos/reorder", admin::reorderPhotos);
        app.post("/admin/lots/{id}/photos/{imageId}/delete", admin::deletePhoto);

        app.start(port);
    }

    private static Auth createAuth(AppConfig cfg) {
        String user = cfg.get("admin.user", "ADMIN_USER", "admin");
        // Пароль — обычный текст в app.yml / ADMIN_PASSWORD; хешируем сами
        // перед Auth (тот работает только с хешем).
        String password = cfg.get("admin.password", "ADMIN_PASSWORD", "admin");
        String passwordHash = sha256Hex(password);
        String sessionSecret = cfg.get("admin.session_secret", "SESSION_SECRET",
                "dev-only-insecure-secret-change-me");

        if (password.equals("admin") || sessionSecret.equals("dev-only-insecure-secret-change-me")) {
            log.warn("Админка запущена с дефолтным dev-паролем и/или dev-секретом сессии. "
                    + "Для продакшна задайте admin.password / admin.session_secret в config/app.yml "
                    + "или ADMIN_PASSWORD / SESSION_SECRET в окружении.");
        }
        return new Auth(user, passwordHash, sessionSecret);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static DataSource createDataSource(String dbPath) throws IOException {
        Path path = Path.of(dbPath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        // SQLite пишет из одного соединения за раз — пул больше 1 только
        // создаст гонки за блокировку файла, а не ускорит запись.
        config.setMaximumPoolSize(1);
        config.setConnectionInitSql("PRAGMA foreign_keys = ON");
        return new HikariDataSource(config);
    }

    private static void initSchema(DataSource dataSource) throws SQLException, IOException {
        try (Connection conn = dataSource.getConnection()) {
            runScript(conn, "/schema.sql");
            // Старые БД могли быть созданы без created_at — колонки
            // достраиваем после CREATE TABLE IF NOT EXISTS (он не мигрирует).
            addColumnIfMissing(conn, "lots", "created_at", "TEXT");
            addColumnIfMissing(conn, "categories", "created_at", "TEXT");
            addColumnIfMissing(conn, "categories", "sort", "INTEGER DEFAULT 0");
            addColumnIfMissing(conn, "categories", "lead", "TEXT");
            addColumnIfMissing(conn, "lot_images", "wm_x", "REAL");
            addColumnIfMissing(conn, "lot_images", "wm_y", "REAL");
            addColumnIfMissing(conn, "lot_images", "wm_width", "REAL");
            addColumnIfMissing(conn, "lot_images", "wm_opacity", "REAL");
            addColumnIfMissing(conn, "media_files", "group_id", "INTEGER");
            addColumnIfMissing(conn, "media_files", "applied", "INTEGER NOT NULL DEFAULT 0");
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE INDEX IF NOT EXISTS idx_media_files_group ON media_files(group_id)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_media_files_applied ON media_files(applied)");
            }
            // категории без sort заполняем по id (стабильный порядок «как было»)
            try (Statement st = conn.createStatement()) {
                st.execute("UPDATE categories SET sort = id WHERE sort = 0 OR sort IS NULL");
            }
            if (isEmpty(conn, "lots")) {
                runScript(conn, "/seed.sql");
            }
            // миграция hero: settings hero_image → hero_slides, если таблица пуста
            migrateLegacyHeroSlides(conn);
        }
    }

    private static void migrateLegacyHeroSlides(Connection conn) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM hero_slides")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }
        String desktop = readSetting(conn, "hero_image");
        String mobile = readSetting(conn, "hero_image_mobile");
        if (desktop == null || desktop.isBlank()) {
            return;
        }
        try (var st = conn.prepareStatement(
                "INSERT INTO hero_slides (sort, kind, desktop_path, mobile_path, alt, created_at) VALUES (?,?,?,?,?,?)")) {
            st.setInt(1, 0);
            st.setString(2, "image");
            st.setString(3, desktop);
            st.setString(4, mobile);
            st.setString(5, "Нарядные платья Milastoria");
            st.setString(6, java.time.Instant.now().toString());
            st.executeUpdate();
        }
    }

    private static String readSetting(Connection conn, String key) throws SQLException {
        try (var st = conn.prepareStatement("SELECT value FROM settings WHERE key = ?")) {
            st.setString(1, key);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String type)
            throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            log.info("Миграция: добавлена колонка {}.{}", table, column);
        }
    }

    private static boolean isEmpty(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1) == 0;
        }
    }

    /**
     * Наивный разделитель по ";" — годится для наших schema.sql/seed.sql
     * без точек с запятой внутри строковых литералов. sqlite-jdbc, как и
     * большинство JDBC-драйверов, не умеет выполнять несколько statement'ов
     * одним execute().
     */
    private static void runScript(Connection conn, String resourcePath) throws IOException, SQLException {
        String sql = readResource(resourcePath);
        // Новый Statement на каждый блок: sqlite-jdbc может финализировать
        // PreparedStatement после execute — повторное использование роняет старт.
        for (String statement : sql.split(";")) {
            String body = stripLeadingComments(statement);
            if (body.isEmpty()) {
                continue;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(body);
            }
        }
    }

    private static String stripLeadingComments(String sql) {
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("--")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }

    private static String readResource(String path) throws IOException {
        try (InputStream in = App.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Ресурс не найден: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static SqlSessionFactory createSqlSessionFactory(DataSource dataSource) {
        Environment environment = new Environment("default", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        // lot_id -> lotId, cover_image -> coverImage и т.д.
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(CategoryMapper.class);
        configuration.addMapper(LotMapper.class);
        configuration.addMapper(MediaMapper.class);
        configuration.addMapper(DictMapper.class);
        configuration.addMapper(SettingsMapper.class);
        configuration.addMapper(SloganMapper.class);
        configuration.addMapper(AnalyticsMapper.class);
        configuration.addMapper(ru.milastoria.mapper.HeroSlideMapper.class);
        configuration.addMapper(ru.milastoria.mapper.MediaGroupMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }
}
