package ru.milastoria;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppConfigTest {

    @Test
    void parsesNestedYaml() {
        Map<String, String> m = AppConfig.parseYaml("""
                admin:
                  user: admin
                  password: secret # comment
                storage:
                  content_dir: /data/content
                server:
                  port: 8080
                """);
        assertEquals("admin", m.get("admin.user"));
        assertEquals("secret", m.get("admin.password"));
        assertEquals("/data/content", m.get("storage.content_dir"));
        assertEquals("8080", m.get("server.port"));
        assertNull(m.get("admin"));
    }

    @Test
    void unquotesValues() {
        Map<String, String> m = AppConfig.parseYaml("""
                site:
                  base_url: "https://milastoria.ru"
                """);
        assertEquals("https://milastoria.ru", m.get("site.base_url"));
    }
}
