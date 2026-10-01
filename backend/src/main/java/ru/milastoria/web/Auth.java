package ru.milastoria.web;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Логин/пароль/сессия для одного администратора — без таблицы users,
 * без Spring Security. Пароль хранится как SHA-256 (env-переменная),
 * не в открытом виде; сессия — подписанная HMAC-SHA256 кука без
 * состояния на сервере (не нужна таблица сессий).
 *
 * SHA-256 без соли/KDF — сознательный компромисс для одного пароля
 * одного администратора за HTTPS на собственном VPS, не для
 * многопользовательской системы с чужими паролями.
 */
public class Auth {

    private static final long SESSION_TTL_SECONDS = 7 * 24 * 60 * 60; // неделя

    private final String username;
    private final String passwordHashHex;
    private final String sessionSecret;

    public Auth(String username, String passwordHashHex, String sessionSecret) {
        this.username = username;
        this.passwordHashHex = passwordHashHex.toLowerCase();
        this.sessionSecret = sessionSecret;
    }

    public boolean checkCredentials(String candidateUser, String candidatePassword) {
        if (candidateUser == null || candidatePassword == null) {
            return false;
        }
        boolean userOk = constantTimeEquals(username, candidateUser);
        boolean passOk = constantTimeEquals(passwordHashHex, sha256Hex(candidatePassword));
        return userOk && passOk;
    }

    /** payload = "expiryEpochSeconds", подпись = HMAC-SHA256(payload). */
    public String issueSessionCookie() {
        String payload = Long.toString(Instant.now().getEpochSecond() + SESSION_TTL_SECONDS);
        return payload + "." + hmacHex(payload);
    }

    public boolean isValidSession(String cookieValue) {
        if (cookieValue == null) {
            return false;
        }
        int dot = cookieValue.indexOf('.');
        if (dot < 0) {
            return false;
        }
        String payload = cookieValue.substring(0, dot);
        String signature = cookieValue.substring(dot + 1);
        if (!constantTimeEquals(signature, hmacHex(payload))) {
            return false;
        }
        try {
            return Instant.now().getEpochSecond() < Long.parseLong(payload);
        } catch (NumberFormatException e) {
            return false;
        }
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

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
