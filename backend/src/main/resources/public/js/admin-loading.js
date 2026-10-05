/* Глобальный loading-modal для админки: «вертушка» на загрузках и submit форм.
   window.AdminLoading.show(message) → token
   window.AdminLoading.update(message)
   window.AdminLoading.hide(token) / hideAll()
   Страховка: авто-hide через 90с, чтобы страница не «залипла». */

(function () {
  if (window.AdminLoading) return;

  var overlay = null;
  var box = null;
  var textEl = null;
  var depth = 0;
  var tokenSeq = 0;
  var activeTokens = {};
  var safetyTimer = null;
  var SAFETY_MS = 90000;

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

    var spinnerEl = document.createElement("div");
    spinnerEl.className = "admin-loading__spinner";
    spinnerEl.setAttribute("aria-hidden", "true");

    textEl = document.createElement("div");
    textEl.className = "admin-loading__text";
    textEl.textContent = "Подождите…";

    box.appendChild(spinnerEl);
    box.appendChild(textEl);
    overlay.appendChild(box);
    document.body.appendChild(overlay);
  }

  function clearSafety() {
    if (safetyTimer) {
      clearTimeout(safetyTimer);
      safetyTimer = null;
    }
  }

  function armSafety() {
    clearSafety();
    safetyTimer = setTimeout(function () {
      // если «вертушка» забыли снять — снимаем сами
      hideAll();
    }, SAFETY_MS);
  }

  function show(message) {
    ensureDom();
    tokenSeq += 1;
    var token = "al-" + tokenSeq;
    activeTokens[token] = true;
    depth += 1;
    overlay.hidden = false;
    document.documentElement.classList.add("is-admin-loading");
    document.body.classList.add("is-admin-loading");
    if (message && textEl) {
      textEl.textContent = message;
    }
    armSafety();
    return token;
  }

  function update(message) {
    ensureDom();
    if (message && textEl) textEl.textContent = message;
  }

  function hide(token) {
    if (!overlay) return;
    if (token && activeTokens[token]) {
      delete activeTokens[token];
      depth = Math.max(0, depth - 1);
    } else if (token && !activeTokens[token]) {
      return;
    } else {
      depth = Math.max(0, depth - 1);
    }
    if (depth > 0 && Object.keys(activeTokens).length > 0) return;
    // если токенов не осталось — прячем
    if (Object.keys(activeTokens).length === 0) {
      depth = 0;
      overlay.hidden = true;
      document.documentElement.classList.remove("is-admin-loading");
      document.body.classList.remove("is-admin-loading");
      clearSafety();
    }
  }

  function hideAll() {
    activeTokens = {};
    depth = 0;
    clearSafety();
    if (!overlay) return;
    overlay.hidden = true;
    document.documentElement.classList.remove("is-admin-loading");
    document.body.classList.remove("is-admin-loading");
  }

  function isLoading() {
    return depth > 0 && overlay && !overlay.hidden;
  }

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
  };

  window.addEventListener("pagehide", function () {
    hideAll();
  });
  window.addEventListener("pageshow", function (e) {
    if (e && e.persisted) hideAll();
  });
  document.addEventListener("visibilitychange", function () {
    if (document.visibilityState === "visible" && isLoading()) {
      // страница снова видна — сбрасываем только если модалка «голодает»
    }
  });
})();
