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
                        Видео прикрепится к лоту как есть (без кропа/watermark). Файл уйдёт из медиатеки в папку лота.
                    @else
                        Для фото снова предложится кадрирование и наложение watermark. Оригинал из медиатеки после
                        прикрепления будет удалён физически — на лоте останутся thumb/full.
                    @endif
                </p>
            </div>
        </div>

        <form method="post" action="/admin/media/${view.media().getId()}/attach" id="attach-form" class="admin-form" style="margin-top:1.25rem"
              data-loading-message="Прикрепляю файл к лоту…">
            <div class="lot-picker" id="attach-lot-picker">
                <div class="lot-picker__head">
                    <label for="attach-lot-filter">Лот</label>
                    <input id="attach-lot-filter" type="search"
                           placeholder="поиск по заголовку / slug…"
                           autocomplete="off"
                           data-selected-lot-id="${view.selectedLotId() != null ? String.valueOf(view.selectedLotId()) : ""}"
                           data-selected-lot-title="${view.selectedLotTitle() != null ? view.selectedLotTitle() : ""}">
                    <span class="admin-hint lot-picker__selected" id="attach-lot-selected">
                        @if(view.selectedLotId() != null && view.selectedLotTitle() != null)
                            Выбрано: ${view.selectedLotTitle()} (#${view.selectedLotId()})
                        @else
                            Лот не выбран — найдите по названию
                        @endif
                    </span>
                </div>
                <input type="hidden" name="lot_id" id="attach-lot-id"
                       value="${view.selectedLotId() != null ? String.valueOf(view.selectedLotId()) : ""}">
                <div class="lot-picker__list" id="attach-lot-list" role="listbox" aria-label="Список лотов">
                    @for(AdminLotRow lot : view.lots())
                        <button type="button"
                                class="lot-picker__item${view.selectedLotId() != null && view.selectedLotId() == lot.getId() ? " is-selected" : ""}"
                                role="option"
                                aria-selected="${view.selectedLotId() != null && view.selectedLotId() == lot.getId() ? "true" : "false"}"
                                data-lot-id="${lot.getId()}"
                                data-search="${lot.getTitle()} ${lot.getSlug()} #${lot.getId()}">
                            <span class="lot-picker__title">${lot.getTitle()}</span>
                            <span class="lot-picker__meta">/${lot.getSlug()} · #${lot.getId()}
                                @if(lot.getCategoryTitle() != null && !lot.getCategoryTitle().isBlank())
                                    · ${lot.getCategoryTitle()}
                                @endif
                            </span>
                        </button>
                    @endfor
                    @if(view.lots().isEmpty())
                        <div class="admin-empty">Лотов нет</div>
                    @endif
                </div>
                <p class="admin-hint">
                    Список длинный? Введите фрагмент заголовка или slug — отфильтруется мгновенно.
                    <a href="/admin/media/lots?mediaId=${view.media().getId()}">Открыть отдельный экран поиска лота</a>.
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
            @endif

            <div class="admin-form__actions">
                @if(view.media().isImage())
                    <button type="button" class="btn btn--line" id="attach-crop-open">Кадрировать и watermark</button>
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
                    <button type="button" class="btn btn--line" id="crop-cancel">Без кропа / закрыть</button>
                    <button type="button" class="btn btn--fill" id="crop-confirm">Готово</button>
                </div>
            </div>

            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.css">
            <script src="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.js"></script>
            <script src="/js/admin-crop-wm.js"></script>
            <script src="/js/admin-media-attach.js"></script>
        @endif
        <script src="/js/admin-lot-picker.js"></script>
    `
)
