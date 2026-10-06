package ru.milastoria.domain;

/**
 * Файл медиатеки админки: фотография или видео, загруженные с телефона
 * до прикрепления к лоту. Оригинал хранится на диске; после attach
 * запись помечается флагом applied (файл остаётся на диске).
 */
public class MediaFile {

    public static final String KIND_IMAGE = "image";
    public static final String KIND_VIDEO = "video";

    private long id;
    private String kind;
    private String originalName;
    private String path;
    private String thumbPath;
    private Integer width;
    private Integer height;
    private String uploadedAt;
    /** "yyyy-MM-dd HH:mm:ss" — дата из EXIF / имени файла / mtime. */
    private String exifDatetime;
    private String exifMake;
    private String exifModel;
    private Integer exifOrientation;
    private String exifJson;
    private String exifSearch;
    private Integer cropX;
    private Integer cropY;
    private Integer cropW;
    private Integer cropH;
    /** Группа пачки загрузки; null — без группы. */
    private Long groupId;
    /** Denormalized имя из media_groups (grp-N). */
    private String groupName;
    /** 1 = прикреплён к лоту; 0/null = свободен в медиатеке. */
    private Integer applied;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getThumbPath() {
        return thumbPath;
    }

    public void setThumbPath(String thumbPath) {
        this.thumbPath = thumbPath;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(String uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getExifDatetime() {
        return exifDatetime;
    }

    public void setExifDatetime(String exifDatetime) {
        this.exifDatetime = exifDatetime;
    }

    public String getExifMake() {
        return exifMake;
    }

    public void setExifMake(String exifMake) {
        this.exifMake = exifMake;
    }

    public String getExifModel() {
        return exifModel;
    }

    public void setExifModel(String exifModel) {
        this.exifModel = exifModel;
    }

    public Integer getExifOrientation() {
        return exifOrientation;
    }

    public void setExifOrientation(Integer exifOrientation) {
        this.exifOrientation = exifOrientation;
    }

    public String getExifJson() {
        return exifJson;
    }

    public void setExifJson(String exifJson) {
        this.exifJson = exifJson;
    }

    public String getExifSearch() {
        return exifSearch;
    }

    public void setExifSearch(String exifSearch) {
        this.exifSearch = exifSearch;
    }

    public Integer getCropX() {
        return cropX;
    }

    public void setCropX(Integer cropX) {
        this.cropX = cropX;
    }

    public Integer getCropY() {
        return cropY;
    }

    public void setCropY(Integer cropY) {
        this.cropY = cropY;
    }

    public Integer getCropW() {
        return cropW;
    }

    public void setCropW(Integer cropW) {
        this.cropW = cropW;
    }

    public Integer getCropH() {
        return cropH;
    }

    public void setCropH(Integer cropH) {
        this.cropH = cropH;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Integer getApplied() {
        return applied;
    }

    public void setApplied(Integer applied) {
        this.applied = applied;
    }

    public boolean isImage() {
        return KIND_IMAGE.equals(kind);
    }

    public boolean isVideo() {
        return KIND_VIDEO.equals(kind);
    }

    public boolean isApplied() {
        return applied != null && applied != 0;
    }

    /** Дата для сортировки: EXIF, иначе дата загрузки. */
    public String sortDatetime() {
        return (exifDatetime != null && !exifDatetime.isBlank()) ? exifDatetime : uploadedAt;
    }
}
