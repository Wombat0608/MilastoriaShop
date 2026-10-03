/* Админка: подтверждения форм, slug из названия.
   Кроп загрузки фото — отдельный admin-upload.js (только на странице фото). */

(function () {
  document.querySelectorAll("form[data-confirm]").forEach(function (f) {
    f.addEventListener("submit", function (e) {
      if (!window.confirm(f.dataset.confirm)) e.preventDefault();
    });
  });

  var MAP = {
    а: "a", б: "b", в: "v", г: "g", д: "d", е: "e", ё: "e",
    ж: "zh", з: "z", и: "i", й: "y", к: "k", л: "l", м: "m",
    н: "n", о: "o", п: "p", р: "r", с: "s", т: "t", у: "u",
    ф: "f", х: "h", ц: "c", ч: "ch", ш: "sh", щ: "sch",
    ъ: "", ы: "y", ь: "", э: "e", ю: "yu", я: "ya"
  };

  function slugify(text) {
    return String(text || "")
      .toLowerCase()
      .split("")
      .map(function (ch) {
        if (MAP[ch] !== undefined) return MAP[ch];
        if (/[a-z0-9]/.test(ch)) return ch;
        return "-";
      })
      .join("")
      .replace(/-{2,}/g, "-")
      .replace(/^-+|-+$/g, "");
  }

  var source = document.querySelector("[data-slug-source]");
  var target = document.querySelector("[data-slug-target]");
  if (source && target) {
    var filledOnce = false;
    source.addEventListener("input", function () {
      if (target.value && !filledOnce && source.dataset.slugTouched !== "1") {
        // первый ввод заполняет slug; дальше пользователь может править сам
        filledOnce = true;
      }
      if (!target.dataset.userEdited || target.dataset.userEdited !== "1") {
        target.value = slugify(source.value);
      }
    });
    target.addEventListener("input", function () {
      target.dataset.userEdited = target.value ? "1" : "0";
      // нормализуем: латиница/цифры/дефис
      target.value = target.value
        .toLowerCase()
        .replace(/[^a-z0-9-]/g, "")
        .replace(/-{2,}/g, "-");
    });
    if (!target.value && source.value) {
      target.value = slugify(source.value);
    }
  }
})();
