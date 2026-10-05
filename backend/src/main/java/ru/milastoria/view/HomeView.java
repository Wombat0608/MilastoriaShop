package ru.milastoria.view;

import java.util.List;

/**
 * Главная: случайный H1-слоган + hero + направления + «О нас» + «Контакты».
 * Избранное и видео живут в галерее (только внутри выбранного раздела).
 * slogan/aboutHtml — сырой HTML через {@link RawHtml}.
 * heroImageMobile пустой → браузер берёт heroImage (десктоп).
 */
public record HomeView(List<CategoryTile> categories,
                       String sloganHtml,
                       String heroLead,
                       String heroImage,
                       String heroImageMobile,
                       String aboutTitle,
                       String aboutHtml,
                       String aboutImage,
                       String contactsTitle,
                       String contactsLead,
                       String contactsImage,
                       String contactsPhone,
                       String contactsEmail,
                       String contactsAddress,
                       List<ContactLine> contactsMessengers,
                       SiteContacts contacts,
                       String baseUrl) {

    public RawHtml slogan() {
        return RawHtml.of(sloganHtml);
    }

    public RawHtml aboutContent() {
        return RawHtml.of(aboutHtml);
    }

    /** Мобильная картинка hero; если не задана — та же, что для ПК. */
    public String heroImageOrFallback() {
        return (heroImageMobile == null || heroImageMobile.isBlank()) ? heroImage : heroImageMobile;
    }

    public boolean hasContactsPhone() {
        return contactsPhone != null && !contactsPhone.isBlank();
    }

    public boolean hasContactsEmail() {
        return contactsEmail != null && !contactsEmail.isBlank();
    }

    public boolean hasContactsAddress() {
        return contactsAddress != null && !contactsAddress.isBlank();
    }

    public boolean hasContactsMessengers() {
        return contactsMessengers != null && !contactsMessengers.isEmpty();
    }

    /** tel: из отображаемого телефона — «+7 (926) 429-64-58» → «tel:+79264296458». */
    public String phoneHref() {
        if (!hasContactsPhone()) {
            return "#";
        }
        String digits = contactsPhone.replaceAll("[^0-9+]", "");
        if (digits.isEmpty()) {
            return "#";
        }
        if (!digits.startsWith("+")) {
            digits = "+" + digits;
        }
        return "tel:" + digits;
    }

    /** mailto: из отображаемого адреса. */
    public String emailHref() {
        if (!hasContactsEmail()) {
            return "#";
        }
        return "mailto:" + contactsEmail.trim();
    }
}
