package ru.milastoria.domain;

/** Слайд промо-шапки (hero) главной: пара ПК+моб или видео. */
public class HeroSlide {

    public static final String KIND_IMAGE = "image";
    public static final String KIND_VIDEO = "video";

    private long id;
    private int sort;
    private String kind;
    private String desktopPath;
    private String mobilePath;
    private String alt;
    private String createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getDesktopPath() {
        return desktopPath;
    }

    public void setDesktopPath(String desktopPath) {
        this.desktopPath = desktopPath;
    }

    public String getMobilePath() {
        return mobilePath;
    }

    public void setMobilePath(String mobilePath) {
        this.mobilePath = mobilePath;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isVideo() {
        return KIND_VIDEO.equals(kind);
    }

    public boolean isImage() {
        return !isVideo();
    }

    /** Мобильный путь; для image без моб-файла — тот же, что ПК. */
    public String mobileOrDesktop() {
        if (mobilePath == null || mobilePath.isBlank()) {
            return desktopPath;
        }
        return mobilePath;
    }

    public String altOrFallback() {
        if (alt != null && !alt.isBlank()) {
            return alt;
        }
        return isVideo() ? "Видео Milastoria" : "Нарядные платья Milastoria";
    }
}
