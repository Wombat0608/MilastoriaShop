/* Админка: словари-крошки (Повод / Материалы / Теги).
   Lookup: ввод + datalist (Enter/«+»). Значения лежат в hidden-полях
   name=occasions|fabrics|tags через «|». Крестик убирает крошку. */

(function () {
  function parseValues(raw) {
    if (!raw) return [];
    return String(raw)
      .split("|")
      .map(function (s) { return s.trim(); })
      .filter(Boolean);
  }

  function normalize(value) {
    return String(value || "").replace(/\s+/g, " ").trim();
  }

  document.querySelectorAll("[data-dict-field]").forEach(function (root) {
    var hidden = root.querySelector("[data-dict-values]");
    var chipsEl = root.querySelector("[data-dict-chips]");
    var input = root.querySelector("[data-dict-input]");
    var addBtn = root.querySelector("[data-dict-add]");
    if (!hidden || !chipsEl) return;

    var entries = parseValues(hidden.value);

    function render() {
      chipsEl.innerHTML = "";
      entries.forEach(function (value, idx) {
        var chip = document.createElement("span");
        chip.className = "dict-chip";
        chip.appendChild(document.createTextNode(value));
        var del = document.createElement("button");
        del.type = "button";
        del.className = "dict-chip__x";
        del.setAttribute("aria-label", "Убрать " + value);
        del.textContent = "×";
        del.addEventListener("click", function () {
          entries.splice(idx, 1);
          hidden.value = entries.join("|");
          render();
        });
        chip.appendChild(del);
        chipsEl.appendChild(chip);
      });
      hidden.value = entries.join("|");
    }

    function addValue(raw) {
      var value = normalize(raw);
      if (!value) return;
      var exists = entries.some(function (e) {
        return e.toLowerCase() === value.toLowerCase();
      });
      if (!exists) entries.push(value);
      if (input) input.value = "";
      render();
    }

    if (addBtn) addBtn.addEventListener("click", function () { addValue(input && input.value); });
    if (input) {
      input.addEventListener("keydown", function (e) {
        if (e.key === "Enter") {
          e.preventDefault();
          addValue(input.value);
        }
      });
    }

    render();
  });
})();
