package ru.milastoria.view;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Строка списка лотов в админке: иконка (первый thumb), дата создания,
 * slug/id, статус, категория, поле «Порядок» (lots.sort).
 * Обычный класс с getter'ами — MyBatis заполняет через set*;
 * record в этой роли капризен без -parameters в компиляторе.
 */
public class AdminLotRow {

    private long id;
    private String slug;
    private String title;
    private String status;
    private boolean featured;
    private String createdAt;
    private String coverThumb;
    private String categoryTitle;
    private String categorySlug;
    private int sort;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCoverThumb() {
        return coverThumb;
    }

    public void setCoverThumb(String coverThumb) {
        this.coverThumb = coverThumb;
    }

    public String getCategoryTitle() {
        return categoryTitle;
    }

    public void setCategoryTitle(String categoryTitle) {
        this.categoryTitle = categoryTitle;
    }

    public String getCategorySlug() {
        return categorySlug;
    }

    public void setCategorySlug(String categorySlug) {
        this.categorySlug = categorySlug;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public boolean published() {
        return "published".equals(status);
    }

    /** «1 Oct 2025, 14:03» по UTC; «—» если created_at null (старые строки). */
    public String createdAtFormatted() {
        if (createdAt == null || createdAt.isBlank()) {
            return "—";
        }
        try {
            Instant instant = Instant.parse(createdAt);
            return DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
                    .withZone(ZoneOffset.UTC)
                    .format(instant);
        } catch (Exception e) {
            return createdAt;
        }
    }
}
