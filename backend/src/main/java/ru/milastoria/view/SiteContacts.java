package ru.milastoria.view;

import java.util.List;

/** Контакты сайта из settings (contacts_*); используются в шапке и подвале. */
public record SiteContacts(String phone,
                           String phoneHref,
                           String email,
                           String address,
                           List<ContactLine> messengers) {

    public static final String DEFAULT_PHONE = "+7 (926) 429-64-58";
    public static final String DEFAULT_EMAIL = "shop@milastoria.com";
    public static final String DEFAULT_ADDRESS = "Московская область, город Кубинка";
    public static final String DEFAULT_MESSENGERS =
            "WhatsApp|https://wa.me/79264296458\nTelegram|https://t.me/Milastoria";

    /** Приоритет: settings → дефолты кода (как SiteController.home). */
    public static SiteContacts fromSettings(String phone, String email, String address, String messengersRaw) {
        String p = (phone == null || phone.isBlank()) ? DEFAULT_PHONE : phone;
        String e = (email == null || email.isBlank()) ? DEFAULT_EMAIL : email;
        String a = (address == null || address.isBlank()) ? DEFAULT_ADDRESS : address;
        String m = (messengersRaw == null || messengersRaw.isBlank()) ? DEFAULT_MESSENGERS : messengersRaw;
        return new SiteContacts(p, telHref(p), e, a, ContactLine.parseMessengers(m));
    }

    public static String telHref(String phone) {
        if (phone == null || phone.isBlank()) {
            return "#";
        }
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.isEmpty()) {
            return "#";
        }
        if (!digits.startsWith("+")) {
            digits = "+" + digits;
        }
        return "tel:" + digits;
    }

    public static String mailtoHref(String email) {
        if (email == null || email.isBlank()) {
            return "#";
        }
        return "mailto:" + email.trim();
    }

    public boolean hasPhone() {
        return phone != null && !phone.isBlank();
    }

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }

    public boolean hasAddress() {
        return address != null && !address.isBlank();
    }

    public boolean hasMessengers() {
        return messengers != null && !messengers.isEmpty();
    }
}
