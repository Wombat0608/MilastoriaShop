package ru.milastoria.web;

import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import ru.milastoria.domain.Category;
import ru.milastoria.domain.HeroSlide;
import ru.milastoria.domain.Lot;
import ru.milastoria.mapper.CategoryMapper;
import ru.milastoria.mapper.DictMapper;
import ru.milastoria.mapper.HeroSlideMapper;
import ru.milastoria.mapper.LotMapper;
import ru.milastoria.mapper.SettingsMapper;
import ru.milastoria.mapper.SloganMapper;
import ru.milastoria.util.Fts;
import ru.milastoria.domain.Slogan;
import ru.milastoria.view.CategoryOption;
import ru.milastoria.view.CategoryTile;
import ru.milastoria.view.ContactLine;
import ru.milastoria.view.GalleryView;
import ru.milastoria.view.HomeView;
import ru.milastoria.view.LotCard;
import ru.milastoria.view.SiteContacts;
import ru.milastoria.view.VideoTile;
import ru.milastoria.view.WorkView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Три страницы сайта. Держим их вместе, пока это ~200 строк и общие
 * приватные хелперы (toCard, categoryTitleById) — как только контроллер
 * разрастётся, разносить по классам будет видно, где резать, а не
 * заранее угадывать границы.
 */
public class SiteController {

    private static final Random RANDOM = new Random();
    private static final String DEFAULT_SLOGAN =
            "Платье — начало вашей <em>истории</em>";
    private static final String DEFAULT_ABOUT_TITLE = "Меньше витрины, больше характера";
    private static final String DEFAULT_ABOUT_HTML =
            "<p>Мы шьём нарядные детские и взрослые платья с 2016 года. "
                    + "Работаем <strong>по меркам — без промежуточных примерок</strong>: "
                    + "опыт и точный расчёт позволяют снять мерки, подобрать ткань и материалы "
                    + "и сшить платье, которое садится с первого раза.</p>"
                    + "<p>Подбор тканей, декора и фурнитуры — часть заказа: находим то, "
                    + "что нужно именно вашему событию, силуэту и настроению.</p>";
    private static final String DEFAULT_GALLERY_TITLE = "Галерея работ";
    private static final String DEFAULT_GALLERY_LEAD =
            "Коллекция нарядных детских, женских платьев, Family Look и работ клиентов. "
                    + "Каждый лот — с аннотацией, тегами и, при необходимости, видео.";
    private static final String DEFAULT_CONTACTS_TITLE = "Обсудим платье для вашего события";
    private static final String DEFAULT_CONTACTS_LEAD =
            "Напишите в WhatsApp, Telegram или позвоните — расскажем о сроках, "
                    + "посадке и вариантах комплекта.";
    private static final String DEFAULT_CONTACTS_PHONE = "+7 (926) 429-64-58";
    private static final String DEFAULT_CONTACTS_EMAIL = "shop@milastoria.com";
    private static final String DEFAULT_CONTACTS_ADDRESS = "Московская область, город Кубинка";
    private static final String DEFAULT_CONTACTS_MESSENGERS =
            "WhatsApp|https://wa.me/79264296458\nTelegram|https://t.me/Milastoria";
    private static final String DEFAULT_CONTACTS_IMAGE = "/content/img/client-01.jpg";
    private static final String DEFAULT_HERO_LEAD =
            "Нарядные платья ручной работы для детей и взрослых. Шьём по меркам — "
                    + "без промежуточных примерок; подбираем ткани и материалы под ваш образ.";
    private static final String DEFAULT_HERO_IMAGE = "/content/img/hero.jpg";

    private final SqlSessionFactory sqlSessionFactory;
    private final TemplateEngine templateEngine;
    private final String baseUrl;

