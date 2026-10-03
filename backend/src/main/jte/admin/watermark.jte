@import ru.milastoria.view.WatermarkView

@param WatermarkView view

@template.admin.layout(
    title = "Водяной знак",
    content = @`
        <div class="section-head admin-head">
            <h1>Водяной знак</h1>
            <div class="admin-head__actions">
                <a class="btn btn--line" href="/admin">Лоты</a>
            </div>
        </div>

        @if(view.error() != null)
            <p class="admin-error">${view.error()}</p>
        @endif
        @if(view.notice() != null)
            <p class="admin-notice">${view.notice()}</p>
        @endif

        <div class="wm-settings">
            <div class="wm-settings__preview">
                <span class="admin-field-label">Текущий watermark</span>
                @if(view.hasFile())
                    <div class="wm-settings__frame">
                        <img src="/admin/branding/watermark.png" alt="Watermark">
                    </div>
                @else
                    <div class="wm-settings__frame wm-settings__frame--empty">Файл не загружен</div>
                @endif
            </div>

            <form method="post" action="/admin/watermark" enctype="multipart/form-data" class="admin-form">
                <label>
                    Файл watermark (PNG с альфа-каналом)
                    <input type="file" name="watermark" accept="image/png,image/webp,image/svg+xml,image/*">
                    <span class="admin-hint">Заменит текущий файл. PNG/WebP/SVG.</span>
                </label>
                <div class="admin-form__row admin-form__row--fields">
                    <label class="admin-field">
                        Размер по умолчанию, % ширины
                        <input type="number" name="width_percent" min="5" max="60" step="1"
                               value="${view.widthPercent()}" required>
                        <span class="admin-hint">Ставится при новой загрузке фото, если не сдвинули на кропе.</span>
                    </label>
                    <label class="admin-field">
                        Отступ (SE), % ширины
                        <input type="number" name="margin_percent" min="0" max="20" step="1"
                               value="${view.marginPercent()}" required>
                    </label>
                </div>
                <div class="admin-form__actions">
                    <button type="submit" class="btn btn--fill">Сохранить</button>
                </div>
            </form>
        </div>
    `
)
