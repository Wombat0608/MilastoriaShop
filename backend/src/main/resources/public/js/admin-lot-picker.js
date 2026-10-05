/* Выбор лота: мгновенный клиентский фильтр списка (без select на 200+ строк). */

(function () {
  var filter = document.getElementById("attach-lot-filter");
  var list = document.getElementById("attach-lot-list");
  var lotIdInput = document.getElementById("attach-lot-id");
  var selectedLabel = document.getElementById("attach-lot-selected");
  var form = document.getElementById("attach-form");
  if (!list) return;

  function items() {
    return Array.prototype.slice.call(list.querySelectorAll(".lot-picker__item"));
  }

  function decodeSearch(raw) {
    try {
      return decodeURIComponent(raw || "");
    } catch (e) {
      return raw || "";
    }
  }

  function applyFilter(q) {
    var needle = (q || "").trim().toLowerCase();
    var visible = 0;
    items().forEach(function (item) {
      var hay = decodeSearch(item.getAttribute("data-search") || "").toLowerCase();
      var title = (item.querySelector(".lot-picker__title") || {}).textContent || "";
      var meta = (item.querySelector(".lot-picker__meta") || {}).textContent || "";
      var match = !needle
        || hay.indexOf(needle) !== -1
        || title.toLowerCase().indexOf(needle) !== -1
        || meta.toLowerCase().indexOf(needle) !== -1;
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
    var title = (item.querySelector(".lot-picker__title") || {}).textContent || "";
    items().forEach(function (other) {
      other.classList.toggle("is-selected", other === item);
      other.setAttribute("aria-selected", other === item ? "true" : "false");
    });
    if (lotIdInput) lotIdInput.value = id;
    if (selectedLabel) {
      selectedLabel.textContent = "Выбрано: " + title + " (#" + id + ")";
    }
    var submit = form ? form.querySelector("#attach-submit") : null;
    if (submit) submit.disabled = false;
    var videoSubmit = form ? form.querySelector('.admin-form__actions button[type="submit"]') : null;
    if (videoSubmit && videoSubmit.id !== "attach-submit") {
      // video-кнопка тоже enabled по умолчанию
    }
  }

  list.addEventListener("click", function (e) {
    var item = e.target.closest(".lot-picker__item");
    if (!item) return;
    e.preventDefault();
    selectItem(item);
  });

  if (filter) {
    var timer = null;
    filter.addEventListener("input", function () {
      clearTimeout(timer);
      timer = setTimeout(function () {
        applyFilter(filter.value);
      }, 80);
    });
    filter.addEventListener("keydown", function (e) {
      if (e.key === "Enter") {
        e.preventDefault();
        var first = items().filter(function (item) { return !item.hidden; })[0];
        if (first) selectItem(first);
      }
    });
  }

  if (form) {
    form.addEventListener("submit", function (e) {
      if (lotIdInput && !lotIdInput.value) {
        e.preventDefault();
        if (selectedLabel) {
          selectedLabel.textContent = "Сначала найдите и выберите лот";
        }
        if (filter) filter.focus();
      }
    });
  }

  // preselect
  var preId = filter ? filter.getAttribute("data-selected-lot-id") : "";
  if (preId) {
    var pre = list.querySelector('.lot-picker__item[data-lot-id="' + preId + '"]');
    if (pre) selectItem(pre);
  }

  if (filter) applyFilter(filter.value);
})();
