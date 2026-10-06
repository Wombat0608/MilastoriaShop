package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;
import ru.milastoria.domain.MediaFile;

import java.util.List;

public interface MediaMapper {

    /**
     * Страница списка. Сортировка: группа (новые пачки вверху),
     * внутри — EXIF/загрузка DESC.
     * applied: "0"/null — свободные (по умолчанию), "1" — прикреплённые, "all" — все.
     */
    List<MediaFile> findPage(@Param("kind") String kind,
                             @Param("q") String q,
                             @Param("applied") String applied,
                             @Param("limit") int limit,
                             @Param("offset") int offset);

    int countPage(@Param("kind") String kind, @Param("q") String q,
                  @Param("applied") String applied);

    MediaFile findById(@Param("id") long id);

    int insert(MediaFile file);

    /** Ставит applied=1; 0 = уже прикреплён (claim). */
    int markApplied(@Param("id") long id);

    /** Снимает флаг applied (например, если attach не прошёл). */
    int unmarkApplied(@Param("id") long id);

    /** Удаляет строку; 0 = уже забрано. */
    int deleteById(@Param("id") long id);
}
