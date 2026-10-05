@import ru.milastoria.view.GalleryView

@param GalleryView view

@template.layout(
    title = view.title() + " — Milastoria",
    description = view.lead(),
    activeNav = "gallery",
    contacts = view.contacts(),
    baseUrl = view.baseUrl(),
    path = view.seoPath(),
    extraJsonLd = "",
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
                <label class="search-row__label" for="gallery-search">
                    <span class="search-row__heart" aria-hidden="true"></span>
                    Поиск
                </label>
                <div class="search-row__field">
                    <input id="gallery-search" name="q" type="search"
                           placeholder="платье, gold, family…"
                           value="${view.query() != null ? view.query() : ""}">
                </div>
            </form>

            @if(view.showCategoryHighlights())
                <div class="section-head" style="margin-top:2.5rem">
                    <h2>Избранные работы</h2>
                </div>
                <div class="lot-masonry" style="margin-bottom:2.5rem">
                    @for(var card : view.featuredLots())
                        @template.partials.lotCard(card = card)
                    @endfor
                </div>

                @if(!view.videos().isEmpty())
                    <div class="section-head" style="margin-bottom:0">
                        <h2>Образы в движении</h2>
                        <span class="more" style="pointer-events:none; border-color:transparent">Пролистайте вбок →</span>
                    </div>
                    <div class="video-strip" style="margin-top:1rem; margin-bottom:2.5rem">
                        @for(var v : view.videos())
                            @template.partials.videoCard(v = v)
                        @endfor
                    </div>
                @endif
            @endif

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
                    <span class="eyebrow">Заказ</span>
                    <h2 style="font-size:1.6rem">Как проходит заказ</h2>
                    <p style="margin-top:1rem; color:var(--ink-soft); max-width:28em">
                        Шьём по вашим меркам — <strong>без промежуточных примерок</strong>.
                        Подбираем ткани и материалы под задачу, силуэт и событие.
                    </p>
                    <a class="btn btn--line" href="/#contacts" style="margin-top:1.5rem">Обсудить заказ</a>
                </div>
                <div class="manifesto__facts manifesto__facts--order" style="font-style:normal">
                    <div>
                        <strong>Заявка</strong>
                        <span>Пишите в WhatsApp или Telegram: дата события, образы из галереи, пожелания по силуэту и цвету.</span>
                    </div>
                    <div>
                        <strong>Мерки и подбор тканей</strong>
                        <span>Снимаем мерки, подбираем ткань, декор и материалы — находим то, что нужно именно вашему платью.</span>
                    </div>
                    <div>
                        <strong>Пошив без примерок</strong>
                        <span>Платье шьётся по размерам: без промежуточных примерок, с точной посадкой «под вас».</span>
                    </div>
                    <div>
                        <strong>Готовое платье</strong>
                        <span>Финальная передача — образ готов к вашему событию.</span>
                    </div>
                </div>
            </div>
        </section>
    `
)
