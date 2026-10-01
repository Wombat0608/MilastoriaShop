@import ru.milastoria.view.VideoTile
@param VideoTile v

<a class="video-card" href="${v.href()}">
    <img src="${v.posterPath()}" alt="${v.title()}" loading="lazy">
    <div class="video-card__shade"></div>
    <div class="video-card__play" aria-hidden="true"></div>
    <div class="video-card__caption">
        <strong>${v.title()}</strong>
        <span>${v.kind()} · ${v.caption()}</span>
    </div>
</a>
