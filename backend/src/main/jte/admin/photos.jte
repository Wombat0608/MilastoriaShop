@import ru.milastoria.view.AdminPhotosView
@import ru.milastoria.domain.LotImage

@param AdminPhotosView view

@template.admin.layout(
    title = view.lot().getTitle(),
    content = @`
        <nav class="breadcrumb" aria-label="Навигация">
            <a href="/admin">Лоты</a>
            <span aria-hidden="true">/</span>
            <a href="/admin/lots/${view.lot().getId()}/edit">${view.lot().getTitle()}</a>
            <span aria-hidden="true">/</span>
            <span>Фото</span>
        </nav>
        <div class="section-head admin-head">
            <h1>${view.lot().getTitle()}</h1>
            <div class="admin-head__actions">
                <a class="btn btn--line" href="/admin/media?lotId=${view.lot().getId()}">Из медиатеки</a>
                <a class="btn btn--line" href="/admin/lots/${view.lot().getId()}/edit">Вернуться к лоту</a>
                <a class="btn btn--line" href="/work/${view.lot().getSlug()}" target="_blank" rel="noopener">Сайт</a>
            </div>
        </div>

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif

        <p class="admin-hint" style="margin-bottom:0.75rem">
            Порядок: перетащите фото. Watermark: на кропе — тяните и растягивайте рамку
            (размер и позиция сохраняются у фото).
            <a href="/admin/watermark">Настройки watermark</a>.
            Лупа в углу карточки — полноразмерный просмотр.
        </p>

        <div class="admin-photo-grid" id="photo-grid" data-lot-id="${view.lot().getId()}">
            @for(LotImage img : view.images())
                <figure class="admin-photo-card" data-image-id="${img.getId()}" draggable="true">
                    <div class="admin-photo-card__handle" aria-hidden="true">⋮⋮</div>
                    <img src="${img.getPathThumb()}" alt="${img.getAlt() != null ? img.getAlt() : view.lot().getTitle()}">
                    <form method="post" action="/admin/lots/${view.lot().getId()}/photos/${img.getId()}/delete"
                          data-confirm="Удалить фото?">
                        <button type="submit" class="admin-photo-delete" aria-label="Удалить фото">×</button>
                    </form>
                    <button type="button"
                            class="admin-photo-view"
                            data-full="${img.getPathFull() != null && !img.getPathFull().isBlank() ? img.getPathFull() : img.getPathThumb()}"
                            data-alt="${img.getAlt() != null && !img.getAlt().isBlank() ? img.getAlt() : view.lot().getTitle()}"
                            aria-label="Просмотр фото" title="Просмотр">
                        <svg class="admin-photo-view__icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" focusable="false">
                            <path fill="currentColor" d="M10.5 3a7.5 7.5 0 1 0 4.55 13.46l4.24 4.24 1.41-1.41-4.24-4.24A7.5 7.5 0 0 0 10.5 3zm0 2a5.5 5.5 0 1 1 0 11 5.5 5.5 0 0 1 0-11z"/>
                        </svg>
                    </button>
                </figure>
            @endfor
        </div>

        <form method="post" action="/admin/lots/${view.lot().getId()}/photos" enctype="multipart/form-data" id="upload-form">
            <input type="hidden" name="x" id="crop-x">
            <input type="hidden" name="y" id="crop-y">
            <input type="hidden" name="width" id="crop-w">
            <input type="hidden" name="height" id="crop-h">
            <input type="hidden" name="wm_x" id="wm-x">
            <input type="hidden" name="wm_y" id="wm-y">
            <input type="hidden" name="wm_width" id="wm-width">
            <input type="hidden" name="wm_opacity" id="wm-opacity-hidden" value="1">

            <label class="upload-picker" id="upload-picker">
                <span>+ Добавить фото</span>
                <input type="file" name="photo" accept="image/*" id="file-input" required>
            </label>
        </form>

        <div class="crop-stage" id="crop-stage" hidden
             data-wm-width="${view.defaultWmWidth()}"
             data-wm-margin="${view.defaultWmMargin()}">
            <div class="crop-stage__image-wrap" id="crop-wrap">
                <img id="crop-image" alt="">
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
                <button type="button" class="btn btn--line" id="crop-cancel">Отмена</button>
                <button type="button" class="btn btn--fill" id="crop-confirm">Готово</button>
            </div>
        </div>

        <div class="lightbox admin-lightbox" id="admin-photo-view" role="dialog" aria-modal="true" aria-label="Просмотр фото" hidden>
            <button type="button" class="lightbox__close" id="pv-close" aria-label="Закрыть">×</button>
            <button type="button" class="lightbox__nav lightbox__nav--prev" id="pv-prev" aria-label="Предыдущее фото">‹</button>
            <img id="pv-img" alt="">
            <button type="button" class="lightbox__nav lightbox__nav--next" id="pv-next" aria-label="Следующее фото">›</button>
            <div class="lightbox__caption" id="pv-caption"></div>
        </div>

        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.css">
        <script src="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/sortablejs@1.15.3/Sortable.min.js"></script>
        <script src="/js/admin-upload.js"></script>
        <script src="/js/admin-crop-wm.js"></script>
        <script src="/js/admin-reorder.js"></script>
        <script src="/js/admin-photo-view.js"></script>
    `
)
