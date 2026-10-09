@import ru.milastoria.view.MediaLotPickerView
@import ru.milastoria.view.AdminLotRow
@import ru.milastoria.view.TextUtil

@param MediaLotPickerView view

@template.admin.layout(
    title = "Выбрать лот",
    content = @`
        <nav class="breadcrumb" aria-label="Навигация">
            <a href="/admin">Лоты</a>
            <span aria-hidden="true">/</span>
            <a href="/admin/media">Медиатека</a>
            <span aria-hidden="true">/</span>
            <span>Выбрать лот</span>
        </nav>

        <div class="section-head admin-head">
            <h1>@if(view.attachingBatch())
                Прикрепить к лоту (${view.mediaIds().size()} файл(ов))
            @elseif(view.mediaId() != null)
                Куда прикрепить файл #${view.mediaId()}
            @else
                Выберите лот
            @endif</h1>
            <div class="admin-head__actions">
                <a class="btn btn--line" href="/admin/media">В медиатеку</a>
                <a class="btn btn--line" href="/admin">Лоты</a>
            </div>
        </div>

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif
        @if(view.notice() != null)
            <p class="admin-notice">${view.notice()}</p>
        @endif

        @if(view.attachingBatch())
            <p class="admin-hint">
                Выбрано файлов: <strong>${view.mediaIds().size()}</strong>
                — нажмите «Прикрепить сюда» у нужного лота.
                Для каждого фото откроется кроп и watermark (по очереди).
            @elseif(view.mediaId() != null)
                Выберите лот — откроется форма кропа и watermark для этого файла.
            @else
                Поиск по заголовку и slug. Список по ${view.pageSize()} работ на странице.
            @endif</p>

        <form class="admin-search" method="get" action="/admin/media/lots" id="media-lot-search">
            <input type="hidden" name="mediaIds" value="${view.mediaIdsCsv()}">
            @if(view.mediaId() != null)
                <input type="hidden" name="mediaId" value="${view.mediaId()}">
            @endif
            <div class="admin-search__row">
                <label for="lot-q">Поиск лота</label>
                <input id="lot-q" name="q" type="search"
                       placeholder="например: золото, gold, family…"
                       value="${view.query() != null ? view.query() : ""}">
            </div>
            <div class="admin-meta" style="margin-top:0.4rem">
                Найдено: ${view.total()} · стр. ${view.page()} из ${view.totalPages()}
            </div>
        </form>

        @if(view.lots().isEmpty())
            <div class="admin-empty">
                @if(view.searching())
                    Ничего не найдено по запросу «${view.query()}»
                @else
                    Лотов пока нет.
                @endif
            </div>
        @else
            <div class="admin-lot-list">
                @for(AdminLotRow lot : view.lots())
                    <article class="admin-lot-row">
                        <a class="admin-lot-row__icon" href="/admin/lots/${lot.getId()}/edit" aria-label="Открыть ${lot.getTitle()}">
                            @if(lot.getCoverThumb() != null && !lot.getCoverThumb().isBlank())
                                <img src="${lot.getCoverThumb()}" alt="" loading="lazy">
                            @else
                                <span class="admin-lot-row__placeholder" aria-hidden="true">?</span>
                            @endif
                        </a>
                        <div class="admin-lot-row__body grow">
                            <span class="admin-lot-row__title">${lot.getTitle()}</span>
                            <div class="admin-lot-row__meta">
                                <code class="admin-lot-row__slug">/${lot.getSlug()}</code>
                                <span class="admin-lot-row__id">#${lot.getId()}</span>
                                @if(lot.getCategoryTitle() != null && !lot.getCategoryTitle().isBlank())
                                    <span class="admin-lot-row__cat">${lot.getCategoryTitle()}</span>
                                @endif
                                <span class="admin-lot-row__date">${lot.createdAtFormatted()}</span>
                                <span class="admin-status ${lot.published() ? "is-published" : ""}">${lot.getStatus()}</span>
                            </div>
                        </div>
                        <div class="admin-lot-row__actions">
                            @if(view.attachingBatch())
                                <form method="post" action="/admin/media/lots/${lot.getId()}/attach-selected" class="media-pick-form"
                                    data-loading-message="Прикрепляю к «${lot.getTitle()}»…">
                                    @for(Long mediaId : view.mediaIds())
                                        <input type="hidden" name="media_ids" value="${mediaId}">
                                    @endfor
                                    <button type="submit" class="btn btn--fill">Прикрепить сюда</button>
                                </form>
                            @elseif(view.mediaId() != null)
                                <a class="btn btn--fill" href="/admin/media/${view.mediaId()}/attach?lotId=${lot.getId()}">Выбрать</a>
                            @else
                                <a class="btn btn--line" href="/admin/media?lotId=${lot.getId()}">Медиатека к лоту</a>
                            @endif
                            <a class="btn btn--line" href="/admin/lots/${lot.getId()}/edit">Правка</a>
                        </div>
                    </article>
                @endfor
            </div>

            @if(view.totalPages() > 1)
                <nav class="media-pagination" aria-label="Страницы лотов">
                    @if(view.page() > 1)
                        <a class="btn btn--line" href="/admin/media/lots?${view.pageQuery(view.page() - 1)}">← Назад</a>
                    @else
                        <span class="media-pagination__disabled">← Назад</span>
                    @endif
                    <span class="media-pagination__page">${view.page()} / ${view.totalPages()}</span>
                    @if(view.page() < view.totalPages())
                        <a class="btn btn--line" href="/admin/media/lots?${view.pageQuery(view.page() + 1)}">Вперёд →</a>
                    @else
                        <span class="media-pagination__disabled">Вперёд →</span>
                    @endif
                </nav>
            @endif
        @endif
    `
)
