@import ru.milastoria.view.ContactsView

@param ContactsView view

@template.admin.layout(
    title = "Контакты",
    content = @`
        <div class="section-head admin-head">
            <h1>Контакты</h1>
            <div class="admin-head__actions">
                <a class="btn btn--line" href="/admin">Лоты</a>
                <a class="btn btn--line" href="/#contacts" target="_blank" rel="noopener">Сайт</a>
            </div>
        </div>

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif
        @if(view.notice() != null)
            <p class="admin-notice">${view.notice()}</p>
        @endif

        <section class="admin-block">
            <h2>Блок «Контакты» на главной</h2>
            <p class="admin-hint">
                Заголовок H2, подпись, фото и контакты для секции #contacts.
                Мессенджеры: по одной паре на строку в формате
                <code>Имя|ссылка</code>, например
                <code>WhatsApp|https://wa.me/79264296458</code>.
            </p>

            <form method="post" action="/admin/contacts" enctype="multipart/form-data" class="admin-form">
                <label>
                    Заголовок H2
                    <input type="text" name="contacts_title" value="${view.title()}"
                           placeholder="Обсудим платье для вашего события">
                </label>
                <label>
                    Подпись
                    <textarea name="contacts_lead" rows="3"
                              placeholder="Напишите в WhatsApp, Telegram или позвоните…">${view.lead()}</textarea>
                </label>

                <div class="admin-form__row admin-form__row--fields">
                    <label class="admin-field">
                        Телефон
                        <input type="text" name="contacts_phone" value="${view.phone()}"
                               placeholder="+7 (926) 429-64-58">
                    </label>
                    <label class="admin-field">
                        Почта
                        <input type="text" name="contacts_email" value="${view.email()}"
                               placeholder="shop@milastoria.ru">
                    </label>
                </div>
                <label>
                    Адрес
                    <input type="text" name="contacts_address" value="${view.address()}"
                           placeholder="Московская область, город Кубинка">
                </label>
                <label>
                    Мессенджеры (Имя|ссылка, по строке)
                    <textarea name="contacts_messengers" rows="4"
                              placeholder="WhatsApp|https://wa.me/79264296458&#10;Telegram|https://t.me/Milastoria">${view.messengers()}</textarea>
                </label>

                <div class="cat-cover">
                    <span class="admin-field-label">Фото</span>
                    <div class="cat-cover__row">
                        <div class="cat-cover__thumb">
                            @if(view.image() != null && !view.image().isBlank())
                                <img src="${view.image()}" alt="Контакты">
                            @else
                                <span class="cat-cover__empty">нет фото</span>
                            @endif
                        </div>
                        <div class="cat-cover__controls">
                            <label class="btn btn--line cat-cover__btn">
                                Выбрать / заменить
                                <input type="file" name="contacts_image" accept="image/*" hidden>
                            </label>
                            <span class="admin-hint">Без кропа и watermark. Пишется в CONTENT_DIR/img/contacts.jpg.</span>
                        </div>
                    </div>
                </div>

                <div class="admin-form__actions">
                    <button type="submit" class="btn btn--fill">Сохранить контакты</button>
                </div>
            </form>
        </section>
    `
)
