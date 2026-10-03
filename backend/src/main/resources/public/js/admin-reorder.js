/* Админка: перетаскивание фото на странице лота.
   SortableJS — touch-драг на iPad; после drop шлём POST order=id1,id2,… */

(function () {
  var grid = document.getElementById("photo-grid");
  if (!grid || typeof Sortable === "undefined") return;

  var lotId = grid.getAttribute("data-lot-id");
  if (!lotId) return;

  Sortable.create(grid, {
    animation: 150,
    ghostClass: "is-sort-ghost",
    chosenClass: "is-sort-chosen",
    dragClass: "is-sort-drag",
    handle: ".admin-photo-card",
    // кнопки «Просмотр» и «Удалить» не должны начинать drag
    filter: ".admin-photo-view, .admin-photo-delete, form",
    preventOnFilter: false,
    delay: 180,
    delayOnTouchOnly: true,
    touchStartThreshold: 4,
    fallbackTolerance: 3,
    onEnd: function () {
      var ids = Array.prototype.map
        .call(grid.querySelectorAll("[data-image-id]"), function (el) {
          return el.getAttribute("data-image-id");
        })
        .join(",");

      fetch("/admin/lots/" + lotId + "/photos/reorder", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
        body: "order=" + encodeURIComponent(ids),
        credentials: "same-origin",
      }).then(function (res) {
        if (!res.ok) {
          console.error("reorder failed", res.status);
        }
      });
    },
  });
})();
