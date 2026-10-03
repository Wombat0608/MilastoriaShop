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
                        Платье шьётся на заказ — от первого разговора до готового образа.
                        Расскажите повод и настроение, подскажем силуэт, ткань и сроки.
                    </p>
                    <a class="btn btn--line" href="/#contacts" style="margin-top:1.5rem">Обсудить заказ</a>
                </div>
                <div class="manifesto__facts manifesto__facts--order" style="font-style:normal">
                    <div>
                        <strong>1. Заявка</strong>
                        <span>Пишите в WhatsApp или Telegram: дата события, примеры из галереи, пожелания по силуэту и цвету.</span>
                    </div>
                    <div>
                        <strong>2. Эскиз и ткани</strong>
                        <span>Обсуждаем детали, подбираем ткань и декор, согласовываем стоимость и сроки пошива.</span>
                    </div>
                    <div>
                        <strong>3. Замеры и пошив</strong>
                        <span>Снимаем мерки, шьём платье, при необходимости правим на промежуточных примерках.</span>
                    </div>
                    <div>
                        <strong>4. Готовое платье</strong>
                        <span>Финальная примерка и передача — платье готово к вашему событию.</span>
                    </div>
                </div>
            </div>
        </section>
    `
)
