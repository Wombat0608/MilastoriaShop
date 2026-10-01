@import ru.milastoria.view.WorkView
@import ru.milastoria.domain.LotImage
@import ru.milastoria.domain.LotVideo

@param WorkView view

@template.layout(
    title = (view.lot().getMetaTitle() != null ? view.lot().getMetaTitle() : view.lot().getTitle() + " — Milastoria"),
    description = (view.lot().getMetaDescription() != null ? view.lot().getMetaDescription() : view.lot().getAnnotation()),
    activeNav = "gallery",
    content = @`
        <div class="wrap" style="padding-top: 2.5rem;">
            <nav class="breadcrumb" aria-label="Навигация">
                <a href="/">Главная</a>
                <span aria-hidden="true">/</span>
                <a href="/gallery">Галерея</a>
                <span aria-hidden="true">/</span>
                <a href="/gallery?category=${view.categorySlug()}">${view.categoryTitle()}</a>
                <span aria-hidden="true">/</span>
                <span>${view.lot().getTitle()}</span>
            </nav>

            <div class="work-layout">
                <div class="work-gallery" data-work-gallery>
                    <button type="button" class="work-gallery__main" data-lightbox-open aria-label="Открыть фото на весь экран">
                        @if(!view.images().isEmpty())
                            <img data-main-image src="${view.images().get(0).getPathFull()}" alt="${view.images().get(0).getAlt()}">
                        @endif
                    </button>
                    <div class="work-gallery__thumbs">
                        @for(LotImage img : view.images())
                            <button type="button" class="thumb ${img == view.images().get(0) ? "is-active" : ""}"
                                    data-full="${img.getPathFull()}" data-alt="${img.getAlt()}">
                                <img src="${img.getPathThumb()}" alt="${img.getAlt() != null ? img.getAlt() : view.lot().getTitle()}" loading="lazy">
                            </button>
                        @endfor
                    </div>

                    @for(LotVideo video : view.videos())
                        <div class="video-block" id="video-${video.getId()}">
                            <video controls playsinline preload="metadata" poster="${video.getPosterPath()}">
                                <source src="${video.getPath()}" type="video/mp4">
                            </video>
                            <div class="video-block__label">${video.getCaption() != null ? video.getCaption() : "Видео-фрагмент"}</div>
                        </div>
                    @endfor
                </div>

                <aside class="work-aside">
                    <span class="eyebrow">${view.categoryTitle()}</span>
                    <h1>${view.lot().getTitle()}</h1>
                    @if(view.lot().getAnnotation() != null)
                        <p class="annotation">${view.lot().getAnnotation()}</p>
                    @endif

                    <ul class="spec-list">
                        <li><span class="k">Категория</span><span class="v">${view.categoryTitle()}</span></li>
                        @if(!view.occasions().isEmpty())
                            <li>
                                <span class="k">Повод</span>
                                <span class="v">
                                    @for(String item : view.occasions())
                                        <span class="tag-chip">${item}</span>
                                    @endfor
                                </span>
                            </li>
                        @endif
                        @if(view.lot().getYear() != null)
                            <li><span class="k">Год</span><span class="v">${view.lot().getYear()}</span></li>
                        @endif
                        @if(!view.fabrics().isEmpty())
                            <li>
                                <span class="k">Материалы</span>
                                <span class="v">
                                    @for(String item : view.fabrics())
                                        <span class="tag-chip">${item}</span>
                                    @endfor
                                </span>
                            </li>
                        @endif
                        @if(!view.tags().isEmpty())
                            <li>
                                <span class="k">Теги</span>
                                <span class="v">
                                    @for(String tag : view.tags())
                                        <a class="tag" href="/gallery?tag=${tag}">${tag}</a>
                                    @endfor
                                </span>
                            </li>
                        @endif
                    </ul>

                    <div class="actions">
                        <a class="btn btn--fill" href="https://wa.me/79264296458" target="_blank" rel="noopener">Обсудить пошив</a>
                        <a class="btn btn--line" href="tel:+79264296458">+7 (926) 429-64-58</a>
                    </div>
                </aside>
            </div>

            @if(view.lot().getSeoText() != null)
                <section class="work-seo">
                    <h2>О работе</h2>
                    <p>${view.lot().getSeoText()}</p>
                </section>
            @endif

            @if(!view.related().isEmpty())
                <section class="section work-related">
                    <div class="section-head">
                        <h2>Похожие работы</h2>
                        <a class="more" href="/gallery?category=${view.categorySlug()}">Все ${view.categoryTitle().toLowerCase()}</a>
                    </div>
                    <div class="lot-strip">
                        @for(var card : view.related())
                            @template.partials.lotCard(card = card)
                        @endfor
                    </div>
                </section>
            @endif
        </div>
    `
)
