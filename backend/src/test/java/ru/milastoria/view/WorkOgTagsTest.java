package ru.milastoria.view;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import ru.milastoria.domain.Lot;
import ru.milastoria.domain.LotImage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Превью ссылки в мессенджерах берётся из og:image, а не снимка страницы —
 * значит у страницы работы там должно быть первое фото работы, а не hero сайта.
 */
class WorkOgTagsTest {

    private static final String BASE_URL = "https://milastoria.com";

    @Test
    void workPageUsesFirstPhotoAsOgImage() {
        String html = renderWork(viewWithImages(List.of(
                image("/content/img/plate-denim-meh__1__full.jpg", "Джинсовое платье с меховой отделкой"),
                image("/content/img/plate-denim-meh__2__full.jpg", "Второй кадр"))));

        assertTrue(html.contains(
                        "<meta property=\"og:image\" content=\"https://milastoria.com/content/img/plate-denim-meh__1__full.jpg\">"),
                "og:image — первое фото работы, не hero");
        assertTrue(html.contains(
                        "<meta name=\"twitter:image\" content=\"https://milastoria.com/content/img/plate-denim-meh__1__full.jpg\">"),
                "twitter:image повторяет og:image");
        assertTrue(html.contains("<meta property=\"og:image:alt\" content=\"Джинсовое платье с меховой отделкой\">"),
                "og:image:alt — alt первого фото");
        assertTrue(html.contains("<meta property=\"og:type\" content=\"product\">"));
    }

    @Test
    void workPageFallsBackToHeroWhenThereAreNoPhotos() {
        String html = renderWork(viewWithImages(List.of()));

        assertTrue(html.contains(
                        "<meta property=\"og:image\" content=\"https://milastoria.com/content/img/hero.jpg\">"),
                "без фото — общий hero, чтобы превью не пропало");
        assertTrue(html.contains("<meta property=\"og:image:alt\" content=\"Джинсовое платье с меховой отделкой\">"),
                "alt — заголовок работы, а не пустая строка");
    }

    @Test
    void ogImageHelperAcceptsPathAndAbsoluteUrl() {
        assertTrue(SiteSeo.ogImage(BASE_URL, "/content/img/a.jpg")
                .equals("https://milastoria.com/content/img/a.jpg"));
        assertTrue(SiteSeo.ogImage(BASE_URL, "https://cdn.example/x.jpg")
                .equals("https://cdn.example/x.jpg"));
        assertTrue(SiteSeo.ogImage(BASE_URL, null).equals("https://milastoria.com/content/img/hero.jpg"));
    }

    private static WorkView viewWithImages(List<LotImage> images) {
        Lot lot = new Lot();
        lot.setSlug("plate-denim-meh");
        lot.setTitle("Джинсовое платье с меховой отделкой");
        lot.setAnnotation("Приталенное платье из синего денима.");
        return new WorkView(
                lot,
                "Женское платье",
                "women",
                images,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                BASE_URL
        );
    }

    private static LotImage image(String pathFull, String alt) {
        LotImage img = new LotImage();
        img.setPathFull(pathFull);
        img.setPathThumb(pathFull);
        img.setAlt(alt);
        return img;
    }

    private static String renderWork(WorkView view) {
        StringOutput out = new StringOutput();
        TemplateEngine.createPrecompiled(ContentType.Html).render("work.jte", view, out);
        return out.toString();
    }
}