    public SiteController(SqlSessionFactory sqlSessionFactory, TemplateEngine templateEngine, String baseUrl) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.templateEngine = templateEngine;
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://milastoria.com" : baseUrl.trim();
    }

    public void home(Context ctx) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            CategoryMapper categoryMapper = session.getMapper(CategoryMapper.class);
            LotMapper lotMapper = session.getMapper(LotMapper.class);
            SloganMapper sloganMapper = session.getMapper(SloganMapper.class);
            SettingsMapper settings = session.getMapper(SettingsMapper.class);

            List<CategoryTile> categories = categoryMapper.findAll().stream()
                    .map(cat -> new CategoryTile(cat.getSlug(), cat.getTitle(), cat.getCoverImage(),
                            lotMapper.countPublishedByCategory(cat.getId()), cat.getLead()))
                    .toList();

            String randomSlogan = pickRandomSlogan(sloganMapper);
            String slogan = (randomSlogan == null || randomSlogan.isBlank())
                    ? DEFAULT_SLOGAN
                    : randomSlogan;

            String heroLead = nvlSettings(settings.get("hero_lead"), DEFAULT_HERO_LEAD);
            String heroImage = nvlSettings(settings.get("hero_image"), DEFAULT_HERO_IMAGE);
            String heroImageMobile = nvlSettings(settings.get("hero_image_mobile"), "");
            List<HeroSlide> heroSlides = session.getMapper(HeroSlideMapper.class).findAll();
            // fallback: нет слайдов в БД — один слайд из legacy settings
            if (heroSlides.isEmpty()) {
                HeroSlide legacy = new HeroSlide();
                legacy.setId(0);
                legacy.setSort(0);
                legacy.setKind(HeroSlide.KIND_IMAGE);
                legacy.setDesktopPath(heroImage);
                legacy.setMobilePath(heroImageMobile);
                heroSlides = List.of(legacy);
            }

            String aboutTitle = settings.get("about_title");
            if (aboutTitle == null || aboutTitle.isBlank()) {
                aboutTitle = DEFAULT_ABOUT_TITLE;
            }
            String aboutHtml = settings.get("about_html");
            if (aboutHtml == null || aboutHtml.isBlank()) {
                aboutHtml = DEFAULT_ABOUT_HTML;
            }
            String aboutImage = settings.get("about_image");
            if (aboutImage == null || aboutImage.isBlank()) {
                aboutImage = "/content/img/client-03.jpg";
            }

            String contactsTitle = settings.get("contacts_title");
            if (contactsTitle == null || contactsTitle.isBlank()) {
                contactsTitle = DEFAULT_CONTACTS_TITLE;
            }
            String contactsLead = settings.get("contacts_lead");
            if (contactsLead == null || contactsLead.isBlank()) {
                contactsLead = DEFAULT_CONTACTS_LEAD;
            }
            String contactsImage = settings.get("contacts_image");
            if (contactsImage == null || contactsImage.isBlank()) {
                contactsImage = DEFAULT_CONTACTS_IMAGE;
            }
            String contactsPhone = settings.get("contacts_phone");
            if (contactsPhone == null || contactsPhone.isBlank()) {
                contactsPhone = DEFAULT_CONTACTS_PHONE;
            }
            String contactsEmail = settings.get("contacts_email");
            if (contactsEmail == null || contactsEmail.isBlank()) {
                contactsEmail = DEFAULT_CONTACTS_EMAIL;
            }
            String contactsAddress = settings.get("contacts_address");
            if (contactsAddress == null || contactsAddress.isBlank()) {
                contactsAddress = DEFAULT_CONTACTS_ADDRESS;
            }
            String contactsMessengersRaw = settings.get("contacts_messengers");
            if (contactsMessengersRaw == null || contactsMessengersRaw.isBlank()) {
                contactsMessengersRaw = DEFAULT_CONTACTS_MESSENGERS;
            }
            List<ContactLine> contactsMessengers = ContactLine.parseMessengers(contactsMessengersRaw);
            SiteContacts siteContacts = SiteContacts.fromSettings(
                    contactsPhone, contactsEmail, contactsAddress, contactsMessengersRaw);

            render(ctx, "home.jte",
                    new HomeView(categories, slogan,
                            heroLead, heroImage, heroImageMobile, heroSlides,
                            aboutTitle, aboutHtml, aboutImage,
                            contactsTitle, contactsLead, contactsImage,
                            contactsPhone, contactsEmail, contactsAddress,
                            contactsMessengers, siteContacts, baseUrl));
        }
    }

    public void gallery(Context ctx) {
        String categorySlug = ctx.queryParam("category");
        String tag = ctx.queryParam("tag");
        String query = normalize(ctx.queryParam("q"));
        // Общий с админкой FTS5: «золотое платье» ищет по title/annotation/seo/tags одинаково
        String ftsQuery = Fts.toMatchExpression(query);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            CategoryMapper categoryMapper = session.getMapper(CategoryMapper.class);
            LotMapper lotMapper = session.getMapper(LotMapper.class);

            List<CategoryOption> options = categoryMapper.findAll().stream()
                    .map(cat -> new CategoryOption(cat.getSlug(), cat.getTitle(),
                            cat.getSlug().equals(categorySlug)))
                    .toList();

            List<LotCard> lots = lotMapper.findPublished(categorySlug, tag, ftsQuery).stream()
                    .map(lot -> toCard(lotMapper, lot))
                    .toList();

            // Избранное + «Образы в движении» — только чистый просмотр раздела
            List<LotCard> featured = List.of();
            List<VideoTile> videos = List.of();
            boolean pureCategory = categorySlug != null && tag == null && ftsQuery == null;
            if (pureCategory) {
                Category cat = categoryMapper.findBySlug(categorySlug);
                if (cat != null) {
                    // Обычная сетка (не bento): фото лотов 3:4, bento рвал кадры.
                    // До 5 карточек, порядок случайный при каждом заходе.
                    List<LotCard> featuredPool = lotMapper.findFeaturedByCategory(cat.getId(), 20).stream()
                            .map(lot -> toCard(lotMapper, lot))
                            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
                    Collections.shuffle(featuredPool, RANDOM);
                    featured = featuredPool.size() > 5 ? List.copyOf(featuredPool.subList(0, 5)) : List.copyOf(featuredPool);

                    videos = lotMapper.findFeaturedByCategory(cat.getId(), 20).stream()
                            .flatMap(lot -> lotMapper.findVideosByLotId(lot.getId()).stream()
                                    .map(video -> new VideoTile(
                                            lot.getTitle(),
                                            "",
                                            video.getPosterPath(),
                                            "/work/" + lot.getSlug() + "#video-" + video.getId(),
                                            video.getCaption())))
                            .limit(3)
                            .toList();
                }
            }

            String title;
            String lead;
            if (tag != null) {
                title = "Тег: " + tag;
                lead = "Подборка по тегу «" + tag + "». Нарядные детские и женские платья ателье Milastoria.";
            } else if (categorySlug != null) {
                Category cat = categoryMapper.findBySlug(categorySlug);
                title = cat != null ? cat.getTitle() : "Галерея";
                // Лид раздела короче SEO-текста — на странице раздела сначала он
                lead = cat != null && cat.getLead() != null && !cat.getLead().isBlank()
                        ? cat.getLead()
                        : (cat != null ? cat.getSeoText() : "");
            } else {
                SettingsMapper settings = session.getMapper(SettingsMapper.class);
                title = nvlSettings(settings.get("gallery_title"), DEFAULT_GALLERY_TITLE);
                lead = nvlSettings(settings.get("gallery_description"), DEFAULT_GALLERY_LEAD);
            }

            SiteContacts siteContacts = loadSiteContacts(session);

            render(ctx, "gallery.jte",
                    new GalleryView(title, lead, options, tag, query, lots, featured, videos, siteContacts, baseUrl));
        }
    }

    public void work(Context ctx) {
        String slug = ctx.pathParam("slug");

        try (SqlSession session = sqlSessionFactory.openSession()) {
            LotMapper lotMapper = session.getMapper(LotMapper.class);
            CategoryMapper categoryMapper = session.getMapper(CategoryMapper.class);
            DictMapper dictMapper = session.getMapper(DictMapper.class);

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
                    dictMapper.findNamesByLot(lot.getId(), "occasion"),
                    dictMapper.findNamesByLot(lot.getId(), "fabric"),
                    dictMapper.findNamesByLot(lot.getId(), "tag"),
                    related,
                    loadSiteContacts(session),
                    baseUrl
            );

            render(ctx, "work.jte", view);
        }
    }

    /** Контакты из settings contacts_* для шапки/подвала (все публичные страницы). */
    private static SiteContacts loadSiteContacts(SqlSession session) {
        SettingsMapper settings = session.getMapper(SettingsMapper.class);
        return SiteContacts.fromSettings(
                settings.get("contacts_phone"),
                settings.get("contacts_email"),
                settings.get("contacts_address"),
                settings.get("contacts_messengers"));
    }

    private LotCard toCard(LotMapper lotMapper, Lot lot) {
        var images = lotMapper.findImagesByLotId(lot.getId());
        String cover = images.isEmpty() ? null : images.get(0).getPathThumb();
        boolean hasVideo = !lotMapper.findVideosByLotId(lot.getId()).isEmpty();
        return new LotCard(lot.getSlug(), lot.getTitle(), lot.getAnnotation(), cover, hasVideo);
    }

    /** Случайный включённый H1-слоган. В Java — детерминированнее, чем ORDER BY RANDOM() в SQLite. */
    private static String pickRandomSlogan(SloganMapper sloganMapper) {
        List<Slogan> enabled = sloganMapper.findEnabled();
        if (enabled == null || enabled.isEmpty()) {
            return null;
        }
        return enabled.get(RANDOM.nextInt(enabled.size())).getText();
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** Пустая строка из settings не должна затирать дефолтный текст. */
    private static String nvlSettings(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private void render(Context ctx, String template, Object model) {
        StringOutput output = new StringOutput();
        templateEngine.render(template, model, output);
        // HTML меняется на каждый запрос (слоган, списки) — браузер не должен держать кеш
        ctx.header("Cache-Control", "no-cache");
        ctx.html(output.toString());
    }
}
