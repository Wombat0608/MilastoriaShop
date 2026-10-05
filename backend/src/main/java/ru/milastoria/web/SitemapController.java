package ru.milastoria.web;

import io.javalin.http.Context;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import ru.milastoria.domain.Category;
import ru.milastoria.domain.Lot;
import ru.milastoria.mapper.CategoryMapper;
import ru.milastoria.mapper.LotMapper;

import java.util.List;

/** robots.txt (статика) + sitemap.xml из БД: главная, галерея, категории, опубликованные лоты. */
public class SitemapController {

    private final SqlSessionFactory sqlSessionFactory;
    private final String baseUrl;

    public SitemapController(SqlSessionFactory sqlSessionFactory, String baseUrl) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public void sitemap(Context ctx) {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Стабильные lastmod: не Instant.now() на каждый запрос — иначе Google
        // видит «всё всегда новое» и хуже кэширует.
        String staticMod = "2026-10-03T00:00:00Z";
        appendUrl(xml, "/", staticMod, "1.0");
        appendUrl(xml, "/gallery", staticMod, "0.8");

        try (SqlSession session = sqlSessionFactory.openSession()) {
            CategoryMapper categoryMapper = session.getMapper(CategoryMapper.class);
            LotMapper lotMapper = session.getMapper(LotMapper.class);

            List<Category> categories = categoryMapper.findAll();
            for (Category cat : categories) {
                String mod = cat.getCreatedAt() != null && !cat.getCreatedAt().isBlank()
                        ? cat.getCreatedAt()
                        : staticMod;
                appendUrl(xml, "/gallery?category=" + cat.getSlug(), mod, "0.7");
            }

            List<Lot> lots = lotMapper.findPublished(null, null, null);
            for (Lot lot : lots) {
                String lastmod = lot.getCreatedAt() != null && !lot.getCreatedAt().isBlank()
                        ? lot.getCreatedAt()
                        : staticMod;
                appendUrl(xml, "/work/" + lot.getSlug(), lastmod, "0.9");
            }
        }

        xml.append("</urlset>\n");
        ctx.contentType("application/xml; charset=utf-8");
        ctx.header("Cache-Control", "max-age=3600");
        ctx.result(xml.toString());
    }

    private void appendUrl(StringBuilder xml, String path, String lastmod, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append(path).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("    <changefreq>weekly</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}
