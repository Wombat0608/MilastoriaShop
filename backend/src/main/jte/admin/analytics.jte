@import ru.milastoria.view.AnalyticsView
@import ru.milastoria.view.PathStat
@import ru.milastoria.view.DailyStat

@param AnalyticsView view

@template.admin.layout(
    title = "Статистика",
    content = @`
        <div class="section-head admin-head">
            <h1>Статистика</h1>
            <div class="admin-head__actions">
                <a class="btn btn--line" href="/admin">Лоты</a>
                <a class="btn btn--line" href="/" target="_blank" rel="noopener">Сайт</a>
            </div>
        </div>

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif
        @if(view.notice() != null)
            <p class="admin-notice">${view.notice()}</p>
        @endif

        <section class="admin-block">
            <h2>Сводка</h2>
            <p class="admin-hint">
                Серверная аналитика (без внешних трекеров). Уникальные посетители —
                анонимные cookie, не IP. Боты и запросы админки не учитываются.
            </p>

            <div class="stats-periods" role="navigation" aria-label="Период">
                <a href="/admin/analytics?days=1" class="filter-btn ${view.periodDays() == 1 ? "is-active" : ""}">Сегодня</a>
                <a href="/admin/analytics?days=7" class="filter-btn ${view.periodDays() == 7 ? "is-active" : ""}">7 дней</a>
                <a href="/admin/analytics?days=30" class="filter-btn ${view.periodDays() == 30 ? "is-active" : ""}">30 дней</a>
                <a href="/admin/analytics?days=90" class="filter-btn ${view.periodDays() == 90 ? "is-active" : ""}">90 дней</a>
            </div>

            <div class="stats-grid">
                <div class="stats-card">
                    <span class="stats-card__label">Сегодня</span>
                    <strong>${view.uniqueToday()}</strong>
                    <span class="stats-card__sub">уникальных · ${view.viewsToday()} просмотров</span>
                </div>
                <div class="stats-card">
                    <span class="stats-card__label">7 дней</span>
                    <strong>${view.unique7()}</strong>
                    <span class="stats-card__sub">уникальных · ${view.views7()} просмотров</span>
                </div>
                <div class="stats-card">
                    <span class="stats-card__label">30 дней</span>
                    <strong>${view.unique30()}</strong>
                    <span class="stats-card__sub">уникальных · ${view.views30()} просмотров</span>
                </div>
                <div class="stats-card">
                    <span class="stats-card__label">Всего</span>
                    <strong>${view.uniqueAll()}</strong>
                    <span class="stats-card__sub">уникальных · ${view.viewsAll()} просмотров</span>
                </div>
            </div>

            <div class="stats-grid" style="margin-top:1rem">
                <div class="stats-card stats-card--soft">
                    <span class="stats-card__label">Сессии, 7 дней</span>
                    <strong>${view.sessions7()}</strong>
                    <span class="stats-card__sub">${view.pagesPerSession7()} страниц в среднем</span>
                </div>
                <div class="stats-card stats-card--soft">
                    <span class="stats-card__label">Прямые заходы, 7 дней</span>
                    <strong>${view.direct7()}</strong>
                    <span class="stats-card__sub">без источника в URL</span>
                </div>
            </div>
        </section>

        <section class="admin-block" style="margin-top:2rem">
            <h2>Динамика (14 дней)</h2>
            @if(view.daily().isEmpty())
                <div class="admin-empty">Пока нет данных — посетите сайт, чтобы собрать статистику.</div>
            @else
                <div class="stats-trend">
                    @for(DailyStat d : view.daily())
                        <div class="stats-trend__row" title="${d.day()}: ${d.visitors()} уникальных, ${d.views()} просмотров">
                            <span class="stats-trend__day">${d.day().substring(5)}</span>
                            <div class="stats-trend__bars">
                                <span class="stats-trend__bar stats-trend__bar--u" style="width:${Math.max(4, Math.min(100, d.visitors() * 8))}%"></span>
                                <span class="stats-trend__bar stats-trend__bar--v" style="width:${Math.max(4, Math.min(100, d.views() * 4))}%"></span>
                            </div>
                            <span class="stats-trend__nums">${d.visitors()} / ${d.views()}</span>
                        </div>
                    @endfor
                </div>
                <p class="admin-hint" style="margin-top:0.75rem">
                    <span class="stats-legend stats-legend--u">уникальные</span>
                    <span class="stats-legend stats-legend--v">просмотры</span>
                </p>
            @endif
        </section>

        <section class="admin-block" style="margin-top:2rem">
            <h2>Страницы · период ${view.periodDays()} дн.</h2>
            @if(view.topPaths().isEmpty())
                <div class="admin-empty">Нет просмотров за период.</div>
            @else
                <div class="admin-lot-list">
                    @for(PathStat p : view.topPaths())
                        <div class="admin-lot-row admin-lot-row--stat">
                            <div class="admin-lot-row__body grow">
                                <code class="stats-path">${p.path()}</code>
                            </div>
                            <div class="admin-lot-row__meta">
                                <span class="admin-lot-row__id">${p.cnt()}×</span>
                            </div>
                        </div>
                    @endfor
                </div>
            @endif
        </section>

        <section class="admin-block" style="margin-top:2rem">
            <h2>Вход на сайт (первые страницы)</h2>
            @if(view.entryPaths().isEmpty())
                <div class="admin-empty">Нет данных.</div>
            @else
                <div class="admin-lot-list">
                    @for(PathStat p : view.entryPaths())
                        <div class="admin-lot-row admin-lot-row--stat">
                            <div class="admin-lot-row__body grow">
                                <code class="stats-path">${p.path()}</code>
                            </div>
                            <div class="admin-lot-row__meta">
                                <span class="admin-lot-row__id">${p.cnt()}×</span>
                            </div>
                        </div>
                    @endfor
                </div>
            @endif
        </section>

        <section class="admin-block" style="margin-top:2rem">
            <h2>Источники переходов</h2>
            @if(view.referrers().isEmpty())
                <div class="admin-empty">Пока только прямые заходы или нет данных.</div>
            @else
                <div class="admin-lot-list">
                    @for(PathStat p : view.referrers())
                        <div class="admin-lot-row admin-lot-row--stat">
                            <div class="admin-lot-row__body grow">
                                <span class="stats-path stats-path--text">${p.path()}</span>
                            </div>
                            <div class="admin-lot-row__meta">
                                <span class="admin-lot-row__id">${p.cnt()}×</span>
                            </div>
                        </div>
                    @endfor
                </div>
            @endif
        </section>

        <section class="admin-block" style="margin-top:2rem">
            <h2>Маршруты посетителей</h2>
            <p class="admin-hint">Последние 15 сессий: порядок страниц, как человек шёл по сайту.</p>
            @if(view.recentSessions().isEmpty())
                <div class="admin-empty">Маршрутов пока нет.</div>
            @else
                <div class="admin-session-list">
                    @for(var s : view.recentSessions())
                        <article class="admin-session">
                            <div class="admin-session__head">
                                <span class="admin-session__id">${s.sessionId().substring(0, 8)}…</span>
                                <span class="admin-session__time">${s.startedAt()}</span>
                                <span class="admin-session__count">${s.paths().size()} стр.</span>
                            </div>
                            <div class="admin-session__path">
                                @for(int i = 0; i < s.paths().size(); i++)
                                    <code class="stats-path">${s.paths().get(i)}</code>
                                    @if(i < s.paths().size() - 1)
                                        <span class="admin-session__arrow" aria-hidden="true">→</span>
                                    @endif
                                @endfor
                            </div>
                        </article>
                    @endfor
                </div>
            @endif
        </section>
    `
)
