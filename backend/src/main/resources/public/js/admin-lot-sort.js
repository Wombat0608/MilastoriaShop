/* Админка: drag-and-drop сортировка лотов + вертушка на POST reorder. */

(function () {
  var grid = document.getElementById("lot-grid");
  if (!grid || typeof Sortable === "undefined") return;
  if (grid.getAttribute("data-sortable") !== "1") return;

  Sortable.create(grid, {
    animation: 150,
    ghostClass: "is-sort-ghost",
    chosenClass: "is-sort-chosen",
    handle: ".admin-lot-row",
    filter: "a, button, form, input",
    preventOnFilter: false,
    delay: 180,
    delayOnTouchOnly: true,
    touchStartThreshold: 4,
    fallbackTolerance: 3,
    onEnd: function () {
      var ids = Array.prototype.map
        .call(grid.querySelectorAll("[data-lot-id]"), function (el) {
          return el.getAttribute("data-lot-id");
        })
        .join(",");

      var loading = window.AdminLoading;
      var token = loading && loading.show ? loading.show("Сохраняю порядок…") : null;

      fetch("/admin/lots/reorder", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
        body: "order=" + encodeURIComponent(ids),
        credentials: "same-origin",
      }).then(function (res) {
        if (loading && loading.hide) loading.hide(token);
        if (!res.ok) {
          console.error("lot reorder failed", res.status);
        }
      }).catch(function (err) {
        if (loading && loading.hide) loading.hide(token);
        console.error("lot reorder failed", err);
      });
    },
  });
})();
