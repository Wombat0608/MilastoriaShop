package ru.milastoria.view;

import java.util.List;

/** Дашборд /admin/analytics. */
public record AnalyticsView(
        int uniqueToday,
        int viewsToday,
        int unique7,
        int views7,
        int unique30,
        int views30,
        int uniqueAll,
        int viewsAll,
        int sessions7,
        double pagesPerSession7,
        int direct7,
        List<PathStat> topPaths,
        List<PathStat> entryPaths,
        List<PathStat> referrers,
        List<DailyStat> daily,
        List<SessionPath> recentSessions,
        int periodDays,
        String error,
        String notice
) {

    /** Один визит: сессия + порядок страниц. */
    public record SessionPath(String sessionId,
                              String firstPath,
                              String lastPath,
                              String startedAt,
                              List<String> paths) {
    }
}
