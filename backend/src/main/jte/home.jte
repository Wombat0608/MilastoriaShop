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
                <span class="eyebrow">Ателье с 2016 года · Кубинка</span>
                <h1>Платье — начало вашей <em>истории</em></h1>
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
                <div class="wrap" style="margin-bottom: 2.25rem;">
                    <div class="section-head" style="margin-bottom:0">
                        <h2>Образы в движении</h2>
                        <span class="more" style="pointer-events:none; border-color:transparent">Пролистайте вбок →</span>
                    </div>
                </div>
                <div class="video-strip bleed">
                    @for(var v : view.videos())
                        @template.partials.videoCard(v = v)
                    @endfor
                </div>
            </section>
        @endif

        <section class="section" id="about">
            <div class="wrap split">
                <div class="split__visual">
                    <img src="/content/img/client-03.jpg" alt="Образ гостя в платье Milastoria" loading="lazy">
                </div>
                <div>
                    <span class="eyebrow">Подход</span>
                    <h2>Меньше витрины, больше характера</h2>
                    <p>
                        Мы уходим от «магазинной» механики Tilda к спокойной галерее: крупные кадры,
                        короткие аннотации, теги для поиска, видео там, где платье раскрывается
                        в движении.
                    </p>
                    <a class="link" href="/work/zoloto-bant">Пример карточки лота →</a>
                </div>
            </div>
        </section>

        <section class="section contacts-dark" id="contacts">
            <div class="wrap split">
                <div>
                    <span class="eyebrow">Контакты</span>
                    <h2>Обсудим платье для вашего события</h2>
                    <p>
                        Напишите в WhatsApp, Telegram или позвоните — расскажем о сроках,
                        посадке и вариантах комплекта.
                    </p>
                    <ul class="contact-list">
                        <li>
                            <span class="label">Телефон</span>
                            <a href="tel:+79264296458">+7 (926) 429-64-58</a>
                        </li>
                        <li>
                            <span class="label">Почта</span>
                            <a href="mailto:shop@milastoria.ru">shop@milastoria.ru</a>
                        </li>
                        <li>
                            <span class="label">Адрес</span>
                            <span>Московская область, город Кубинка</span>
                        </li>
                        <li>
                            <span class="label">Мессенджеры</span>
                            <span>
                                <a href="https://wa.me/79264296458" target="_blank" rel="noopener">WhatsApp</a>
                                ·
                                <a href="https://t.me/Milastoria" target="_blank" rel="noopener">Telegram</a>
                            </span>
                        </li>
                    </ul>
                </div>
                <div class="split__visual">
                    <img src="/content/img/client-01.jpg" alt="Клиент Milastoria" loading="lazy">
                </div>
            </div>
        </section>
    `
)
