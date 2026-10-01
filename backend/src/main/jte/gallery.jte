@import ru.milastoria.view.GalleryView

@param GalleryView view

@template.layout(
    title = view.title() + " — Milastoria",
    description = view.lead(),
    activeNav = "gallery",
    content = @`
        <div class="wrap">
            <header class="page-hero">
                <nav class="breadcrumb" aria-label="Хлебные крошки">
                    <a href="/">Главная</a>
                    <span aria-hidden="true">/</span>
                    <span>Галерея</span>
                </nav>
                <h1>${view.title()}</h1>
                <p>${view.lead()}</p>
            </header>

            <div class="filters" role="toolbar" aria-label="Фильтры по категориям">
                <a href="/gallery" class="filter-btn ${view.noFilters() ? "is-active" : ""}">Все работы</a>
                @for(var opt : view.categoryOptions())
                    <a href="/gallery?category=${opt.slug()}" class="filter-btn ${opt.active() ? "is-active" : ""}">${opt.title()}</a>
                @endfor
            </div>

            <form class="search-row" method="get" action="/gallery">
                @if(view.activeCategorySlug() != null)
                    <input type="hidden" name="category" value="${view.activeCategorySlug()}">
                @endif
                <label for="gallery-search">Поиск</label>
                <input id="gallery-search" name="q" type="search" placeholder="платье, gold, family…" value="${view.query() != null ? view.query() : ""}">
            </form>

            <div class="lot-masonry" style="margin-bottom: 4rem">
                @if(view.lots().isEmpty())
                    <div class="empty">Ничего не найдено</div>
                @endif
                @for(var card : view.lots())
                    @template.partials.lotCard(card = card)
                @endfor
            </div>
        </div>

        <section class="section section--tight" style="background: var(--paper-dim); border-top:1px solid var(--line);">
            <div class="wrap manifesto">
                <div>
                    <span class="eyebrow">SEO</span>
                    <h2 style="font-size:1.6rem">Как это работает для поиска</h2>
                </div>
                <div class="manifesto__facts" style="font-style:normal">
                    <div>
                        <strong>Поля лота</strong>
                        <span>title, description, canonical, JSON-LD Product, alt у каждого кадра — заполняются в админке при публикации.</span>
                    </div>
                    <div>
                        <strong>URL-структура</strong>
                        <span>Категории и лоты — по адресам <code>/gallery?category=girls</code>, <code>/work/gold-01</code>.</span>
                    </div>
                    <div>
                        <strong>Рендер</strong>
                        <span>Серверный (jte) — страница приходит краулеру уже с готовым HTML, без JS-сборки на клиенте.</span>
                    </div>
                </div>
            </div>
        </section>
    `
)
