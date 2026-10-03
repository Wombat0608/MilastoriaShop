package ru.milastoria.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Аналитика посещений сайта (серверная, без внешних трекеров). */
public interface AnalyticsMapper {

    void insertVisit(@Param("visitorId") String visitorId,
                     @Param("sessionId") String sessionId,
                     @Param("path") String path,
                     @Param("referrer") String referrer,
                     @Param("userAgent") String userAgent,
                     @Param("createdAt") String createdAt);

    int countUniqueVisitors(@Param("since") String since);

    int countPageViews(@Param("since") String since);

    int countSessions(@Param("since") String since);

    List<ru.milastoria.view.PathStat> topPaths(@Param("since") String since,
                                               @Param("limit") int limit);

    List<ru.milastoria.view.PathStat> topEntryPaths(@Param("since") String since,
                                                    @Param("limit") int limit);

    List<ru.milastoria.view.PathStat> topReferrers(@Param("since") String since,
                                                   @Param("limit") int limit);

    int directEntryCount(@Param("since") String since);

    List<ru.milastoria.view.DailyStat> dailyStats(@Param("since") String since);

    List<String> recentSessionIds(@Param("limit") int limit);

    List<ru.milastoria.view.VisitRow> visitsBySessions(@Param("sessionIds") List<String> sessionIds);
}
