/* Глобальный loading-modal для админки: «вертушка» на загрузках и submit форм.
   window.AdminLoading.show(message) / update(message) / hide()
   window.AdminLoading.post(url, options, message) — fetch + spinner + anti-double. */

(function () {
  if (window.AdminLoading) return;

  var KEY = "admin-loading-busy";
  var overlay = null;
  var box = null;
  var textEl = null;
  var spinnerEl = null;
  var depth = 0;
  var busyToken = null;

  function ensureDom() {
    if (overlay) return;
    overlay = document.createElement("div");
    overlay.className = "admin-loading";
    overlay.id = "admin-loading";
    overlay.hidden = true;
    overlay.setAttribute("role", "status");
    overlay.setAttribute("aria-live", "polite");
    overlay.setAttribute("aria-busy", "true");

    box = document.createElement("div");
    box.className = "admin-loading__box";

    spinnerEl = document.createElement("div");
    spinnerEl.className = "admin-loading__spinner";
    spinnerEl.setAttribute("aria-hidden", "true");

    textEl = document.createElement("div");
    textEl.className = "admin-loading__text";
    textEl.textContent = "Подождите…";

    box.appendChild(spinnerEl);
    box.appendChild(textEl);
    overlay.appendChild(box);
    document.body.appendChild(overlay);

    overlay.addEventListener("click", function (e) {
      // не даём «прокликать» мимо — модалка блокирует фон
      e.preventDefault();
    });
  }

  function show(message) {
    ensureDom();
    depth += 1;
    overlay.hidden = false;
    document.documentElement.classList.add("is-admin-loading");
    document.body.classList.add("is-admin-loading");
    if (message) {
      textEl.textContent = message;
    }
    return busyToken = Symbol("admin-loading");
  }

  function update(message) {
    ensureDom();
    if (message) textEl.textContent = message;
  }

  function hide(token) {
    if (!overlay) return;
    if (token && busyToken !== token) return;
    depth = Math.max(0, depth - 1);
    if (depth > 0) return;
    overlay.hidden = true;
    document.documentElement.classList.remove("is-admin-loading");
    document.body.classList.remove("is-admin-loading");
    busyToken = null;
  }

  function hideAll() {
    depth = 0;
    if (!overlay) return;
    overlay.hidden = true;
    document.documentElement.classList.remove("is-admin-loading");
    document.body.classList.remove("is-admin-loading");
    busyToken = null;
  }

  function isLoading() {
    return depth > 0;
  }

  /** POST/fetch с вертушкой. options — как у fetch. */
  function post(url, options, message) {
    var token = show(message || "Отправляю…");
    var opts = options || {};
    opts.method = opts.method || "POST";
    opts.credentials = opts.credentials || "same-origin";
    return fetch(url, opts)
      .then(function (res) {
        hide(token);
        return res;
      })
      .catch(function (err) {
        hide(token);
        throw err;
      });
  }

  function get(url, options, message) {
    var token = show(message || "Загружаю…");
    var opts = options || {};
    opts.method = opts.method || "GET";
    opts.credentials = opts.credentials || "same-origin";
    return fetch(url, opts)
      .then(function (res) {
        hide(token);
        return res;
      })
      .catch(function (err) {
        hide(token);
        throw err;
      });
  }

  window.AdminLoading = {
    show: show,
    update: update,
    hide: hide,
    hideAll: hideAll,
    isLoading: isLoading,
    post: post,
    get: get,
    KEY: KEY,
  };

  // страховка: если страница уходит (навигация) — вертушка не мешает
  window.addEventListener("pagehide", function () {
    hideAll();
  });
  document.addEventListener("visibilitychange", function () {
    if (document.visibilityState === "visible" && !isLoading()) {
      // no-op
    }
  });
})();
