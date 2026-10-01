package ru.milastoria.view;

import java.util.List;

public record HomeView(List<CategoryTile> categories, List<LotCard> featuredLots, List<VideoTile> videos) {
}
