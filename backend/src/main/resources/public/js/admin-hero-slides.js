/* Админка: порядок слайдов hero (↑/↓) + сохранение order в форму. */

(function () {
  var form = document.getElementById("hero-slides-reorder");
  var grid = document.getElementById("hero-slides-grid");
  var orderInput = document.getElementById("hero-order");
  if (!form || !grid || !orderInput) return;

  function ids() {
    return Array.prototype.map
      .call(grid.querySelectorAll("[data-slide-id]"), function (el) {
        return el.getAttribute("data-slide-id");
      })
      .join(",");
  }

  function sync() {
    orderInput.value = ids();
  }

  function move(id, dir) {
    var row = grid.querySelector('[data-slide-id="' + id + '"]');
    if (!row) return;
    if (dir < 0) {
      var prev = row.previousElementSibling;
      if (prev) grid.insertBefore(row, prev);
    } else {
      var next = row.nextElementSibling;
      if (next) grid.insertBefore(next, row);
    }
    sync();
    form.submit();
  }

  grid.addEventListener("click", function (e) {
    var up = e.target.closest("[data-hero-up]");
    if (up) {
      move(up.getAttribute("data-hero-up"), -1);
      return;
    }
    var down = e.target.closest("[data-hero-down]");
    if (down) {
      move(down.getAttribute("data-hero-down"), 1);
    }
  });

  sync();
})();
