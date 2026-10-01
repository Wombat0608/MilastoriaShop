@import ru.milastoria.view.LotCard
@param LotCard card

<article class="lot-card">
    <a href="/work/${card.slug()}" class="lot-card__link">
        <div class="lot-card__media">
            @if(card.coverThumb() != null)
                <img src="${card.coverThumb()}" alt="${card.title()}" loading="lazy">
            @endif
            @if(card.hasVideo())
                <span class="lot-card__video" aria-hidden="true">▶</span>
            @endif
        </div>
        <div class="lot-card__body">
            <h3>${card.title()}</h3>
            @if(card.annotation() != null)
                <p>${card.annotation()}</p>
            @endif
        </div>
    </a>
</article>
