@import ru.milastoria.view.HomeView

@param HomeView view

@template.layout(
    title = "Milastoria — ателье нарядного детского и женского платья",
    description = "Milastoria — ателье нарядных детских и женских платьев: галерея работ, Family Look, пошив на заказ. Кубинка, Московская область.",
    activeNav = "home",
    content = @`
        <section class="hero">
            <div class="hero__media">
                <img src="/content/img/hero.jpg" alt="Нарядные платья Milastoria" fetchpriority="high">
                <div class="hero__shade"></div>
            </div>
            <div class="wrap hero__content">
                <span class="eyebrow">Ателье с 2016 года</span>
                <h1>${view.slogan()}</h1>
                <p class="lead">
                    Нарядные платья ручной работы для детей и взрослых: галерея работ, Family Look,
                    съёмки клиентов. Пошив на заказ — от идеи до примерки.
                </p>
                <div class="hero__actions">
                    <a class="btn btn--fill-inverse" href="/gallery">Смотреть галерею</a>
                    <a class="link" href="/gallery?category=family">Family Look →</a>
                </div>
            </div>
        </section>

        <div class="marquee" aria-hidden="true">
            <div class="marquee__track">
                <span>Платья для девочек</span>
                <span>Женское платье</span>
                <span>Family Look</span>
                <span>Работы клиентов</span>
                <span>Пошив на заказ</span>
                <span>Платья для девочек</span>
                <span>Женское платье</span>
                <span>Family Look</span>
                <span>Работы клиентов</span>
                <span>Пошив на заказ</span>
            </div>
        </div>

        <section class="section">
            <div class="wrap manifesto">
                <div>
                    <span class="eyebrow">Подход</span>
                    <h2>Галерея, а не витрина магазина</h2>
                </div>
                <div class="manifesto__facts">
                    <div>
                        <strong>С 2016 года</strong>
                        <span>Занимаемся пошивом дизайнерского детского и женского платья, экспериментируем с материалами и кроем.</span>
                    </div>
                    <div>
                        <strong>Направления</strong>
                        <span>Платья для девочек · женское платье · Family Look · работы клиентов</span>
                    </div>
                    <div>
                        <strong>Формат</strong>
                        <span>Спокойная галерея вместо магазинной механики: крупные кадры, короткие аннотации, видео там, где платье раскрывается в движении.</span>
                    </div>
                </div>
            </div>
        </section>

        <section class="section section--tight" style="padding-top:0">
            <div class="wrap">
                <div class="section-head">
                    <h2>Направления</h2>
                    <a class="more" href="/gallery">Вся галерея</a>
                </div>
                <div class="cat-bento">
                    @for(var cat : view.categories())
                        @template.partials.catTile(cat = cat)
                    @endfor
                </div>
            </div>
        </section>

        <section class="section">
            <div class="wrap">
                <div class="section-head">
                    <h2>Избранные работы</h2>
                    <a class="more" href="/gallery">Все работы</a>
                </div>
                <div class="lot-masonry">
                    @for(var card : view.featuredLots())
                        @template.partials.lotCard(card = card)
                    @endfor
                </div>
            </div>
        </section>

        @if(!view.videos().isEmpty())
            <section class="section section--tight">
                <div class="wrap">
                    <div class="section-head" style="margin-bottom:0">
                        <h2>Образы в движении</h2>
                        <span class="more" style="pointer-events:none; border-color:transparent">Пролистайте вбок →</span>
                    </div>
                    <div class="video-strip">
                        @for(var v : view.videos())
                            @template.partials.videoCard(v = v)
                        @endfor
                    </div>
                </div>
            </section>
        @endif

        <section class="section" id="about">
            <div class="wrap split">
                <div class="split__visual">
                    <img src="${view.aboutImage()}" alt="${view.aboutTitle()}" loading="lazy">
                </div>
                <div>
                    <span class="eyebrow">О нас</span>
                    <h2>${view.aboutTitle()}</h2>
                    <div class="about-html">
                        ${view.aboutContent()}
                    </div>
                </div>
            </div>
        </section>

        <section class="section contacts-dark" id="contacts">
            <div class="wrap split">
                <div>
                    <span class="eyebrow">Контакты</span>
                    <h2>${view.contactsTitle()}</h2>
                    <p>${view.contactsLead()}</p>
                    <ul class="contact-list">
                        @if(view.hasContactsPhone())
                            <li>
                                <span class="label">Телефон</span>
                                <a href="${view.phoneHref()}">${view.contactsPhone()}</a>
                            </li>
                        @endif
                        @if(view.hasContactsEmail())
                            <li>
                                <span class="label">Почта</span>
                                <a href="${view.emailHref()}">${view.contactsEmail()}</a>
                            </li>
                        @endif
                        @if(view.hasContactsAddress())
                            <li>
                                <span class="label">Адрес</span>
                                <span>${view.contactsAddress()}</span>
                            </li>
                        @endif
                        @if(view.hasContactsMessengers())
                            <li>
                                <span class="label">Мессенджеры</span>
                                <span>
                                    @for(int mi = 0; mi < view.contactsMessengers().size(); mi++)
                                        @if(mi > 0)
                                            ·
                                        @endif
                                        <a href="${view.contactsMessengers().get(mi).url()}" target="_blank" rel="noopener">${view.contactsMessengers().get(mi).name()}</a>
                                    @endfor
                                </span>
                            </li>
                        @endif
                    </ul>
                </div>
                <div class="split__visual">
                    <img src="${view.contactsImage()}" alt="${view.contactsTitle()}" loading="lazy">
                </div>
            </div>
        </section>
    `
)
