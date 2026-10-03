package ru.milastoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Конфигурация приложения из YAML (config/app.yml) + переменные окружения.
 *
 * Приоритет: ENV → YAML → дефолт в коде.
 * YAML — простой двухуровневый вид (admin.password, storage.content_dir …),
 * без внешних библиотек парсера — проект маленький, зависимостей лишних нет.
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private final Map<String, String> values = new HashMap<>();

    private AppConfig() {
    }

    /** Загружает YAML (если есть), затем накрывает пустыми значениями ENV. */
    public static AppConfig load() {
        AppConfig config = new AppConfig();
        String path = firstNonBlank(System.getenv("APP_CONFIG"), "config/app.yml");
        Path file = Path.of(path);
        if (Files.isRegularFile(file)) {
            try {
                config.values.putAll(parseYaml(Files.readString(file)));
                log.info("Конфигурация: {}", file.toAbsolutePath().normalize());
            } catch (Exception e) {
                log.warn("Не удалось прочитать {}: {}", file, e.getMessage());
            }
        } else {
            log.warn("Файл конфигурации не найден: {} (используются env/дефолты)", file.toAbsolutePath());
        }
        return config;
    }

    /** ENV имеет приоритет над YAML. */
    public String get(String yamlKey, String envKey, String fallback) {
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromYaml = values.get(yamlKey);
        if (fromYaml != null && !fromYaml.isBlank()) {
            return fromYaml.trim();
        }
        return fallback;
    }

    public String requireFromYamlOrEnv(String yamlKey, String envKey, String fallbackIfMissing) {
        return get(yamlKey, envKey, fallbackIfMissing);
    }

    /** Для отладки/логов: только значения из YAML (без env). */
    public String yamlValue(String yamlKey) {
        return values.get(yamlKey);
    }

    /**
     * Парсит плоский/двухуровневый YAML:
     * <pre>
     * admin:
     *   password: secret
     * storage:
     *   content_dir: /data/content
     * </pre>
     * → admin.password=secret, storage.content_dir=/data/content
     */
    static Map<String, String> parseYaml(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        String prefix = "";
        for (String rawLine : text.split("\n")) {
            String line = rawLine.replace("\r", "");
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int indent = line.length() - line.stripLeading().length();
            int colon = indexOfKeyColon(trimmed);
            if (colon < 0) {
                continue;
            }
            String key = trimmed.substring(0, colon).trim().toLowerCase();
            String value = trimmed.substring(colon + 1).trim();
            value = unquote(value);
            // убираем комментарий в конце строки: value: foo # bar
            if (!value.startsWith("\"") && !value.startsWith("'")) {
                int hash = value.indexOf(" #");
                if (hash > 0) {
                    value = value.substring(0, hash).trim();
                }
            }
            if (indent == 0) {
                prefix = key;
                if (!value.isEmpty()) {
                    out.put(key, value);
                    prefix = "";
                }
            } else {
                String full = prefix.isEmpty() ? key : prefix + "." + key;
                out.put(full, value);
            }
        }
        return out;
    }

    private static int indexOfKeyColon(String line) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (c == ':' && !inSingle && !inDouble) {
                return i;
            }
        }
        return -1;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char a = value.charAt(0);
            char b = value.charAt(value.length() - 1);
            if ((a == '"' && b == '"') || (a == '\'' && b == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    /** Удобный доступ к ресурсу (не используется в runtime, оставлен для тестов). */
    static String readResource(String classpath) throws IOException {
        try (InputStream in = AppConfig.class.getResourceAsStream(classpath)) {
            if (in == null) {
                throw new IOException("not found: " + classpath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
