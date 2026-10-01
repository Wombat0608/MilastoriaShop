package ru.milastoria.web;

import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import ru.milastoria.domain.Category;
import ru.milastoria.domain.Lot;
import ru.milastoria.mapper.CategoryMapper;
import ru.milastoria.mapper.LotMapper;
import ru.milastoria.view.CategoryOption;
import ru.milastoria.view.CategoryTile;
import ru.milastoria.view.GalleryView;
import ru.milastoria.view.HomeView;
import ru.milastoria.view.LotCard;
import ru.milastoria.view.VideoTile;
import ru.milastoria.view.WorkView;

import java.util.List;

/**
 * Три страницы сайта. Держим их вместе, пока это ~200 строк и общие
 * приватные хелперы (toCard, categoryTitleById) — как только контроллер
 * разрастётся, разносить по классам будет видно, где резать, а не
 * заранее угадывать границы.
 */
public class SiteController {

    private final SqlSessionFactory sqlSessionFactory;
    private final TemplateEngine templateEngine;

    public SiteController(SqlSessionFactory sqlSessionFactory, TemplateEngine templateEngine) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.templateEngine = templateEngine;
    }

    public void home(Context ctx) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            CategoryMapper categoryMapper = session.getMapper(CategoryMapper.class);
            LotMapper lotMapper = session.getMapper(LotMapper.class);

            List<CategoryTile> categories = categoryMapper.findAll().stream()
                    .map(cat -> new CategoryTile(cat.getSlug(), cat.getTitle(), cat.getCoverImage(),
                            lotMapper.countPublishedByCategory(cat.getId())))
                    .toList();

            List<LotCard> featured = lotMapper.findFeatured(9).stream()
                    .map(lot -> toCard(lotMapper, lot))
                    .toList();

            List<VideoTile> videos = lotMapper.findFeatured(20).stream()
                    .flatMap(lot -> lotMapper.findVideosByLotId(lot.getId()).stream()
                            .map(video -> new VideoTile(lot.getTitle(), "Лот", video.getPosterPath(),
                                    "/work/" + lot.getSlug(), video.getCaption())))
                    .limit(3)
                    .toList();

            render(ctx, "home.jte", new HomeView(categories, featured, videos));
        }
    }

    public void gallery(Context ctx) {
        String categorySlug = ctx.queryParam("category");
        String tag = ctx.queryParam("tag");
        String query = normalize(ctx.queryParam("q"));

        try (SqlSession session = sqlSessionFactory.openSession()) {
            CategoryMapper categoryMapper = session.getMapper(CategoryMapper.class);
            LotMapper lotMapper = session.getMapper(LotMapper.class);

            List<CategoryOption> options = categoryMapper.findAll().stream()
                    .map(cat -> new CategoryOption(cat.getSlug(), cat.getTitle(),
                            cat.getSlug().equals(categorySlug)))
                    .toList();

            List<LotCard> lots = lotMapper.findPublished(categorySlug, tag, query).stream()
                    .map(lot -> toCard(lotMapper, lot))
                    .toList();

            String title;
            String lead;
            if (tag != null) {
                title = "Тег: " + tag;
                lead = "Подборка по тегу «" + tag + "». Нарядные детские и женские платья ателье Milastoria.";
            } else if (categorySlug != null) {
                Category cat = categoryMapper.findBySlug(categorySlug);
                title = cat != null ? cat.getTitle() : "Галерея";
                lead = cat != null ? cat.getSeoText() : "";
            } else {
                title = "Галерея работ";
                lead = "Коллекция нарядных детских, женских платьев, Family Look и работ клиентов. "
                        + "Каждый лот — с аннотацией, тегами и, при необходимости, видео.";
            }

            render(ctx, "gallery.jte", new GalleryView(title, lead, options, tag, query, lots));
        }
    }

    public void work(Context ctx) {
        String slug = ctx.pathParam("slug");

        try (SqlSession session = sqlSessionFactory.openSession()) {
            LotMapper lotMapper = session.getMapper(LotMapper.class);
            CategoryMapper categoryMapper = session.getMapper(CategoryMapper.class);

            Lot lot = lotMapper.findBySlug(slug);
            if (lot == null) {
                throw new NotFoundResponse("Лот не найден: " + slug);
            }

            Category category = categoryMapper.findAll().stream()
                    .filter(c -> c.getId() == lot.getCategoryId())
                    .findFirst()
                    .orElse(null);

            List<LotCard> related = lotMapper.findRelated(lot.getCategoryId(), lot.getId(), 4).stream()
                    .map(l -> toCard(lotMapper, l))
                    .toList();

            WorkView view = new WorkView(
                    lot,
                    category != null ? category.getTitle() : "",
                    category != null ? category.getSlug() : "",
                    lotMapper.findImagesByLotId(lot.getId()),
                    lotMapper.findVideosByLotId(lot.getId()),
                    lotMapper.findTagNamesByLotId(lot.getId()),
                    related
            );

            render(ctx, "work.jte", view);
        }
    }

    private LotCard toCard(LotMapper lotMapper, Lot lot) {
        var images = lotMapper.findImagesByLotId(lot.getId());
        String cover = images.isEmpty() ? null : images.get(0).getPathThumb();
        boolean hasVideo = !lotMapper.findVideosByLotId(lot.getId()).isEmpty();
        return new LotCard(lot.getSlug(), lot.getTitle(), lot.getAnnotation(), cover, hasVideo);
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private void render(Context ctx, String template, Object model) {
        StringOutput output = new StringOutput();
        templateEngine.render(template, model, output);
        ctx.html(output.toString());
    }
}
