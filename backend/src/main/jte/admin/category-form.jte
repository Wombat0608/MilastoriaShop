@import ru.milastoria.view.CategoryFormView
@import ru.milastoria.domain.Category

@param CategoryFormView view

@template.admin.layout(
    title = view.pageTitle(),
    content = @`
        <nav class="breadcrumb" aria-label="Навигация">
            <a href="/admin/categories">Разделы</a>
            <span aria-hidden="true">/</span>
            <span>${view.creating() ? "Новый раздел" : view.category().getTitle()}</span>
        </nav>
        <div class="section-head admin-head">
            <h1>${view.pageTitle()}</h1>
            @if(!view.creating())
                <div class="admin-head__actions">
                    <a class="btn btn--line" href="/gallery?category=${view.category().getSlug()}" target="_blank" rel="noopener">Галерея</a>
                </div>
            @endif
        </div>

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif
        @if(view.notice() != null)
            <p class="admin-notice">${view.notice()}</p>
        @endif

        <form method="post" action="${view.action()}" enctype="multipart/form-data" class="admin-form admin-form--lot" id="category-form">
            <label>
                Название *
                <input type="text" name="title" required data-slug-source
                       value="${view.category() != null && view.category().getTitle() != null ? view.category().getTitle() : ""}"
                       placeholder="Платья для девочек">
            </label>

            <label>
                Slug (URL) *
                <input type="text" name="slug" data-slug-target
                       value="${view.category() != null && view.category().getSlug() != null ? view.category().getSlug() : ""}"
                       placeholder="girls" pattern="[a-z0-9]+(-[a-z0-9]+)*">
                <span class="admin-hint">Латиница и дефисы. URL галереи: /gallery?category=…</span>
            </label>

            <label>
                Лид (под заголовком на плитке)
                <textarea name="lead" rows="2"
                          placeholder="Короткий зазывной, 1–2 строки">${view.category() != null && view.category().getLead() != null ? view.category().getLead() : ""}</textarea>
                <span class="admin-hint">Показывается на направлении на главной под названием. SEO-текст — отдельно, для страницы раздела.</span>
            </label>

            <label>
                SEO-текст
                <textarea name="seo_text" rows="3">${view.category() != null && view.category().getSeoText() != null ? view.category().getSeoText() : ""}</textarea>
            </label>

            <div class="cat-cover">
                <span class="admin-field-label">Обложка</span>
                <div class="cat-cover__row">
                    <div class="cat-cover__thumb" id="cover-thumb">
                        @if(view.category() != null && view.category().getCoverImage() != null && !view.category().getCoverImage().isBlank())
                            <img src="${view.category().getCoverImage()}" alt="Обложка раздела">
                        @else
                            <span class="cat-cover__empty">нет обложки</span>
                        @endif
                    </div>
                    <div class="cat-cover__controls">
                        <label class="btn btn--line cat-cover__btn">
                            Выбрать / заменить
                            <input type="file" name="cover" accept="image/*" id="cover-file" hidden>
                        </label>
                        <span class="admin-hint">Кадрирование — после выбора файла. Без водяного знака.</span>
                    </div>
                </div>
                <input type="hidden" name="cover_x" id="cover-x">
                <input type="hidden" name="cover_y" id="cover-y">
                <input type="hidden" name="cover_w" id="cover-w">
                <input type="hidden" name="cover_h" id="cover-h">
            </div>

            <div class="admin-form__actions">
                <button type="submit" class="btn btn--fill">${view.creating() ? "Создать раздел" : "Сохранить"}</button>
                <a class="btn btn--line" href="/admin/categories">К списку</a>
            </div>
        </form>

        <div class="crop-stage" id="cover-crop-stage" hidden>
            <div class="crop-stage__image-wrap" id="cover-crop-wrap">
                <img id="cover-crop-image" alt="">
            </div>
            <div class="crop-stage__actions">
                <button type="button" class="btn btn--line" id="cover-crop-cancel">Отмена</button>
                <button type="button" class="btn btn--fill" id="cover-crop-confirm">Готово</button>
            </div>
        </div>

        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.css">
        <script src="https://cdn.jsdelivr.net/npm/cropperjs@1.6.2/dist/cropper.min.js"></script>
        <script src="/js/admin-cover-crop.js"></script>
    `
)
