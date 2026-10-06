package ru.milastoria.domain;

/** Группа загрузки медиафайлов (одна пачка с телефона). Имя: grp-${id}. */
public class MediaGroup {

    private long id;
    private String name;
    private String createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
