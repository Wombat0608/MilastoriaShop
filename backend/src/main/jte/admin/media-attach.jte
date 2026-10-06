@import ru.milastoria.view.MediaAttachView
@import ru.milastoria.view.AdminLotRow
@import ru.milastoria.domain.MediaFile

@param MediaAttachView view

@template.admin.layout(
    title = "Прикрепить к лоту",
    content = @`
        <nav class="breadcrumb" aria-label="Навигация">
            <a href="/admin">Лоты</a>
            <span aria-hidden="true">/</span>
            <a href="/admin/media">Медиатека</a>
            <span aria-hidden="true">/</span>
            <span>Прикрепить</span>
        </nav>

        <div class="section-head admin-head">
            <h1>Прикрепить к лоту</h1>
            <div class="admin-head__actions">
                <a class="btn btn--line" href="/admin/media">В медиатеку</a>
                <a class="btn btn--line" href="/admin/media/lots?mediaId=${view.media().getId()}">Выбрать лот</a>
            </div>
        </div>

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif
        @if(view.notice() != null)
            <p class="admin-notice">${view.notice()}</p>
        @endif

        @if(view.hasQueue())
            <p class="admin-hint">
                В очереди ещё файлов: <strong>${view.queueCsv()}</strong>
                — после прикрепления откроется следующий (кроп и watermark для каждого).
            </p>
        @endif

        <div class="media-attach-card">
            <div class="media-attach-card__preview">
                @if(view.media().isVideo())
                    <div class="media-card__video-badge">видео</div>
                    @if(view.media().getThumbPath() != null && !view.media().getThumbPath().isBlank())
                        <img src="${view.media().getThumbPath()}" alt="${view.media().getOriginalName()}">
                    @else
                        <div class="media-card__placeholder media-card__placeholder--lg" aria-hidden="true">▶</div>
                    @endif
                @else
                    <img src="${view.media().getThumbPath() != null && !view.media().getThumbPath().isBlank() ? view.media().getThumbPath() : view.media().getPath()}"
                         alt="${view.media().getOriginalName()}">
                @endif
            </div>
            <div class="media-attach-card__meta">
                <div><strong>${view.media().getOriginalName()}</strong></div>
                <div class="admin-meta" style="margin:0">
                    Съёмка: ${view.media().getExifDatetime() != null ? view.media().getExifDatetime() : "—"}
                    · Загружено: ${view.media().getUploadedAt()}
                    @if(view.media().getExifMake() != null || view.media().getExifModel() != null)
                        · ${view.media().getExifMake() != null ? view.media().getExifMake() : ""} ${view.media().getExifModel() != null ? view.media().getExifModel() : ""}
                    @endif
                </div>
                <p class="admin-hint">
                    @if(view.media().isVideo())
                        Видео прикрепится как есть (без кропа/watermark). Файл останется в медиатеке и будет помечен как прикреплённый.
                    @else
                        Для фото <strong>обязательны</strong> кроп и watermark (экран откроется сразу)
                        либо явная кнопка «Без кропа» — тогда файл будет прикреплён целиком с WM по умолчанию.
                        Файл останется в медиатеке с флагом «прикреплённый».
                    @endif
                </p>
            </div>
        </div>

        <form method="post" action="/admin/media/${view.media().getId()}/attach" id="attach-form" class="admin-form" style="margin-top:1.25rem"
              data-kind="${view.media().isVideo() ? "video" : "image"}"
              data-loading-message="Прикрепляю файл к лоту…">
            <div class="lot-picker" id="attach-lot-picker">
                <div class="lot-picker__head">
                    <label for="attach-lot-filter">Лот</label>
                    <input id="attach-lot-filter" type="search"
                           placeholder="поиск по заголовку / slug…"
                           autocomplete="off"
                           data-selected-lot-id="${view.selectedLotId() != null ? String.valueOf(view.selectedLotId()) : ""}"
                           data-selected-lot-title="${view.selectedLotTitle() != null ? view.selectedLotTitle() : ""}">
                </div>
                <input type="hidden" name="lot_id" id="attach-lot-id"
                       value="${view.selectedLotId() != null ? String.valueOf(view.selectedLotId()) : ""}">
                <input type="hidden" name="queue" value="${view.queueCsv()}">
                <div class="admin-meta" id="attach-lot-selected" style="margin:0.25rem 0 0.75rem">
                    @if(view.selectedLotId() != null && view.selectedLotTitle() != null)
                        Выбрано: <strong>${view.selectedLotTitle()}</strong> (#${view.selectedLotId()})
                    @else
                        Выберите лот в списке ниже
                    @endif
                </div>

                <div class="admin-lot-list" id="attach-lot-list" aria-label="Список лотов">
                    @for(AdminLotRow lot : view.lots())
                        <article class="admin-lot-row${view.selectedLotId() != null && view.selectedLotId() == lot.getId() ? " is-selected" : ""}"
                                 data-lot-id="${lot.getId()}"
                                 data-search="${lot.getTitle()} ${lot.getSlug()} #${lot.getId()}">
                            <span class="admin-lot-row__icon" aria-hidden="true">
                                @if(lot.getCoverThumb() != null && !lot.getCoverThumb().isBlank())
                                    <img src="${lot.getCoverThumb()}" alt="" loading="lazy">
                                @else
                                    <span class="admin-lot-row__placeholder">?</span>
                                @endif
                            </span>
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
                                <button type="button" class="btn btn--line lot-picker__pick" data-lot-id="${lot.getId()}">
                                    Выбрать
                                </button>
                            </div>
                        </article>
                    @endfor
                    @if(view.lots().isEmpty())
                        <div class="admin-empty">Лотов нет</div>
                    @endif
                </div>

                <p class="admin-hint" style="margin-top:0.75rem">
                    Нажмите «Выбрать» у нужного лота (или кликните строку). Поиск — фильтрует список мгновенно.
                    <a href="/admin/media/lots?mediaId=${view.media().getId()}">Открыть полный список лотов</a>.
                </p>
            </div>

            @if(view.media().isImage())
                <input type="hidden" name="x" id="crop-x">
                <input type="hidden" name="y" id="crop-y">
                <input type="hidden" name="width" id="crop-w">
                <input type="hidden" name="height" id="crop-h">
                <input type="hidden" name="wm_x" id="wm-x">
                <input type="hidden" name="wm_y" id="wm-y">
                <input type="hidden" name="wm_width" id="wm-width">
                <input type="hidden" name="wm_opacity" id="wm-opacity-hidden" value="1">
                <input type="hidden" name="no_crop" id="no_crop" value="">
            @endif

            <div class="admin-form__actions">
                @if(view.media().isImage())
                    <button type="button" class="btn btn--line" id="attach-crop-open">Кадрировать и watermark</button>
                    <button type="button" class="btn btn--line" id="attach-no-crop">Без кропа</button>
                    <button type="submit" class="btn btn--fill" id="attach-submit">Прикрепить</button>
                @else
                    <button type="submit" class="btn btn--fill">Прикрепить видео к лоту</button>
                @endif
            </div>
        </form>

        @if(view.media().isImage())
            <div class="crop-stage" id="crop-stage" hidden
                 data-wm-width="${view.defaultWmWidth()}"
                 data-wm-margin="${view.defaultWmMargin()}">
                <div class="crop-stage__image-wrap" id="crop-wrap">
                    <img id="crop-image" src="${view.media().getPath()}" alt="${view.media().getOriginalName()}">
                    <div class="crop-wm" id="crop-wm" hidden>
                        <img src="/admin/branding/watermark.png" alt="" aria-hidden="true">
                        <div class="crop-wm__handle" id="crop-wm-handle" aria-hidden="true"></div>
                    </div>
                </div>
                <div class="crop-stage__bar">
                    <label class="crop-stage__opacity">
                        <span>Прозрачность WM</span>
                        <input type="range" id="wm-opacity" min="10" max="100" value="100">
                        <span id="wm-opacity-val">100%</span>
                    </label>
                </div>
                <div class="crop-stage__actions">
                    <button type="button" class="btn btn--line" id="crop-cancel">Без кропа</button>
                    <button type="button" class="btn btn--fill" id="crop-confirm">Готово (кроп + WM)</button>
                </div>
            </div>

            <link rel="stylesheet" href="/css/cropper.min.css">
            <script src="/js/cropper.min.js"></script>
            <script src="/js/admin-crop-wm.js"></script>
            <script src="/js/admin-media-attach.js"></script>
        @endif
        <script src="/js/admin-lot-picker.js"></script>
    `
)
