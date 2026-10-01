@import ru.milastoria.view.CategoryTile
@import ru.milastoria.view.TextUtil
@param CategoryTile cat

<a class="cat-tile" href="/gallery?category=${cat.slug()}">
    <img src="${cat.coverImage()}" alt="${cat.title()} — Milastoria" loading="lazy">
    <div class="cat-tile__shade"></div>
    <div class="cat-tile__label">
        <span>${TextUtil.pluralWorks(cat.lotCount())}</span>
        <h3>${cat.title()}</h3>
    </div>
</a>
