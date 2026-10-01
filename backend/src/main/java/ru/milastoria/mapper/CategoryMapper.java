package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;
import ru.milastoria.domain.Category;

import java.util.List;

public interface CategoryMapper {

    List<Category> findAll();

    Category findBySlug(@Param("slug") String slug);

    Category findById(@Param("id") long id);

    boolean existsSlug(@Param("slug") String slug);

    int insert(Category category);

    int update(Category category);

    void updateSort(@Param("id") long id, @Param("sort") int sort);

    int maxSort();
}
