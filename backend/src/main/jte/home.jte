@import ru.milastoria.view.HomeView

@param HomeView view

@template.layout(
    title = "Milastoria — ателье нарядного детского и женского платья | Кубинка",
    description = "Ателье нарядных детских и женских платьев Milastoria: галерея работ, Family Look, пошив на заказ по меркам, подбор тканей. Кубинка, Московская область.",
    activeNav = "home",
    contacts = view.contacts(),
    baseUrl = view.baseUrl(),
    path = "/",
    extraJsonLd = "",
    content = @`
        <section class="hero">
            <div class="hero__media">
                <picture>
                    @if(view.heroImageMobile() != null && !view.heroImageMobile().isBlank())
                        <source media="(max-width: 768px)" srcset="${view.heroImageMobile()}">
                    @endif
                    <img src="${view.heroImage()}" alt="Нарядные платья Milastoria" fetchpriority="high">
                </picture>
                <div class="hero__shade"></div>
            </div>
            <div class="wrap hero__content">
                <span class="eyebrow">Ателье с 2016 года</span>
                <h1>${view.slogan()}</h1>
                <p class="lead">${view.heroLead()}</p>
                <div class="hero__actions">
                    <a class="link" href="/gallery">Смотреть галерею</a>
                </div>
            </div>
        </section>

        @if(!view.categories().isEmpty())
            <div class="marquee" aria-hidden="true">
                <div class="marquee__track">
                    @for(int copy = 0; copy < 2; copy++)
                        @for(var cat : view.categories())
                            <span>${cat.title()}</span>
                        @endfor
                    @endfor
                </div>
            </div>
        @endif

        <section class="section section--tight" style="background: var(--paper-dim); border-bottom:1px solid var(--line);">
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

        <section class="section section--tight">
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
