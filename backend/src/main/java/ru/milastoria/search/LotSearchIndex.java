package ru.milastoria.search;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.milastoria.domain.Lot;
import ru.milastoria.mapper.LotMapper;

import java.util.List;

/**
 * Синхронизация FTS5-таблицы lots_fts с таблицей lots.
 * Единственное место, где индекс пишется: insert/update лота из админки
 * и полный rebuild на старте приложения (на 50–200 лотах пересборка
 * незаметна по времени и гарантирует, что после краша/ручного SQL
 * индекс не разъехался с данными).
 */
public final class LotSearchIndex {

    private static final Logger log = LoggerFactory.getLogger(LotSearchIndex.class);

    private LotSearchIndex() {
    }

    /** Пересобирает строку индекса по лоту и его тегам (вызывать в открытом session, commit — у вызывающего). */
    public static void indexLot(SqlSession session, Lot lot, List<String> tagNames) {
        LotMapper mapper = session.getMapper(LotMapper.class);
        mapper.deleteFromFts(lot.getId());
        String tags = tagNames == null ? "" : String.join(", ", tagNames);
        mapper.insertIntoFts(
                lot.getId(),
                nullSafe(lot.getTitle()),
                nullSafe(lot.getAnnotation()),
                nullSafe(lot.getSeoText()),
                nullSafe(lot.getMetaTitle()),
                nullSafe(lot.getMetaDescription()),
                tags
        );
    }

    public static void removeLot(SqlSession session, long lotId) {
        session.getMapper(LotMapper.class).deleteFromFts(lotId);
    }

    /** Полная пересборка индекса из lots + lot_tags. Безопасно вызывать при каждом старте. */
    public static void rebuildAll(SqlSession session) {
        LotMapper mapper = session.getMapper(LotMapper.class);
        mapper.clearFts();
        List<Lot> lots = mapper.findAllForIndex();
        for (Lot lot : lots) {
            indexLot(session, lot, mapper.findTagNamesByLotId(lot.getId()));
        }
        log.info("Индекс lots_fts пересобран: {} лотов", lots.size());
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
