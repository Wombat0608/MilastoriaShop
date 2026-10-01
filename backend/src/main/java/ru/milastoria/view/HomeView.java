package ru.milastoria.view;

import java.util.List;

/**
 * Главная: случайный H1-слоган + «О нас» + «Контакты».
 * slogan/aboutHtml — сырой HTML через {@link RawHtml}.
 */
public record HomeView(List<CategoryTile> categories,
                       List<LotCard> featuredLots,
                       List<VideoTile> videos,
                       String sloganHtml,
                       String aboutTitle,
                       String aboutHtml,
                       String aboutImage,
                       String contactsTitle,
                       String contactsLead,
                       String contactsImage,
                       String contactsPhone,
                       String contactsEmail,
                       String contactsAddress,
                       List<ContactLine> contactsMessengers) {

    public RawHtml slogan() {
        return RawHtml.of(sloganHtml);
    }

    public RawHtml aboutContent() {
        return RawHtml.of(aboutHtml);
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
