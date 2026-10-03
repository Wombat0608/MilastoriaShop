/* Админка: drag-and-drop сортировка лотов.
   После drop шлём POST /admin/lots/reorder с id1,id2,… —
   бэкенд пишет lots.sort = 1..N (то же поле «Порядок» на форме). */

(function () {
  var grid = document.getElementById("lot-grid");
  if (!grid || typeof Sortable === "undefined") return;
  if (grid.getAttribute("data-sortable") !== "1") return;

  Sortable.create(grid, {
    animation: 150,
    ghostClass: "is-sort-ghost",
    chosenClass: "is-sort-chosen",
    handle: ".admin-lot-row",
    // клики по ссылкам/кнопкам не должны начинать drag
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

      fetch("/admin/lots/reorder", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
        body: "order=" + encodeURIComponent(ids),
        credentials: "same-origin",
      }).then(function (res) {
        if (!res.ok) {
          console.error("lot reorder failed", res.status);
        }
      });
    },
  });
})();
