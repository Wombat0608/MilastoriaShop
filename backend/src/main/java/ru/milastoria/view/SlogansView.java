package ru.milastoria.view;

import ru.milastoria.domain.HeroSlide;
import ru.milastoria.domain.Slogan;

import java.util.List;

public record SlogansView(List<Slogan> slogans,
                          String aboutTitle,
                          String aboutHtml,
                          String aboutImage,
                          String galleryTitle,
                          String galleryDescription,
                          String heroLead,
                          String heroImage,
                          String heroImageMobile,
                          List<HeroSlide> heroSlides,
                          String error,
                          String notice) {

    public static RawHtml raw(String html) {
        return RawHtml.of(html);
    }
}
