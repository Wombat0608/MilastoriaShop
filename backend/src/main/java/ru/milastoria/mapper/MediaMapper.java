package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;
import ru.milastoria.domain.MediaFile;

import java.util.List;

public interface MediaMapper {

    /** Страница списка: сортировка EXIF-дата DESC (fallback uploaded_at). */
    List<MediaFile> findPage(@Param("kind") String kind,
                             @Param("q") String q,
                             @Param("limit") int limit,
                             @Param("offset") int offset);

    int countPage(@Param("kind") String kind, @Param("q") String q);

    MediaFile findById(@Param("id") long id);

    int insert(MediaFile file);

    void deleteById(@Param("id") long id);
}
