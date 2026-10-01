@import ru.milastoria.view.AdminLotsView
@import ru.milastoria.view.AdminLotRow
@import ru.milastoria.domain.Category
@import ru.milastoria.view.TextUtil

@param AdminLotsView view

@template.admin.layout(
    title = "Лоты",
    content = @`
        <div class="section-head admin-head">
            <h1>Лоты</h1>
            <div class="admin-head__actions">
                <a class="btn btn--line" href="/admin/categories">Разделы</a>
                <a class="btn btn--fill" href="/admin/lots/new">+ Новый лот</a>
            </div>
        </div>

        @if(view.notice() != null)
            <p class="admin-notice">${view.notice()}</p>
        @endif

        <form class="admin-search" method="get" action="/admin" id="admin-filter-form">
            <div class="admin-search__row">
                <label for="admin-q">Поиск</label>
                <input id="admin-q" name="q" type="search"
                       placeholder="заголовок, тег, SEO-текст…"
                       value="${view.query() != null ? view.query() : ""}">
            </div>

            @if(!view.categories().isEmpty())
                <div class="admin-cat-filter">
                    <label class="admin-cat-check">
                        @if(!view.filteringCategories())
                            <input type="checkbox" id="cat-all" name="cat_all" value="1" checked>
                        @else
                            <input type="checkbox" id="cat-all" name="cat_all" value="1">
                        @endif
                        <span>Все разделы</span>
                    </label>
                    @for(Category cat : view.categories())
                        @if(view.isCategorySelected(cat.getId()))
                            <label class="admin-cat-check">
                                <input type="checkbox" name="categories" value="${cat.getId()}" checked>
                                <span>${cat.getTitle()}</span>
                            </label>
                        @else
                            <label class="admin-cat-check">
                                <input type="checkbox" name="categories" value="${cat.getId()}">
                                <span>${cat.getTitle()}</span>
                            </label>
                        @endif
                    @endfor
                </div>
            @endif
        </form>

        @if(view.lots().isEmpty())
            <div class="admin-empty">
                @if(view.hasFilters())
                    Ничего не найдено
                    @if(view.searching())
                        по запросу «${view.query()}»
                    @endif
                    @if(view.filteringCategories())
                        в выбранных разделах
                    @endif
                @else
                    Лотов пока нет — <a href="/admin/lots/new">создайте первый</a>.
                @endif
            </div>
        @else
            <div class="admin-meta">
                ${TextUtil.pluralWorks(view.lots().size())}@if(view.searching())
                    · FTS
                @endif@if(view.filteringCategories())
                    · фильтр по разделам
                @endif
            </div>
            <div class="admin-lot-list">
                @for(AdminLotRow lot : view.lots())
                    <article class="admin-lot-row">
                        <a class="admin-lot-row__icon" href="/admin/lots/${lot.id()}/edit" aria-label="Открыть ${lot.title()}">
                            @if(lot.coverThumb() != null && !lot.coverThumb().isBlank())
                                <img src="${lot.coverThumb()}" alt="" loading="lazy">
                            @else
                                <span class="admin-lot-row__placeholder" aria-hidden="true">?</span>
                            @endif
                        </a>
                        <div class="admin-lot-row__body grow">
                            <a class="admin-lot-row__title" href="/admin/lots/${lot.id()}/edit">${lot.title()}</a>
                            <div class="admin-lot-row__meta">
                                <code class="admin-lot-row__slug">/${lot.slug()}</code>
                                <span class="admin-lot-row__id">#${lot.id()}</span>
                                @if(lot.categoryTitle() != null && !lot.categoryTitle().isBlank())
                                    <span class="admin-lot-row__cat">${lot.categoryTitle()}</span>
                                @endif
                                <span class="admin-lot-row__date">${lot.createdAtFormatted()}</span>
                                <span class="admin-status ${lot.published() ? "is-published" : ""}">${lot.status()}</span>
                                @if(lot.featured())
                                    <span class="admin-lot-row__flag">★</span>
                                @endif
                            </div>
                        </div>
                        <div class="admin-lot-row__actions">
                            <a class="btn btn--line" href="/work/${lot.slug()}" target="_blank" rel="noopener">Сайт</a>
                            <a class="btn btn--line" href="/admin/lots/${lot.id()}/photos">Фото</a>
                            <a class="btn btn--line" href="/admin/lots/${lot.id()}/edit">Правка</a>
                        </div>
                    </article>
                @endfor
            </div>
        @endif
        <script src="/js/admin-filter.js"></script>
    `
)
