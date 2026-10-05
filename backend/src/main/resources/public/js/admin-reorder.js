/* Админка: перетаскивание фото на странице лота + вертушка на reorder. */

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
    filter: ".admin-photo-view, .admin-photo-delete, form",
    preventOnFilter: false,
    delay: 180,
    delayOnTouchOnly: true,
    touchStartThreshold: 4,
    onEnd: function () {
      var ids = Array.prototype.map
        .call(grid.querySelectorAll("[data-image-id]"), function (el) {
          return el.getAttribute("data-image-id");
        })
        .join(",");

      var loading = window.AdminLoading;
      var token = loading && loading.show ? loading.show("Сохраняю порядок фото…") : null;

      fetch("/admin/lots/" + lotId + "/photos/reorder", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
        body: "order=" + encodeURIComponent(ids),
        credentials: "same-origin",
      }).then(function (res) {
        if (loading && loading.hide) loading.hide(token);
        if (!res.ok) {
          console.error("reorder failed", res.status);
        }
      }).catch(function (err) {
        if (loading && loading.hide) loading.hide(token);
        console.error("reorder failed", err);
      });
    },
  });
})();
