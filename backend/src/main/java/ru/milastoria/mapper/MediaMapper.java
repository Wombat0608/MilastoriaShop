package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;
import ru.milastoria.domain.MediaFile;

import java.util.List;

public interface MediaMapper {

    /**
     * Страница списка. Сортировка: группа (новые пачки вверху),
     * внутри — EXIF/загрузка DESC.
     */
    List<MediaFile> findPage(@Param("kind") String kind,
                             @Param("q") String q,
                             @Param("limit") int limit,
                             @Param("offset") int offset);

    int countPage(@Param("kind") String kind, @Param("q") String q);

    MediaFile findById(@Param("id") long id);

    int insert(MediaFile file);

    /** Удаляет строку; 0 = уже забрано. */
    int deleteById(@Param("id") long id);
}
