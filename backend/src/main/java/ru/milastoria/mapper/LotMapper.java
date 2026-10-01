package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;
import ru.milastoria.domain.Lot;
import ru.milastoria.domain.LotImage;
import ru.milastoria.domain.LotVideo;
import ru.milastoria.view.AdminLotRow;

import java.util.List;

public interface LotMapper {

    List<Lot> findAll();

    /** Полные строки для rebuild FTS (включая seo/meta). */
    List<Lot> findAllForIndex();

    /** Список в админке: обложка-иконка, категория, created_at. Новый сверху. */
    List<AdminLotRow> findAllAdmin(@Param("categoryIds") List<Long> categoryIds);

    /** Поиск по FTS5 (ftsQuery — результат Fts.toMatchExpression) + фильтр по разделам. */
    List<AdminLotRow> searchAdmin(@Param("ftsQuery") String ftsQuery,
                                   @Param("categoryIds") List<Long> categoryIds);

    List<Lot> findFeatured(@Param("limit") int limit);

    Lot findBySlug(@Param("slug") String slug);

    Lot findById(@Param("id") long id);

    /** Любой из фильтров может быть null — тогда он просто не участвует в WHERE (см. XML). ftsQuery — MATCH-выражение FTS5. */
    List<Lot> findPublished(@Param("categorySlug") String categorySlug,
                             @Param("tag") String tag,
                             @Param("ftsQuery") String ftsQuery);

    List<Lot> findRelated(@Param("categoryId") long categoryId,
                           @Param("excludeId") long excludeId,
                           @Param("limit") int limit);

    int countPublishedByCategory(@Param("categoryId") long categoryId);

    boolean existsSlug(@Param("slug") String slug);

    int maxSort();

    int insert(Lot lot);

    int update(Lot lot);

    List<LotImage> findImagesByLotId(@Param("lotId") long lotId);

    LotImage findImageById(@Param("id") long id);

    void insertImage(LotImage image);

    void deleteImageById(@Param("id") long id);

    void updateImageSort(@Param("id") long id, @Param("sort") int sort);

    List<LotVideo> findVideosByLotId(@Param("lotId") long lotId);

    List<String> findTagNamesByLotId(@Param("lotId") long lotId);

    void upsertTag(@Param("name") String name);

    Long findTagIdByName(@Param("name") String name);

    void linkTag(@Param("lotId") long lotId, @Param("tagId") long tagId);

    void deleteTagsByLotId(@Param("lotId") long lotId);

    void clearFts();

    void deleteFromFts(@Param("lotId") long lotId);

    void insertIntoFts(@Param("lotId") long lotId,
                       @Param("title") String title,
                       @Param("annotation") String annotation,
                       @Param("seoText") String seoText,
                       @Param("metaTitle") String metaTitle,
                       @Param("metaDescription") String metaDescription,
                       @Param("tags") String tags);
}
