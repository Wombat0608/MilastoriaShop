package ru.milastoria.domain;

public class LotImage {

    private long id;
    private long lotId;
    private String pathThumb;
    private String pathFull;
    private String alt;
    private String caption;
    private int sort;
    /** Доли выходного кропа; null — default SE из settings. */
    private Double wmX;
    private Double wmY;
    private Double wmWidth;
    /** 0..1; null — 1.0. */
    private Double wmOpacity;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLotId() {
        return lotId;
    }

    public void setLotId(long lotId) {
        this.lotId = lotId;
    }

    public String getPathThumb() {
        return pathThumb;
    }

    public void setPathThumb(String pathThumb) {
        this.pathThumb = pathThumb;
    }

    public String getPathFull() {
        return pathFull;
    }

    public void setPathFull(String pathFull) {
        this.pathFull = pathFull;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public Double getWmX() {
        return wmX;
    }

    public void setWmX(Double wmX) {
        this.wmX = wmX;
    }

    public Double getWmY() {
        return wmY;
    }

    public void setWmY(Double wmY) {
        this.wmY = wmY;
    }

    public Double getWmWidth() {
        return wmWidth;
    }

    public void setWmWidth(Double wmWidth) {
        this.wmWidth = wmWidth;
    }

    public Double getWmOpacity() {
        return wmOpacity;
    }

    public void setWmOpacity(Double wmOpacity) {
        this.wmOpacity = wmOpacity;
    }
}
