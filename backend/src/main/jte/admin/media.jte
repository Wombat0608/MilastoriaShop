@import ru.milastoria.view.MediaLibraryView
@import ru.milastoria.domain.MediaFile

@param MediaLibraryView view

@template.admin.layout(
    title = "Медиатека",
    content = @`
        <style>.admin-main{max-width:1100px}</style>
        <div class="section-head admin-head">
            <h1>Медиатека</h1>
            <div class="admin-head__actions">
                <a class="btn btn--line" href="/admin/media/lots">Выбрать лот</a>
                <a class="btn btn--line" href="/admin">Лоты</a>
            </div>
        </div>

        @if(view.targetingLot())
            <p class="admin-notice">
                Прикрепление к лоту: <strong>${view.lotTitle()}</strong> (#${view.lotId()})
                — отметьте файлы и нажмите «Прикрепить выбранные».
            </p>
        @else
            <p class="admin-hint" style="margin-bottom:0.75rem">
                Отметьте нужные файлы checkbox'ами и прикрепите к лоту:
                можно выбрать сразу несколько.
                Для точного кропа откройте один файл кнопкой «Кроп / watermark».
            </p>
        @endif

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif
        @if(view.notice() != null)
            <p class="admin-notice">${view.notice()}</p>
        @endif

        <p class="admin-hint" style="margin-bottom:0.75rem">
            Загрузка фото и видео с телефона (можно пачкой). Watermark при выгрузке в медиатеку не ставится.
            Кадрирование — по желанию, кнопка «Пропустить» быстро пропускает шаг.
            Дата съёмки берётся из EXIF (или из имени файла). Список отсортирован по EXIF-дате.
        </p>

        <label class="upload-picker" id="media-upload-picker">
            <span>+ Добавить фото / видео из галереи</span>
            <input type="file" name="files" id="media-files" accept="image/*,video/*" multiple>
        </label>
        <div class="admin-meta" id="media-upload-status" style="margin-top:0.75rem" hidden></div>

        <form class="admin-search" method="get" action="/admin/media" style="margin-top:1.25rem">
            <input type="hidden" name="lotId" value="${view.lotId() != null ? String.valueOf(view.lotId()) : ""}">
            <div class="admin-search__row">
                <label for="media-q">Поиск</label>
                <input id="media-q" name="q" type="search"
                       placeholder="имя файла, дата EXIF, камера…"
                       value="${view.query() != null ? view.query() : ""}">
            </div>
            <div class="admin-cat-filter" style="margin-top:0.35rem">
                <label class="admin-cat-check">
                    @if(view.kind() == null)
                        <input type="radio" name="kind" value="" checked>
                    @else
                        <input type="radio" name="kind" value="">
                    @endif
                    <span>Все</span>
                </label>
                <label class="admin-cat-check">
                    @if(view.kind() != null && view.kind().equals("image"))
                        <input type="radio" name="kind" value="image" checked>
                    @else
                        <input type="radio" name="kind" value="image">
                    @endif
                    <span>Фото</span>
                </label>
                <label class="admin-cat-check">
                    @if(view.kind() != null && view.kind().equals("video"))
                        <input type="radio" name="kind" value="video" checked>
                    @else
                        <input type="radio" name="kind" value="video">
                    @endif
                    <span>Видео</span>
                </label>
            </div>
        </form>

        <div class="media-bulk-bar" id="media-bulk-bar" hidden>
            <div class="media-bulk-bar__info">
                <label class="admin-cat-check">
                    <input type="checkbox" id="media-select-all">
                    <span>Все на странице</span>
                </label>
                <span class="media-bulk-bar__count" id="media-bulk-count">0 выбрано</span>
            </div>
            <div class="media-bulk-bar__actions">
                @if(view.targetingLot())
                    <form method="post" action="/admin/media/attach-batch" id="media-batch-form"
                          data-loading-message="Прикрепляю файлы к лоту…">
                        <input type="hidden" name="lot_id" value="${view.lotId()}">
                        <button type="submit" class="btn btn--fill" id="media-batch-submit"
                                data-confirm="Прикрепить выбранные файлы к лоту? Файлы уйдут из медиатеки в папку лота.">
                            Прикрепить выбранные к лоту
                        </button>
                    </form>
                @else
                    <form method="get" action="/admin/media/lots" id="media-batch-form" data-no-loading>
                        <input type="hidden" name="mediaIds" id="media-batch-ids" value="">
                        <button type="submit" class="btn btn--fill" id="media-batch-submit">
                            Прикрепить выбранные к лоту…
                        </button>
                    </form>
                @endif
                <button type="button" class="btn btn--line" id="media-select-none">Снять выбор</button>
            </div>
        </div>

        <div class="admin-meta" style="margin:0.5rem 0 0.75rem">
            Файлов: ${view.total()} · стр. ${view.page()} из ${view.totalPages()}
        </div>

        @if(view.files().isEmpty())
            <div class="admin-empty">
                @if(view.searching())
                    Ничего не найдено по запросу «${view.query()}»
                @else
                    Медиатека пуста — загрузите фото или видео с телефона.
                @endif
            </div>
        @else
            <div class="media-grid" id="media-grid"
                 data-lot-id="${view.lotId() != null ? String.valueOf(view.lotId()) : ""}">
                @for(MediaFile f : view.files())
                    <article class="media-card media-card--selectable" data-media-id="${f.getId()}" data-kind="${f.getKind()}">
                        <div class="media-card__preview">
                            <label class="media-card__check" title="Выбрать файл">
                                <input type="checkbox" class="media-check" value="${f.getId()}"
                                       aria-label="Выбрать ${f.getOriginalName()}">
                            </label>
                            @if(f.isVideo())
                                <div class="media-card__video-badge">видео</div>
                                @if(f.getThumbPath() != null && !f.getThumbPath().isBlank())
                                    <img src="${f.getThumbPath()}" alt="${f.getOriginalName()}" loading="lazy">
                                @else
                                    <div class="media-card__placeholder" aria-hidden="true">▶</div>
                                @endif
                            @else
                                @if(f.getThumbPath() != null && !f.getThumbPath().isBlank())
                                    <img src="${f.getThumbPath()}" alt="${f.getOriginalName()}" loading="lazy">
                                @else
                                    <img src="${f.getPath()}" alt="${f.getOriginalName()}" loading="lazy">
                                @endif
                            @endif
                        </div>
                        <div class="media-card__body">
                            <div class="media-card__name" title="${f.getOriginalName()}">${f.getOriginalName()}</div>
                            <div class="media-card__meta">
                                <span>Съёмка: <strong>${f.getExifDatetime() != null ? f.getExifDatetime() : "—"}</strong></span>
                                <span>Загружено: ${f.getUploadedAt()}</span>
                                @if(f.getExifMake() != null || f.getExifModel() != null)
                                    <span>${f.getExifMake() != null ? f.getExifMake() : ""} ${f.getExifModel() != null ? f.getExifModel() : ""}</span>
                                @endif
                                <span>${view.kindLabel(f.getKind())}</span>
                            </div>
                            <div class="media-card__actions">
                                <a class="btn btn--line" href="${view.attachHref(f.getId())}">Кроп / watermark</a>
                                <form method="post" action="/admin/media/${f.getId()}/delete"
                                      data-confirm="Удалить файл из медиатеки?">
                                    <input type="hidden" name="back" value="/admin/media?${view.pageQuery(view.page())}">
                                    <button type="submit" class="btn btn--line">Удалить</button>
                                </form>
                            </div>
                        </div>
                    </article>
                @endfor
            </div>

            <nav class="media-pagination" aria-label="Страницы медиатеки">
                @if(view.hasPrev())
                    <a class="btn btn--line"
                       href="/admin/media?${view.pageQuery(view.prevPage())}">← Назад</a>
                @else
                    <span class="media-pagination__disabled">← Назад</span>
                @endif
                <span class="media-pagination__page">${view.page()} / ${view.totalPages()}</span>
                @if(view.hasNext())
                    <a class="btn btn--line"
                       href="/admin/media?${view.pageQuery(view.nextPage())}">Вперёд →</a>
                @else
                    <span class="media-pagination__disabled">Вперёд →</span>
                @endif
            </nav>
        @endif

        <div class="crop-stage" id="media-crop-stage" hidden>
            <div class="crop-stage__image-wrap" id="media-crop-wrap">
                <img id="media-crop-image" alt="">
            </div>
            <div class="crop-stage__actions">
                <button type="button" class="btn btn--line" id="media-crop-skip">Пропустить кроп</button>
                <button type="button" class="btn btn--fill" id="media-crop-confirm">Кропнуть и загрузить</button>
            </div>
        </div>

        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.css">
        <script src="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.js"></script>
        <script src="/js/admin-media-upload.js"></script>
        <script src="/js/admin-media-select.js"></script>
    `
)
