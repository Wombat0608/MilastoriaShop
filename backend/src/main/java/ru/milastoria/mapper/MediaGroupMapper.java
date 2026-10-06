package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;
import ru.milastoria.domain.MediaGroup;

public interface MediaGroupMapper {

    MediaGroup findById(@Param("id") long id);

    /** Создаёт группу с временным именем, возвращает id. */
    int insertTemporary(@Param("tempName") String tempName,
                        @Param("createdAt") String createdAt);

    void updateName(@Param("id") long id, @Param("name") String name);
}
