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

        <div class="admin-photo-grid">
            @for(LotImage img : view.images())
                <figure>
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

            <label class="upload-picker" id="upload-picker">
                <span>+ Добавить фото</span>
                <input type="file" name="photo" accept="image/*" id="file-input" required>
            </label>
        </form>

        <div class="crop-stage" id="crop-stage" hidden>
            <div class="crop-stage__image-wrap">
                <img id="crop-image" alt="">
            </div>
            <div class="crop-stage__actions">
                <button type="button" class="btn btn--line" id="crop-cancel">Отмена</button>
                <button type="button" class="btn btn--fill" id="crop-confirm">Готово</button>
            </div>
        </div>

        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.css">
        <script src="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.js"></script>
        <script src="/js/admin-upload.js"></script>
    `
)
