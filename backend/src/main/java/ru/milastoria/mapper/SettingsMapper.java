package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;

/** Настройки key→value (watermark, about и пр.). */
public interface SettingsMapper {

    String get(@Param("key") String key);

    void put(@Param("key") String key, @Param("value") String value);
}
