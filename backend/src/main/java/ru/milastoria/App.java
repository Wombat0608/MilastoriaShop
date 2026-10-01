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
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.milastoria.image.ImageProcessor;
import ru.milastoria.mapper.CategoryMapper;
import ru.milastoria.mapper.LotMapper;
import ru.milastoria.web.AdminController;
import ru.milastoria.web.Auth;
import ru.milastoria.web.SiteController;

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
        String dbPath = env("DB_PATH", "data/milastoria.db");
        int port = Integer.parseInt(env("PORT", "9091"));
        // По умолчанию — фото/видео из соседнего prototype/assets (удобно
        // для локальной разработки и демонстрации вёрстки). Именно
        // prototype, а не prototype-v2/assets — тот путь симлинк, а Jetty
        // по умолчанию не отдаёт файлы через симлинки (alias-защита от
        // directory traversal) и тихо возвращает 404 без объяснений.
        // В контейнере CONTENT_DIR должен указывать на реальный volume
        // с загрузками, см. Dockerfile.
        Path contentDirPath = Path.of(env("CONTENT_DIR", "../prototype/assets"));
        Files.createDirectories(contentDirPath);
        // Jetty резолвит внешние статические директории надёжно только от
        // абсолютного пути — относительный "../prototype-v2/assets" тихо
        // не находил файлы, несмотря на верный cwd процесса.
        String contentDir = contentDirPath.toAbsolutePath().normalize().toString();

        DataSource dataSource = createDataSource(dbPath);
        initSchema(dataSource);

        SqlSessionFactory sqlSessionFactory = createSqlSessionFactory(dataSource);
        TemplateEngine templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);
        SiteController site = new SiteController(sqlSessionFactory, templateEngine);

        Path dataDir = Path.of(dbPath).toAbsolutePath().normalize().getParent();
        Path watermark = ImageProcessor.extractBundledWatermark(dataDir.resolve("branding"));
        ImageProcessor imageProcessor = new ImageProcessor(env("CONVERT_BIN", "convert"), watermark);

        Auth auth = createAuth();
        AdminController admin = new AdminController(
                sqlSessionFactory, templateEngine, imageProcessor, auth,
                contentDirPath.toAbsolutePath().normalize(),
                contentDirPath.toAbsolutePath().normalize().resolve("originals")
        );

        Javalin app = Javalin.create(config -> {
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
        app.get("/admin/lots/{id}/photos", admin::photosPage);
        app.post("/admin/lots/{id}/photos", admin::uploadPhoto);
        app.post("/admin/lots/{id}/photos/{imageId}/delete", admin::deletePhoto);

        app.start(port);
    }

    private static Auth createAuth() {
        String user = env("ADMIN_USER", "admin");
        // Пароль задаётся обычным текстом в переменной окружения — считать
        // sha256sum руками ради одного пароля на своём же VPS не нужно;
        // хешируем сами перед тем, как передать в Auth (который дальше
        // работает только с хешем, не с открытым текстом).
        String password = env("ADMIN_PASSWORD", "admin");
        String passwordHash = sha256Hex(password);
        String sessionSecret = env("SESSION_SECRET", "dev-only-insecure-secret-change-me");

        if (password.equals("admin") || sessionSecret.equals("dev-only-insecure-secret-change-me")) {
            log.warn("Админка запущена с дефолтным dev-паролем и/или dev-секретом сессии. "
                    + "Для продакшна обязательно задать ADMIN_PASSWORD и SESSION_SECRET через окружение.");
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
            if (isEmpty(conn, "lots")) {
                runScript(conn, "/seed.sql");
            }
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
        try (Statement stmt = conn.createStatement()) {
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
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
        return new SqlSessionFactoryBuilder().build(configuration);
    }
}
