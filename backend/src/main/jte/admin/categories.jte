@import ru.milastoria.view.CategoriesView
@import ru.milastoria.domain.Category

@param CategoriesView view

@template.admin.layout(
    title = "Разделы",
    content = @`
        <div class="section-head admin-head">
            <h1>Разделы</h1>
            <div class="admin-head__actions">
                <a class="btn btn--line" href="/admin">Лоты</a>
                <a class="btn btn--fill" href="/admin/categories/new">+ Новый раздел</a>
            </div>
        </div>

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif
        @if(view.notice() != null)
            <p class="admin-notice">${view.notice()}</p>
        @endif

        @if(view.categories().isEmpty())
            <div class="admin-empty">Разделов пока нет — <a href="/admin/categories/new">создайте первый</a>.</div>
        @else
            <p class="admin-hint" style="margin-bottom:0.75rem">
                Порядок на сайте: перетащите карточки. На iPad — удерживайте и тяните.
            </p>
            <div class="admin-lot-list" id="category-grid">
                @for(Category cat : view.categories())
                    <article class="admin-lot-row admin-lot-row--sortable" data-category-id="${cat.getId()}">
                        <div class="admin-lot-row__drag" aria-hidden="true">⋮⋮</div>
                        <a class="admin-lot-row__icon" href="/admin/categories/${cat.getId()}/edit" aria-label="Открыть ${cat.getTitle()}">
                            @if(cat.getCoverImage() != null && !cat.getCoverImage().isBlank())
                                <img src="${cat.getCoverImage()}" alt="" loading="lazy">
                            @else
                                <span class="admin-lot-row__placeholder" aria-hidden="true">§</span>
                            @endif
                        </a>
                        <div class="admin-lot-row__body grow">
                            <a class="admin-lot-row__title" href="/admin/categories/${cat.getId()}/edit">${cat.getTitle()}</a>
                            <div class="admin-lot-row__meta">
                                <code class="admin-lot-row__slug">${cat.getSlug()}</code>
                                <span class="admin-lot-row__id">#${cat.getId()}</span>
                            </div>
                        </div>
                        <div class="admin-lot-row__actions">
                            <a class="btn btn--line" href="/gallery?category=${cat.getSlug()}" target="_blank" rel="noopener">Галерея</a>
                            <a class="btn btn--line" href="/admin/categories/${cat.getId()}/edit">Правка</a>
                        </div>
                    </article>
                @endfor
            </div>
        @endif
        <script src="https://cdn.jsdelivr.net/npm/sortablejs@1.15.3/Sortable.min.js"></script>
        <script src="/js/admin-category-sort.js"></script>
    `
)
