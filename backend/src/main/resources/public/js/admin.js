/* Админка: подтверждения форм, slug, global loading-modal + anti-double submit.
   ВАЖНО: показываем вертушку только если submit НЕ отменили другие обработчики
   (e.defaultPrevented) — иначе модалка «залипает» и блокирует всю админку. */

(function () {
  function loadingApi() {
    return window.AdminLoading;
  }

  function isLoading() {
    var api = loadingApi();
    return !!(api && api.isLoading && api.isLoading());
  }

  function showLoading(message) {
    var api = loadingApi();
    if (api && api.show) return api.show(message || "Сохраняю…");
    return null;
  }

  function hideLoading() {
    var api = loadingApi();
    if (api && api.hideAll) api.hideAll();
  }

  function isNavigationalForm(form) {
    var method = (form.getAttribute("method") || "get").toLowerCase();
    if (method === "get") return true;
    if (form.hasAttribute("data-no-loading")) return true;
    var target = form.getAttribute("target");
    if (target && target !== "_self") return true;
    return false;
  }

  function defaultMessage(form) {
    if (form.dataset && form.dataset.loadingMessage) return form.dataset.loadingMessage;
    var file = form.querySelector('input[type="file"]');
    if (file && file.files && file.files.length > 0) {
      return "Загружаю файл(ы)…";
    }
    return "Сохраняю…";
  }

  function unlockForm(form) {
    if (!form) return;
    form.dataset.submitting = "0";
    form.classList.remove("is-submitting");
  }

  // ——— confirm + loading на обычных формах ———
  document.querySelectorAll("form").forEach(function (form) {
    form.addEventListener("submit", function (e) {
      // Другие обработчики уже отменили submit (нет лота, нет файлов и т.п.)
      if (e.defaultPrevented) {
        unlockForm(form);
        return;
      }

      if (form.dataset.confirm) {
        if (!window.confirm(form.dataset.confirm)) {
          e.preventDefault();
          unlockForm(form);
          return;
        }
      }

      if (isNavigationalForm(form)) return;

      if (form.dataset.submitting === "1" || isLoading()) {
        e.preventDefault();
        return;
      }

      form.dataset.submitting = "1";
      form.classList.add("is-submitting");
      var token = showLoading(defaultMessage(form));
      // если submit отменят «после» (редко) — снимаем на следующем кадре
      setTimeout(function () {
        if (e.defaultPrevented) {
          unlockForm(form);
          var api = loadingApi();
          if (api && api.hide && token) api.hide(token);
        }
      }, 0);
    });
  });

  window.addEventListener("pageshow", function (e) {
    if (e && e.persisted) {
      document.querySelectorAll("form.is-submitting").forEach(function (form) {
        unlockForm(form);
      });
      hideLoading();
    }
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
        filledOnce = true;
      }
      if (!target.dataset.userEdited || target.dataset.userEdited !== "1") {
        target.value = slugify(source.value);
      }
    });
    target.addEventListener("input", function () {
      target.dataset.userEdited = target.value ? "1" : "0";
      target.value = target.value
        .toLowerCase()
        .replace(/[^a-z0-9-]/g, "")
        .replace(/-{2,}/g, "-");
    });
    if (!target.value && source.value) {
      target.value = slugify(source.value);
    }
  }

  window.AdminFormBusy = {
    showLoading: showLoading,
    hideLoading: hideLoading,
    isLoading: isLoading,
  };
})();
