@import ru.milastoria.domain.Lot
@import java.util.List

@param List<Lot> lots

@template.admin.layout(
    title = "Лоты",
    content = @`
        <div class="section-head">
            <h1>Лоты</h1>
        </div>
        <div class="admin-lot-list">
            @for(Lot lot : lots)
                <div class="admin-lot-row">
                    <div class="grow">
                        <strong>${lot.getTitle()}</strong>
                        <div class="admin-status ${lot.getStatus().equals("published") ? "is-published" : ""}">${lot.getStatus()}</div>
                    </div>
                    <a class="btn btn--line" href="/admin/lots/${lot.getId()}/photos">Фото →</a>
                </div>
            @endfor
        </div>
    `
)
