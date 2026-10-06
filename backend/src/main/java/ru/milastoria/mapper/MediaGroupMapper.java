package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;
import ru.milastoria.domain.MediaGroup;

public interface MediaGroupMapper {

    MediaGroup findById(@Param("id") long id);

    /**
     * Вставляет группу и возвращает id через useGeneratedKeys.
     * Объект обязателен: keyProperty="id" не работает с @Param-примитивами
     * (BindingException / ExecutorException «Could not determine which parameter»).
     */
    int insertTemporary(MediaGroup group);

    void updateName(@Param("id") long id, @Param("name") String name);
}
