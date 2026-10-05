/* Медиатека: множественный выбор файлов + прикрепление к лоту.
   Submit формы — через AdminLoading (см. admin.js); здесь только синхронизация. */

(function () {
  var grid = document.getElementById("media-grid");
  var bar = document.getElementById("media-bulk-bar");
  var countEl = document.getElementById("media-bulk-count");
  var selectAll = document.getElementById("media-select-all");
  var selectNone = document.getElementById("media-select-none");
  var batchForm = document.getElementById("media-batch-form");
  var batchIdsField = document.getElementById("media-batch-ids");
  var batchSubmit = document.getElementById("media-batch-submit");
  if (!grid || !bar || !batchForm) return;

  var isPost = (batchForm.method || "get").toLowerCase() === "post";

  function checks() {
    return Array.prototype.slice.call(grid.querySelectorAll(".media-check"));
  }

  function selectedIds() {
    return checks()
      .filter(function (c) { return c.checked; })
      .map(function (c) { return c.value; });
  }

  function sync() {
    var ids = selectedIds();
    var n = ids.length;
    bar.hidden = checks().length === 0;
    if (countEl) {
      countEl.textContent = n === 0 ? "0 выбрано" : n + " выбрано";
    }
    if (selectAll) {
      var all = checks();
      var checked = all.filter(function (c) { return c.checked; }).length;
      selectAll.checked = all.length > 0 && checked === all.length;
      selectAll.indeterminate = checked > 0 && checked < all.length;
    }
    if (batchSubmit) {
      batchSubmit.disabled = n === 0;
      batchSubmit.textContent = n === 0
        ? "Сначала отметьте файлы"
        : (isPost
          ? "Прикрепить выбранные к лоту (" + n + ")"
          : "Выбрать лот и прикрепить (" + n + ")");
    }
    Array.prototype.slice.call(batchForm.querySelectorAll("input[data-batch-id]"))
      .forEach(function (el) { el.remove(); });
    if (!isPost && batchIdsField) {
      batchIdsField.value = ids.join(",");
    }
    if (isPost) {
      ids.forEach(function (id) {
        var input = document.createElement("input");
        input.type = "hidden";
        input.name = "media_ids";
        input.value = id;
        input.setAttribute("data-batch-id", "1");
        batchForm.appendChild(input);
      });
    }
  }

  grid.addEventListener("change", function (e) {
    if (e.target && e.target.classList && e.target.classList.contains("media-check")) {
      sync();
    }
  });

  if (selectAll) {
    selectAll.addEventListener("change", function () {
      checks().forEach(function (c) { c.checked = selectAll.checked; });
      sync();
    });
  }

  if (selectNone) {
    selectNone.addEventListener("click", function () {
      checks().forEach(function (c) { c.checked = false; });
      sync();
    });
  }

  batchForm.addEventListener("submit", function (e) {
    sync();
    var ids = selectedIds();
    if (!ids.length) {
      e.preventDefault();
      // важно: НЕ показываем вертушку — иначе admin.js её оставит
      return;
    }
  });

  grid.addEventListener("click", function (e) {
    if (e.target.closest("a, button, label, input, form")) return;
    var card = e.target.closest(".media-card");
    if (!card) return;
    var box = card.querySelector(".media-check");
    if (!box) return;
    box.checked = !box.checked;
    sync();
  });

  sync();
})();
