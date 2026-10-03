/* Фильтр разделов: «Все разделы» + чекбоксы, submit сразу при изменении. */

(function () {
  var form = document.getElementById("admin-filter-form");
  if (!form) return;

  var allBox = document.getElementById("cat-all");
  var catBoxes = form.querySelectorAll('input[name="categories"]');
  var qInput = form.querySelector("#admin-q");

  function selectedCats() {
    var ids = [];
    catBoxes.forEach(function (box) {
      if (box.checked) ids.push(box.value);
    });
    return ids;
  }

  function apply() {
    var url = new URL(window.location.pathname, window.location.origin);
    var q = qInput ? qInput.value.trim() : "";
    if (q) url.searchParams.set("q", q);
    selectedCats().forEach(function (id) {
      url.searchParams.append("categories", id);
    });
    window.location.href = url.toString();
  }

  if (allBox) {
    allBox.addEventListener("change", function () {
      if (allBox.checked) {
        catBoxes.forEach(function (box) { box.checked = false; });
        apply();
      } else if (selectedCats().length === 0) {
        // сняли «Все» и ничего не выбрано — оставить без фильтра не выйдет, включаем обратно
        allBox.checked = true;
      }
    });
  }

  catBoxes.forEach(function (box) {
    box.addEventListener("change", function () {
      if (allBox) {
        allBox.checked = selectedCats().length === 0;
      }
      apply();
    });
  });

  if (qInput) {
    var timer = null;
    qInput.addEventListener("input", function () {
      clearTimeout(timer);
      timer = setTimeout(apply, 400);
    });
    qInput.addEventListener("keydown", function (e) {
      if (e.key === "Enter") {
        e.preventDefault();
        clearTimeout(timer);
        apply();
      }
    });
  }
})();
