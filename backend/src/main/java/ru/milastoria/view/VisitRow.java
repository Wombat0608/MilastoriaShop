package ru.milastoria.view;

/** Строка визита для разбора маршрута сессии. */
public record VisitRow(String sessionId,
                       String visitorId,
                       String path,
                       String referrer,
                       String createdAt) {
}
