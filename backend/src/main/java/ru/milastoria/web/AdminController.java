package ru.milastoria.web;

import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.milastoria.domain.Lot;
import ru.milastoria.domain.LotImage;
import ru.milastoria.image.CropRect;
import ru.milastoria.image.ImageProcessor;
import ru.milastoria.mapper.LotMapper;
import ru.milastoria.view.AdminPhotosView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Админка: логин + загрузка/кроп/удаление фото лота. Никакого создания
 * или редактирования текстовых полей лота здесь нет — это отдельная,
 * не менее простая задача, но другая; сейчас лоты редактируются через
 * seed.sql, пока не понадобится настоящая форма.
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

    public void dashboard(Context ctx) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<Lot> lots = session.getMapper(LotMapper.class).findAll();
            render(ctx, "admin/dashboard.jte", lots);
        }
    }

    public void photosPage(Context ctx) {
        long lotId = Long.parseLong(ctx.pathParam("id"));
        try (SqlSession session = sqlSessionFactory.openSession()) {
            LotMapper mapper = session.getMapper(LotMapper.class);
            Lot lot = mapper.findById(lotId);
            if (lot == null) {
                ctx.status(404).result("Лот не найден");
                return;
            }
            render(ctx, "admin/photos.jte", new AdminPhotosView(lot, mapper.findImagesByLotId(lotId), null));
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
                render(ctx, "admin/photos.jte",
                        new AdminPhotosView(lot, mapper.findImagesByLotId(lotId), "Файл не выбран"));
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
                render(ctx, "admin/photos.jte",
                        new AdminPhotosView(lot, mapper.findImagesByLotId(lotId), "Не удалось прочитать область кропа"));
                return;
            }

            try {
                Path original = saveOriginal(lotId, uploaded);
                int nextSort = mapper.findImagesByLotId(lotId).stream()
                        .mapToInt(LotImage::getSort).max().orElse(0) + 1;

                String base = lot.getSlug() + "__" + nextSort;
                Path fullOut = contentDir.resolve("img").resolve(base + "__full.jpg");
                Path thumbOut = contentDir.resolve("img").resolve(base + ".jpg");

                imageProcessor.render(original, crop, ImageProcessor.FULL, fullOut);
                imageProcessor.render(original, crop, ImageProcessor.THUMB, thumbOut);

                LotImage image = new LotImage();
                image.setLotId(lotId);
                image.setPathThumb("/content/img/" + base + ".jpg");
                image.setPathFull("/content/img/" + base + "__full.jpg");
                image.setAlt(lot.getTitle());
                image.setSort(nextSort);
                mapper.insertImage(image);

                ctx.redirect("/admin/lots/" + lotId + "/photos");
            } catch (IOException | InterruptedException e) {
                log.error("Не удалось обработать загруженное фото для лота {}", lotId, e);
                render(ctx, "admin/photos.jte",
                        new AdminPhotosView(lot, mapper.findImagesByLotId(lotId), "Не получилось обработать фото: " + e.getMessage()));
            }
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

    private Path saveOriginal(long lotId, UploadedFile uploaded) throws IOException {
        Files.createDirectories(originalsDir);
        String ext = extensionOf(uploaded.filename());
        Path target = originalsDir.resolve(lotId + "_" + UUID.randomUUID() + ext);
        try (var in = uploaded.content()) {
            Files.copy(in, target);
        }
        return target;
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return ".jpg";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
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

    private void render(Context ctx, String template, Object model) {
        StringOutput output = new StringOutput();
        templateEngine.render(template, model, output);
        ctx.html(output.toString());
    }
}
