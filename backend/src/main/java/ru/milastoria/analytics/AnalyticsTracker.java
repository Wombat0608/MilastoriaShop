package ru.milastoria.analytics;

import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.HandlerType;
import io.javalin.http.SameSite;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.milastoria.mapper.AnalyticsMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Серверная аналитика посещений публичного сайта.
 *
 * Без внешних трекеров (GA и пр.): cookie-идентификатор + SQLite.
 * visitor_id — анонимный UUID, подписанный HMAC тем же секретом, что и
 * admin-сессия. session_id — короткая cookie, по ней собираются маршруты.
 * IP не храним (GDPR/приватность); ботов фильтруем по User-Agent.
 */
public class AnalyticsTracker {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsTracker.class);
    private static final String VISITOR_COOKIE = "mila_vid";
    private static final String SESSION_COOKIE = "mila_sid";
    private static final int SESSION_TTL_SECONDS = 2 * 60 * 60;
    private static final int VISITOR_TTL_SECONDS = 365 * 24 * 60 * 60;
    private static final String[] PATH_PREFIX_SKIP = {
            "/admin", "/assets", "/css", "/js", "/content",
            "/favicon", "/robots", "/sitemap"
    };

    private final SqlSessionFactory sqlSessionFactory;
    private final String sessionSecret;

    public AnalyticsTracker(SqlSessionFactory sqlSessionFactory, String sessionSecret) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.sessionSecret = sessionSecret;
    }

    /** Вызывается в app.before для GET-запросов публичных страниц. */
    public void track(Context ctx) {
        try {
            String path = sanitizePath(ctx.path());
            if (path == null || shouldSkip(path, ctx)) {
                return;
            }
            String userAgent = truncate(ctx.userAgent(), 400);
            if (isBot(userAgent)) {
                return;
            }

            String visitorId = ensureVisitorId(ctx);
            String sessionId = ensureSessionId(ctx);
            String referrer = truncate(ctx.header("Referer"), 500);
            String createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();

            try (SqlSession session = sqlSessionFactory.openSession(true)) {
                session.getMapper(AnalyticsMapper.class).insertVisit(
                        visitorId, sessionId, path, referrer, userAgent, createdAt);
            }
        } catch (Exception e) {
            // Аналитика не должна ронять ответ сайта.
            log.warn("Не удалось записать визит: {}", e.getMessage());
        }
    }

    /** ISO-8601 UTC «сейчас − N дней». */
    public static String sinceIso(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.SECONDS)
                .toString();
    }

    private static String sanitizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return "/";
        }
        String path = raw.split("\\?", 2)[0];
        if (path.isEmpty()) {
            path = "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        // Двойной слэш и точечные пути — не маршруты сайта.
        if (path.contains("//") || path.contains("..")) {
            return null;
        }
        return path;
    }

    private static boolean shouldSkip(String path, Context ctx) {
        if (ctx.method() != HandlerType.GET) {
            return true;
        }
        for (String prefix : PATH_PREFIX_SKIP) {
            if (path.equals(prefix) || path.startsWith(prefix + "/") || path.startsWith(prefix + ".")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            // curl/headless без UA считаем ботами — в дашборде не мешают.
            return true;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        return ua.contains("bot") || ua.contains("spider") || ua.contains("crawler")
                || ua.contains("slurp") || ua.contains("headless") || ua.contains("curl")
                || ua.contains("wget") || ua.contains("python-requests") || ua.contains("postman");
    }

    private String ensureVisitorId(Context ctx) {
        String existing = readSignedCookie(ctx, VISITOR_COOKIE);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        String id = UUID.randomUUID().toString();
        setHttpOnlyCookie(ctx, VISITOR_COOKIE, id, VISITOR_TTL_SECONDS);
        return id;
    }

    private String ensureSessionId(Context ctx) {
        String existing = readSignedCookie(ctx, SESSION_COOKIE);
        if (existing != null && !existing.isBlank()) {
            // Продлеваем сессию: активный посетитель не «рвёт» маршрут.
            setHttpOnlyCookie(ctx, SESSION_COOKIE, existing, SESSION_TTL_SECONDS);
            return existing;
        }
        String id = UUID.randomUUID().toString();
        setHttpOnlyCookie(ctx, SESSION_COOKIE, id, SESSION_TTL_SECONDS);
        return id;
    }

    private void setHttpOnlyCookie(Context ctx, String name, String payload, int maxAgeSeconds) {
        ctx.cookie(new Cookie(
                name,
                sign(payload),
                "/",
                maxAgeSeconds,
                false,
                0,
                true,
                null,
                null,
                SameSite.LAX
        ));
    }

    private String readSignedCookie(Context ctx, String name) {
        String raw = ctx.cookie(name);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int dot = raw.indexOf('.');
        if (dot < 0) {
            return null;
        }
        String payload = raw.substring(0, dot);
        String signature = raw.substring(dot + 1);
        if (!MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                hmacHex(payload).getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        return payload;
    }

    /** payload.value.signature — как admin_session, но payload = сам id. */
    private String sign(String payload) {
        return payload + "." + hmacHex(payload);
    }

    private String hmacHex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sessionSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() <= max ? t : t.substring(0, max);
    }
}
