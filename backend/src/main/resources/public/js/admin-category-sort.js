/* Сортировка разделов drag-and-drop (iPad: long-press) + вертушка. */

(function () {
  var grid = document.getElementById("category-grid");
  if (!grid || typeof Sortable === "undefined") return;

  Sortable.create(grid, {
    animation: 150,
    ghostClass: "is-sort-ghost",
    chosenClass: "is-sort-chosen",
    handle: ".admin-lot-row",
    delay: 180,
    delayOnTouchOnly: true,
    touchStartThreshold: 4,
    onEnd: function () {
      var ids = Array.prototype.map
        .call(grid.querySelectorAll("[data-category-id]"), function (el) {
          return el.getAttribute("data-category-id");
        })
        .join(",");
      var loading = window.AdminLoading;
      var token = loading && loading.show ? loading.show("Сохраняю порядок разделов…") : null;
      fetch("/admin/categories/reorder", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
        body: "order=" + encodeURIComponent(ids),
        credentials: "same-origin",
      }).then(function (res) {
        if (loading && loading.hide) loading.hide(token);
        if (!res.ok) console.error("category reorder failed", res.status);
      }).catch(function (err) {
        if (loading && loading.hide) loading.hide(token);
        console.error("category reorder failed", err);
      });
    },
  });
})();
