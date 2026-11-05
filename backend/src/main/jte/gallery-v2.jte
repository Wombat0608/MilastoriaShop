@import ru.milastoria.view.ConceptGalleryView

@param ConceptGalleryView view

@template.layout(
    title = "Работы ателье — Milastoria",
    description = view.lead(),
    robots = "noindex, nofollow",
    activeNav = "gallery",
    contacts = view.contacts(),
    baseUrl = view.baseUrl(),
    path = view.seoPath(),
    ogType = "website",
    ogImage = ru.milastoria.view.SiteSeo.ogImage(view.baseUrl(), null),
    ogImageAlt = "Milastoria — ателье нарядного платья",
    extraJsonLd = "",
    content = @`
        <div class="wrap">
            <header class="g2-head">
                <span class="eyebrow">ГАЛЕРЕЯ</span>
                <h1 class="g2-head__title">Работы ателье</h1>
            </header>

            <div class="g2-chips-bar">
                <nav class="g2-chips" role="toolbar" aria-label="Фильтры по категориям">
                    <a href="${view.chipHref(view.chips().get(0))}" class="g2-chip${view.noCategory() ? " is-active" : ""}">ВСЕ</a>
                    @for(int ci = 1; ci < view.chips().size(); ci++)
                        <a href="${view.chipHref(view.chips().get(ci))}" class="g2-chip${view.chips().get(ci).active() ? " is-active" : ""}">${view.chips().get(ci).label()} · ${view.chips().get(ci).count()}</a>
                    @endfor
                    <form class="g2-search" method="get" action="/gallery-v2" role="search">
                        @if(view.activeCategorySlug() != null)
                            <input type="hidden" name="category" value="${view.activeCategorySlug()}">
                        @endif
                        <svg class="g2-search__icon" width="15" height="15" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                            <circle cx="10.5" cy="10.5" r="6.5"></circle>
                            <line x1="15.5" y1="15.5" x2="21" y2="21"></line>
                        </svg>
                        <input class="g2-search__input" name="q" type="search"
                               placeholder="Поиск по работам"
                               aria-label="Поиск по работам"
                               value="${view.query() != null ? view.query() : ""}">
                    </form>
                </nav>
            </div>

            <div class="g2-grid">
                @if(view.lots().isEmpty())
                    <div class="g2-empty">Ничего не найдено</div>
                @endif
                @for(int li = 0; li < view.lots().size(); li++)
                    <article class="g2-card${view.isHero(li) ? " g2-card--hero" : ""}">
                        <a href="/work/${view.lots().get(li).slug()}">
                            @if(view.lots().get(li).coverThumb() != null)
                                <img src="${view.lots().get(li).coverThumb()}" alt="${view.lots().get(li).title()}" loading="lazy">
                            @endif
                            @if(view.lots().get(li).hasVideo())
                                <span class="g2-card__video" aria-hidden="true">▶</span>
                            @endif
                            @if(view.isHero(li))
                                <div class="g2-card__hero-label">
                                    <span class="g2-card__eyebrow">${view.lots().get(li).categoryLabel()}</span>
                                    <h3>${view.lots().get(li).title()}</h3>
                                    <span class="g2-card__cta">Смотреть работу →</span>
                                </div>
                            @else
                                <div class="g2-card__plate">${view.lots().get(li).title()}</div>
                            @endif
                            <div class="g2-card__hover">
                                <span class="g2-card__eyebrow">${view.lots().get(li).categoryLabel()}</span>
                                <h3>${view.lots().get(li).title()}</h3>
                                @if(view.lots().get(li).teaser() != null)
                                    <p>${view.lots().get(li).teaser()}</p>
                                @endif
                                <span class="g2-card__cta">Открыть →</span>
                            </div>
                        </a>
                    </article>
                @endfor
            </div>
        </div>
    `
)
