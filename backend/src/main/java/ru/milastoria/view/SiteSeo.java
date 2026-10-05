package ru.milastoria.view;

import java.util.List;

/** SEO-хелперы: абсолютные URL и JSON-LD (без экранирования — для script/ld+json). */
public final class SiteSeo {

    private SiteSeo() {
    }

    public static String absolute(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (path == null || path.isBlank()) {
            return base + "/";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    /** Default social image — hero; если файла нет, всё равно отдаём URL (Google/соцсети попробуют). */
    public static String defaultOgImage(String baseUrl) {
        return absolute(baseUrl, "/content/img/hero.jpg");
    }

    /**
     * JSON-LD LocalBusiness + WebSite. Телефон/адрес — из settings через SiteContacts.
     * Возвращаем сырой JSON для {@code <script type="application/ld+json">}.
     */
    public static String localBusinessJson(SiteContacts contacts, String baseUrl) {
        String url = absolute(baseUrl, "/");
        String image = defaultOgImage(baseUrl);
        String name = "Milastoria";
        String description =
                "Ателье нарядного детского и женского платья Milastoria: галерея работ, Family Look, "
                        + "пошив на заказ по меркам, подбор тканей. Кубинка, Московская область.";

        StringBuilder sb = new StringBuilder(1024);
        sb.append("{\n");
        sb.append("  \"@context\": \"https://schema.org\",\n");
        sb.append("  \"@graph\": [\n");
        sb.append("    {\n");
        sb.append("      \"@type\": \"WebSite\",\n");
        sb.append("      \"@id\": \"").append(url).append("#website\",\n");
        sb.append("      \"url\": \"").append(esc(url)).append("\",\n");
        sb.append("      \"name\": \"").append(esc(name)).append("\",\n");
        sb.append("      \"inLanguage\": \"ru-RU\",\n");
        sb.append("      \"publisher\": { \"@id\": \"").append(url).append("#organization\" }\n");
        sb.append("    },\n");
        sb.append("    {\n");
        sb.append("      \"@type\": \"LocalBusiness\",\n");
        sb.append("      \"@id\": \"").append(url).append("#organization\",\n");
        sb.append("      \"name\": \"").append(esc(name)).append("\",\n");
        sb.append("      \"alternateName\": \"Milastoria ателье\",\n");
        sb.append("      \"url\": \"").append(esc(url)).append("\",\n");
        sb.append("      \"image\": \"").append(esc(image)).append("\",\n");
        sb.append("      \"description\": \"").append(esc(description)).append("\",\n");
        sb.append("      \"priceRange\": \"₽₽\",\n");
        sb.append("      \"currenciesAccepted\": \"RUB\",\n");
        sb.append("      \"areaServed\": \"Москва и Московская область\",\n");
        if (contacts != null && contacts.hasPhone()) {
            sb.append("      \"telephone\": \"").append(esc(contacts.phone())).append("\",\n");
        }
        if (contacts != null && contacts.hasAddress()) {
            sb.append("      \"address\": {\n");
            sb.append("        \"@type\": \"PostalAddress\",\n");
            sb.append("        \"addressCountry\": \"RU\",\n");
            sb.append("        \"addressRegion\": \"Московская область\",\n");
            sb.append("        \"addressLocality\": \"Кубинка\",\n");
            sb.append("        \"streetAddress\": \"").append(esc(contacts.address())).append("\"\n");
            sb.append("      },\n");
        }
        sb.append("      \"sameAs\": [\n");
        sb.append("        \"https://vk.com/milastoria\",\n");
        sb.append("        \"https://t.me/Milastoria\"\n");
        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    /** BreadcrumbList для карточки работы — помогает Google показывать хлебные крошки. */
    public static String breadcrumbJson(SiteContacts ignored, String baseUrl, String categoryTitle, String categorySlug, String lotTitle, String lotSlug) {
        String home = absolute(baseUrl, "/");
        String catUrl = absolute(baseUrl, "/gallery?category=" + categorySlug);
        String lotUrl = absolute(baseUrl, "/work/" + lotSlug);
        StringBuilder sb = new StringBuilder(768);
        sb.append("{\n");
        sb.append("  \"@context\": \"https://schema.org\",\n");
        sb.append("  \"@type\": \"BreadcrumbList\",\n");
        sb.append("  \"itemListElement\": [\n");
        sb.append("    { \"@type\": \"ListItem\", \"position\": 1, \"name\": \"Главная\", \"item\": \"").append(esc(home)).append("\" },\n");
        sb.append("    { \"@type\": \"ListItem\", \"position\": 2, \"name\": \"Галерея\", \"item\": \"").append(esc(absolute(baseUrl, "/gallery"))).append("\" }");
        if (categorySlug != null && !categorySlug.isBlank()) {
            sb.append(",\n    { \"@type\": \"ListItem\", \"position\": 3, \"name\": \"").append(esc(categoryTitle))
                    .append("\", \"item\": \"").append(esc(catUrl)).append("\" }");
            sb.append(",\n    { \"@type\": \"ListItem\", \"position\": 4, \"name\": \"").append(esc(lotTitle))
                    .append("\", \"item\": \"").append(esc(lotUrl)).append("\" }");
        } else {
            sb.append(",\n    { \"@type\": \"ListItem\", \"position\": 3, \"name\": \"").append(esc(lotTitle))
                    .append("\", \"item\": \"").append(esc(lotUrl)).append("\" }");
        }
        sb.append("\n  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }
}
