package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Словари: повод / материалы / теги (dict_values + lot_dict). */
public interface DictMapper {

    List<String> findNamesByKind(@Param("kind") String kind);

    List<String> findNamesByLot(@Param("lotId") long lotId, @Param("kind") String kind);

    void upsertValue(@Param("kind") String kind, @Param("name") String name);

    Long findId(@Param("kind") String kind, @Param("name") String name);

    void replaceLotDict(@Param("lotId") long lotId, @Param("kind") String kind);

    void linkLotDict(@Param("lotId") long lotId, @Param("dictId") long dictId);

    /** Миграция: если dict_values пуст — переносим tags и свободный текст lots. */
    boolean isDictEmpty();

    void seedTagsFromLegacy();

    void seedOccasionsFromLegacy();

    void seedFabricsFromLegacy();

    void seedLinksFromTags();

    void seedLinksFromOccasions();

    void seedLinksFromFabrics();
}
