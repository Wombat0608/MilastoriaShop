package ru.milastoria.view;

import java.util.List;

public record SlogansView(List<ru.milastoria.domain.Slogan> slogans,
                          String aboutTitle,
                          String aboutHtml,
                          String aboutImage,
                          String galleryTitle,
                          String galleryDescription,
                          String heroLead,
                          String heroImage,
                          String heroImageMobile,
                          String error,
                          String notice) {

    public static RawHtml raw(String html) {
        return RawHtml.of(html);
    }
}
