@import ru.milastoria.view.SlogansView
@import ru.milastoria.domain.Slogan

@param SlogansView view

@template.admin.layout(
    title = "Другое",
    content = @`
        <div class="section-head admin-head">
            <h1>Другое</h1>
            <div class="admin-head__actions">
                <a class="btn btn--line" href="/admin">Лоты</a>
                <a class="btn btn--line" href="/" target="_blank" rel="noopener">Сайт</a>
            </div>
        </div>

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif
        @if(view.notice() != null)
            <p class="admin-notice">${view.notice()}</p>
        @endif

        <section class="admin-block">
            <h2>Слоганы H1 (главная)</h2>
            <p class="admin-hint">
                При каждом заходе на главную сервер случайно берёт один включённый слоган.
                Можно использовать простой HTML: <code>&lt;em&gt;слово&lt;/em&gt;</code>.
            </p>

            <form method="post" action="/admin/other/slogans" class="admin-form admin-form--inline">
                <label>
                    Новый слоган
                    <input type="text" name="text" required
                           placeholder="Платье — начало вашей истории">
                </label>
                <div class="admin-form__actions">
                    <button type="submit" class="btn btn--fill">Добавить</button>
                </div>
            </form>

            @if(view.slogans().isEmpty())
                <div class="admin-empty">Слоганов нет — используется текст по умолчанию.</div>
            @else
                <div class="admin-lot-list" style="margin-top:1rem">
                    @for(Slogan s : view.slogans())
                        <article class="admin-lot-row admin-lot-row--slogan">
                            <div class="admin-lot-row__body grow">
                                <div class="admin-slogan-text">${view.raw(s.getText())}</div>
                                <div class="admin-lot-row__meta">
                                    <span class="admin-lot-row__id">#${s.getId()}</span>
                                    @if(s.isEnabled())
                                        <span class="admin-status is-published">включён</span>
                                    @else
                                        <span class="admin-status">выключен</span>
                                    @endif
                                </div>
                            </div>
                            <div class="admin-lot-row__actions">
                                <form method="post" action="/admin/other/slogans/${s.getId()}/toggle">
                                    <button type="submit" class="btn btn--line">
                                        @if(s.isEnabled())
                                            Выключить
                                        @else
                                            Включить
                                        @endif
                                    </button>
                                </form>
                                <form method="post" action="/admin/other/slogans/${s.getId()}/delete"
                                      data-confirm="Удалить слоган?">
                                    <button type="submit" class="btn btn--line">Удалить</button>
                                </form>
                            </div>
                        </article>
                    @endfor
                </div>
            @endif
        </section>

        <section class="admin-block" style="margin-top:2rem">
            <h2>Шапка (hero главной)</h2>
            <p class="admin-hint">
                Текст под H1 — общий для всех слайдов. Фон — коллекция слайдов:
                пара «ПК + телефон» или видео (autoplay, без кнопок управления).
                Если слайдов больше одного — на сайте стрелки ←/→ и автопрокрутка ~30с.
                Один слайд — статичная шапка без стрелок.
            </p>

            <form method="post" action="/admin/other/hero" class="admin-form">
                <label>
                    Подпись (текст под слоганом)
                    <textarea name="hero_lead" rows="3"
                              placeholder="Нарядные платья ручной работы для детей и взрослых…">${view.heroLead()}</textarea>
                </label>
                <div class="admin-form__actions">
                    <button type="submit" class="btn btn--fill">Сохранить текст шапки</button>
                </div>
            </form>

            <h3 style="margin-top:1.5rem">Слайды</h3>
            <p class="admin-hint">
                Сейчас слайдов: <strong>${view.heroSlides().size()}</strong>.
            </p>

            <form method="post" action="/admin/other/hero/slides" enctype="multipart/form-data" class="admin-form">
                <div class="admin-form__row admin-form__row--fields">
                    <label class="admin-field">
                        Алт-текст (alt)
                        <input type="text" name="alt" placeholder="Нарядные платья Milastoria">
                    </label>
                    <label class="admin-field">
                        Фото для ПК
                        <input type="file" name="desktop" accept="image/*" required>
                    </label>
                    <label class="admin-field">
                        Фото для телефона (опц.)
                        <input type="file" name="mobile" accept="image/*">
                        <span class="admin-hint">Портрет; без файла — на моб. будет ПК-версия.</span>
                    </label>
                </div>
                <div class="admin-form__actions">
                    <button type="submit" class="btn btn--fill">Добавить фото-слайд</button>
                </div>
            </form>

            <form method="post" action="/admin/other/hero/slides/video" enctype="multipart/form-data" class="admin-form" style="margin-top:0.75rem">
                <div class="admin-form__row admin-form__row--fields">
                    <label class="admin-field">
                        Алт-текст
                        <input type="text" name="alt" placeholder="Видео Milastoria">
                    </label>
                    <label class="admin-field">
                        Видео (MP4/MOV)
                        <input type="file" name="video" accept="video/*" required>
                        <span class="admin-hint">На сайте: autoplay muted loop, без controls.</span>
                    </label>
                </div>
                <div class="admin-form__actions">
                    <button type="submit" class="btn btn--line">Добавить видео-слайд</button>
                </div>
            </form>

            @if(view.heroSlides().isEmpty())
                <div class="admin-empty" style="margin-top:1rem">Слайдов нет — используется legacy hero_image.</div>
            @else
                <form method="post" action="/admin/other/hero/slides/reorder" id="hero-slides-reorder" style="margin-top:1rem">
                    <div class="admin-lot-list" id="hero-slides-grid">
                        @for(var s : view.heroSlides())
                            <article class="admin-lot-row" data-slide-id="${s.getId()}">
                                <div class="admin-lot-row__icon" style="width:72px;height:48px;overflow:hidden">
                                    @if(s.isVideo())
                                        <span class="admin-lot-row__placeholder" aria-hidden="true">▶</span>
                                    @else
                                        <img src="${s.getDesktopPath()}" alt="" style="width:100%;height:100%;object-fit:cover">
                                    @endif
                                </div>
                                <div class="admin-lot-row__body grow">
                                    <div class="admin-lot-row__title">
                                        @if(s.isVideo())
                                            Видео
                                        @else
                                            Фото-пара
                                        @endif
                                        · ${s.getAlt() != null && !s.getAlt().isBlank() ? s.getAlt() : "—"}
                                    </div>
                                    <div class="admin-lot-row__meta">
                                        <span class="admin-lot-row__id">#${s.getId()}</span>
                                        <code class="admin-lot-row__slug">${s.getDesktopPath()}</code>
                                        @if(s.getMobilePath() != null && !s.getMobilePath().isBlank())
                                            <span class="admin-lot-row__cat">моб: ${s.getMobilePath()}</span>
                                        @endif
                                    </div>
                                </div>
                                <div class="admin-lot-row__actions">
                                    <button type="button" class="btn btn--line" data-hero-up="${s.getId()}">↑</button>
                                    <button type="button" class="btn btn--line" data-hero-down="${s.getId()}">↓</button>
                                    <form method="post" action="/admin/other/hero/slides/${s.getId()}/delete"
                                          data-confirm="Удалить слайд?">
                                        <button type="submit" class="btn btn--line">Удалить</button>
                                    </form>
                                </div>
                            </article>
                        @endfor
                    </div>
                    <input type="hidden" name="order" id="hero-order" value="">
                </form>
                <script src="/js/admin-hero-slides.js"></script>
            @endif
        </section>

        <section class="admin-block" style="margin-top:2rem">
            <h2>Раздел «О нас»</h2>
            <p class="admin-hint">Фото + заголовок + HTML-текст на главной (#about).</p>

            <form method="post" action="/admin/other/about" enctype="multipart/form-data" class="admin-form">
                <label>
                    Заголовок
                    <input type="text" name="about_title" value="${view.aboutTitle()}"
                           placeholder="Меньше витрины, больше характера">
                </label>
                <label>
                    Текст (HTML)
                    <textarea name="about_html" rows="6" placeholder="<p>Наш текст…</p>">${view.aboutHtml()}</textarea>
                </label>
                <div class="cat-cover">
                    <span class="admin-field-label">Фото</span>
                    <div class="cat-cover__row">
                        <div class="cat-cover__thumb">
                            @if(view.aboutImage() != null && !view.aboutImage().isBlank())
                                <img src="${view.aboutImage()}" alt="О нас">
                            @else
                                <span class="cat-cover__empty">нет фото</span>
                            @endif
                        </div>
                        <div class="cat-cover__controls">
                            <label class="btn btn--line cat-cover__btn">
                                Выбрать / заменить
                                <input type="file" name="about_image" accept="image/*" hidden>
                            </label>
                            <span class="admin-hint">Без кропа и watermark. Пишется в CONTENT_DIR/img/about.jpg.</span>
                        </div>
                    </div>
                </div>
                <div class="admin-form__actions">
                    <button type="submit" class="btn btn--fill">Сохранить «О нас»</button>
                </div>
            </form>
        </section>

        <section class="admin-block" style="margin-top:2rem">
            <h2>Раздел «Галерея»</h2>
            <p class="admin-hint">Заголовок и описание на странице /gallery, когда не выбрана категория и не задан поиск.</p>

            <form method="post" action="/admin/other/gallery" class="admin-form">
                <label>
                    Заголовок
                    <input type="text" name="gallery_title" value="${view.galleryTitle()}"
                           placeholder="Галерея работ">
                </label>
                <label>
                    Описание
                    <textarea name="gallery_description" rows="4"
                              placeholder="Коллекция нарядных детских, женских платьев…">${view.galleryDescription()}</textarea>
                </label>
                <div class="admin-form__actions">
                    <button type="submit" class="btn btn--fill">Сохранить «Галерея»</button>
                </div>
            </form>
        </section>
    `
)
