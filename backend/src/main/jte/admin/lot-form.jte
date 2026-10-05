@import ru.milastoria.view.LotFormView
@import ru.milastoria.domain.Category
@import ru.milastoria.domain.Lot

@param LotFormView view

@template.admin.layout(
    title = view.pageTitle(),
    content = @`
        <nav class="breadcrumb" aria-label="Навигация">
            <a href="/admin">Лоты</a>
            <span aria-hidden="true">/</span>
            <span>${view.creating() ? "Новый лот" : view.lot().getTitle()}</span>
        </nav>
        <div class="section-head admin-head">
            <h1>${view.pageTitle()}</h1>
            @if(!view.creating())
                <div class="admin-head__actions">
                    <a class="btn btn--line" href="/admin/media?lotId=${view.lot().getId()}">Из медиатеки</a>
                    <a class="btn btn--line" href="/admin/lots/${view.lot().getId()}/photos">Фото</a>
                    <a class="btn btn--line" href="/work/${view.lot().getSlug()}" target="_blank" rel="noopener">Сайт</a>
                </div>
            @endif
        </div>

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif
        @if(view.notice() != null)
            <p class="admin-notice">${view.notice()}</p>
        @endif

        <form method="post" action="${view.action()}" class="admin-form admin-form--lot">
            <label>
                Название *
                <input type="text" name="title" required
                       value="${view.lot() != null && view.lot().getTitle() != null ? view.lot().getTitle() : ""}"
                       data-slug-source placeholder="Золотое платье Gold">
            </label>

            <div class="admin-form__row admin-form__row--fields">
                        <label class="admin-field">
                            Slug (URL) *
                            <input type="text" name="slug"
                                   value="${view.lot() != null && view.lot().getSlug() != null ? view.lot().getSlug() : ""}"
                                   data-slug-target placeholder="zolotoe-plate-gold" pattern="[a-z0-9]+(-[a-z0-9]+)*">
                        </label>
                        <label class="admin-field">
                            Раздел *
                            <select name="category_id" required>
                                @if(view.lot() == null)
                                    <option value="" disabled selected>— выберите —</option>
                                @else
                                    <option value="" disabled>— выберите —</option>
                                @endif
                                @for(Category cat : view.categories())
                                    @if(view.lot() != null && cat.getId() == view.lot().getCategoryId())
                                        <option value="${cat.getId()}" selected>${cat.getTitle()}</option>
                                    @else
                                        <option value="${cat.getId()}">${cat.getTitle()}</option>
                                    @endif
                                @endfor
                            </select>
                        </label>
                    </div>
                    <p class="admin-hint" style="margin:-0.55rem 0 0">Slug — латиница и дефисы. Если пусто — соберётся из названия.</p>

            <label>
                Аннотация
                <textarea name="annotation" rows="2" placeholder="Пышное платье с многослойным тюлем…">${view.lot() != null && view.lot().getAnnotation() != null ? view.lot().getAnnotation() : ""}</textarea>
            </label>

            <details class="admin-details" open>
                <summary>SEO и поля</summary>
                <div class="admin-details__body">
                    <label>
                        SEO-текст
                        <textarea name="seo_text" rows="4">${view.lot() != null && view.lot().getSeoText() != null ? view.lot().getSeoText() : ""}</textarea>
                    </label>
                    <div class="admin-form__row admin-form__row--fields">
                        <label class="admin-field">
                            META title
                            <input type="text" name="meta_title"
                                   value="${view.lot() != null && view.lot().getMetaTitle() != null ? view.lot().getMetaTitle() : ""}">
                        </label>
                        <label class="admin-field">
                            META description
                            <input type="text" name="meta_description"
                                   value="${view.lot() != null && view.lot().getMetaDescription() != null ? view.lot().getMetaDescription() : ""}">
                        </label>
                    </div>

                    <div class="dict-field" data-dict-field data-dict-name="occasions">
                        <span class="admin-field-label">Повод</span>
                        <div class="dict-chips" data-dict-chips></div>
                        <div class="dict-lookup">
                            <input type="text" data-dict-input list="dict-occasion-list"
                                   placeholder="Выберите или введите новый">
                            <datalist id="dict-occasion-list">
                                @for(String opt : view.occasionOptions())
                                    <option value="${opt}"></option>
                                @endfor
                            </datalist>
                            <button type="button" class="btn btn--line dict-lookup__add" data-dict-add>+ Добавить</button>
                        </div>
                        <input type="hidden" name="occasions" data-dict-values value="${view.occasionsPipe()}">
                    </div>

                    <div class="admin-form__row admin-form__row--fields">
                        <label class="admin-field">
                            Год
                            <input type="text" name="year"
                                   value="${view.lot() != null && view.lot().getYear() != null ? view.lot().getYear() : ""}"
                                   placeholder="2024">
                        </label>
                        <label class="admin-field">
                            Имя клиента
                            <input type="text" name="client_name"
                                   value="${view.lot() != null && view.lot().getClientName() != null ? view.lot().getClientName() : ""}">
                        </label>
                    </div>

                    <div class="dict-field" data-dict-field data-dict-name="fabrics">
                        <span class="admin-field-label">Материалы</span>
                        <div class="dict-chips" data-dict-chips></div>
                        <div class="dict-lookup">
                            <input type="text" data-dict-input list="dict-fabric-list"
                                   placeholder="Выберите или введите новый">
                            <datalist id="dict-fabric-list">
                                @for(String opt : view.fabricOptions())
                                    <option value="${opt}"></option>
                                @endfor
                            </datalist>
                            <button type="button" class="btn btn--line dict-lookup__add" data-dict-add>+ Добавить</button>
                        </div>
                        <input type="hidden" name="fabrics" data-dict-values value="${view.fabricsPipe()}">
                    </div>

                    <div class="dict-field" data-dict-field data-dict-name="tags">
                        <span class="admin-field-label">Теги</span>
                        <div class="dict-chips" data-dict-chips></div>
                        <div class="dict-lookup">
                            <input type="text" data-dict-input list="dict-tag-list"
                                   placeholder="Выберите или введите новый">
                            <datalist id="dict-tag-list">
                                @for(String opt : view.tagOptions())
                                    <option value="${opt}"></option>
                                @endfor
                            </datalist>
                            <button type="button" class="btn btn--line dict-lookup__add" data-dict-add>+ Добавить</button>
                        </div>
                        <span class="admin-hint">Попадают в общий поиск сайта и админки.</span>
                        <input type="hidden" name="tags" data-dict-values value="${view.tagsPipe()}">
                    </div>

                    <div class="admin-form__row admin-form__row--fields">
                        <label class="admin-field">
                            Порядок (сортировка)
                            <input type="number" name="sort"
                                   value="${view.lot() != null ? view.lot().getSort() : 0}"
                                   min="0" step="1">
                            <span class="admin-hint">Меньше — выше. В полном списке лотов можно перетащить строку — поле обновится само.</span>
                        </label>
                        @if(view.lot() != null && view.lot().isFeatured())
                            <label class="admin-check">
                                <input type="checkbox" name="featured" value="1" checked>
                                Избранное (галерея)
                            </label>
                        @else
                            <label class="admin-check">
                                <input type="checkbox" name="featured" value="1">
                                Избранное (галерея)
                            </label>
                        @endif
                    </div>
                    <fieldset class="admin-fieldset">
                        <legend>Статус</legend>
                        @if(view.lot() != null && "published".equals(view.lot().getStatus()))
                            <label class="admin-check">
                                <input type="radio" name="status" value="draft">
                                Черновик
                            </label>
                            <label class="admin-check">
                                <input type="radio" name="status" value="published" checked>
                                Опубликован
                            </label>
                        @else
                            <label class="admin-check">
                                <input type="radio" name="status" value="draft" checked>
                                Черновик
                            </label>
                            <label class="admin-check">
                                <input type="radio" name="status" value="published">
                                Опубликован
                            </label>
                        @endif
                    </fieldset>
                </div>
            </details>

            @if(!view.creating())
                <input type="hidden" name="id" value="${view.lot().getId()}">
            @endif

            <div class="admin-form__actions">
                <button type="submit" class="btn btn--fill">${view.creating() ? "Создать лот" : "Сохранить"}</button>
                <a class="btn btn--line" href="/admin">К списку</a>
            </div>
        </form>
    `
)
