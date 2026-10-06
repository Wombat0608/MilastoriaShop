package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;
import ru.milastoria.domain.HeroSlide;

import java.util.List;

public interface HeroSlideMapper {

    List<HeroSlide> findAll();

    HeroSlide findById(@Param("id") long id);

    int insert(HeroSlide slide);

    void deleteById(@Param("id") long id);

    void updateSort(@Param("id") long id, @Param("sort") int sort);

    int countAll();
}
