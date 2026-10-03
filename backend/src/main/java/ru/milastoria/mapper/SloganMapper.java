package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;
import ru.milastoria.domain.Slogan;

import java.util.List;

public interface SloganMapper {

    List<Slogan> findAll();

    List<Slogan> findEnabled();

    Slogan findById(@Param("id") long id);

    /** Случайный включённый слоган; null, если список пуст. */
    String findRandomEnabled();

    int insert(@Param("text") String text);

    void updateText(@Param("id") long id, @Param("text") String text);

    void setEnabled(@Param("id") long id, @Param("enabled") int enabled);

    void deleteById(@Param("id") long id);
}
