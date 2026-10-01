package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;
import ru.milastoria.domain.Category;

import java.util.List;

public interface CategoryMapper {

    List<Category> findAll();

    Category findBySlug(@Param("slug") String slug);
}
