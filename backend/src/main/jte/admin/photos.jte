@import ru.milastoria.view.AdminPhotosView
@import ru.milastoria.domain.LotImage

@param AdminPhotosView view

@template.admin.layout(
    title = view.lot().getTitle(),
    content = @`
        <nav class="breadcrumb" aria-label="Навигация">
            <a href="/admin">Лоты</a>
            <span aria-hidden="true">/</span>
            <span>${view.lot().getTitle()}</span>
        </nav>
        <h1>${view.lot().getTitle()}</h1>

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif

        <p class="admin-hint" style="margin-bottom:0.75rem">
            Порядок: перетащите фото. Watermark: на кропе — тяните и растягивайте рамку
            (размер и позиция сохраняются у фото).
            <a href="/admin/watermark">Настройки watermark</a>
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

        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.css">
        <script src="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/sortablejs@1.15.3/Sortable.min.js"></script>
        <script src="/js/admin-upload.js"></script>
        <script src="/js/admin-crop-wm.js"></script>
        <script src="/js/admin-reorder.js"></script>
    `
)
