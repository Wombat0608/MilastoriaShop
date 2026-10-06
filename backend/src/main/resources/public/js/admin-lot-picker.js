/* Выбор лота: обычный список admin-lot-row + мгновенный фильтр.
   Клик по строке или кнопке «Выбрать» → hidden lot_id для attach-формы. */

(function () {
  var filter = document.getElementById("attach-lot-filter");
  var list = document.getElementById("attach-lot-list");
  var lotIdInput = document.getElementById("attach-lot-id");
  var selectedLabel = document.getElementById("attach-lot-selected");
  var form = document.getElementById("attach-form");
  if (!list) return;

  function items() {
    return Array.prototype.slice.call(list.querySelectorAll(".admin-lot-row[data-lot-id]"));
  }

  function applyFilter(q) {
    var needle = (q || "").trim().toLowerCase();
    var visible = 0;
    items().forEach(function (item) {
      var hay = (item.getAttribute("data-search") || "").toLowerCase();
      var match = !needle || hay.indexOf(needle) !== -1;
      item.hidden = !match;
      if (match) visible += 1;
    });
    var empty = list.querySelector(".lot-picker__empty");
    if (!empty) {
      empty = document.createElement("div");
      empty.className = "lot-picker__empty admin-empty";
      empty.textContent = "Ничего не найдено";
      list.appendChild(empty);
    }
    empty.hidden = visible > 0;
  }

  function selectItem(item) {
    var id = item.getAttribute("data-lot-id");
    var titleEl = item.querySelector(".admin-lot-row__title");
    var title = titleEl ? titleEl.textContent : ("#" + id);
    items().forEach(function (other) {
      other.classList.toggle("is-selected", other === item);
    });
    if (lotIdInput) lotIdInput.value = id;
    if (selectedLabel) {
      selectedLabel.innerHTML = "Выбрано: <strong></strong> (#" + id + ")";
      selectedLabel.querySelector("strong").textContent = title;
    }
  }

  list.addEventListener("click", function (e) {
    var pick = e.target.closest(".lot-picker__pick");
    if (pick) {
      var row = pick.closest(".admin-lot-row[data-lot-id]");
      if (row) selectItem(row);
      return;
    }
    var row = e.target.closest(".admin-lot-row[data-lot-id]");
    if (!row) return;
    if (e.target.closest("a, form, input, button:not(.lot-picker__pick)")) return;
    selectItem(row);
  });

  if (filter) {
    var timer = null;
    filter.addEventListener("input", function () {
      clearTimeout(timer);
      timer = setTimeout(function () {
        applyFilter(filter.value);
      }, 80);
    });
  }

  if (form) {
    form.addEventListener("submit", function (e) {
      if (lotIdInput && !lotIdInput.value) {
        e.preventDefault();
        if (selectedLabel) {
          selectedLabel.textContent = "Сначала выберите лот в списке";
        }
        if (filter) filter.focus();
      }
    });
  }

  var preId = filter ? filter.getAttribute("data-selected-lot-id") : "";
  if (preId) {
    var pre = list.querySelector('.admin-lot-row[data-lot-id="' + preId + '"]');
    if (pre) selectItem(pre);
  }

  if (filter) applyFilter(filter.value);
})();
