package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;
import ru.milastoria.domain.Lot;
import ru.milastoria.domain.LotImage;
import ru.milastoria.domain.LotVideo;

import java.util.List;

public interface LotMapper {

    List<Lot> findAll();

    List<Lot> findFeatured(@Param("limit") int limit);

    Lot findBySlug(@Param("slug") String slug);

    Lot findById(@Param("id") long id);

    /** Любой из фильтров может быть null — тогда он просто не участвует в WHERE (см. XML). */
    List<Lot> findPublished(@Param("categorySlug") String categorySlug,
                             @Param("tag") String tag,
                             @Param("query") String query);

    List<Lot> findRelated(@Param("categoryId") long categoryId,
                           @Param("excludeId") long excludeId,
                           @Param("limit") int limit);

    int countPublishedByCategory(@Param("categoryId") long categoryId);

    List<LotImage> findImagesByLotId(@Param("lotId") long lotId);

    LotImage findImageById(@Param("id") long id);

    void insertImage(LotImage image);

    void deleteImageById(@Param("id") long id);

    List<LotVideo> findVideosByLotId(@Param("lotId") long lotId);

    List<String> findTagNamesByLotId(@Param("lotId") long lotId);
}
