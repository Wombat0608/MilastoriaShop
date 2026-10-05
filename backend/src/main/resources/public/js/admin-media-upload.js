/* Медиатека: выбор файлов пачкой → кроп фото (или сразу upload) → POST.
   Вертушка (AdminLoading) — только на время сетевого запроса.
   Кроп-экран показывается БЕЗ модалки, иначе overlay перекрывает UI.
   После каждого файла модалка снимается (hideLoading). */

(function () {
  var input = document.getElementById("media-files");
  if (!input) return;

  var statusEl = document.getElementById("media-upload-status");
  var stage = document.getElementById("media-crop-stage");
  var cropImage = document.getElementById("media-crop-image");
  var confirmBtn = document.getElementById("media-crop-confirm");
  var skipBtn = document.getElementById("media-crop-skip");

  var queue = [];
  var index = 0;
  var cropper = null;
  var busy = false;
  var cropFailed = false;
  var loadingToken = null;
  var uploading = false;

  function api() {
    return window.AdminLoading;
  }

  function isImage(file) {
    return (file.type && file.type.indexOf("image/") === 0)
      || /\.(jpe?g|png|gif|webp|heic|heif|avif|bmp)$/i.test(file.name);
  }

  function setStatus(text) {
    if (!statusEl) return;
    statusEl.hidden = !text;
    statusEl.textContent = text || "";
  }

  function showLoading(text) {
    var a = api();
    if (!a || !a.show) return;
    if (!loadingToken) {
      loadingToken = a.show(text || "Загружаю…");
    } else {
      a.update(text);
    }
  }

  function hideLoading() {
    var a = api();
    if (!a) {
      loadingToken = null;
      return;
    }
    if (loadingToken && a.hide) {
      a.hide(loadingToken);
    } else if (a.hideAll) {
      a.hideAll();
    }
    loadingToken = null;
  }

  function closeStage() {
    if (!stage) return;
    stage.hidden = true;
    document.body.style.overflow = "";
    if (cropper) {
      try { cropper.destroy(); } catch (e) { /* ignore */ }
      cropper = null;
    }
  }

  function uploadOne(file, crop) {
    var form = new FormData();
    form.append("file", file, file.name);
    if (crop && crop.width > 0 && crop.height > 0) {
      form.append("x", String(Math.max(0, Math.round(crop.x))));
      form.append("y", String(Math.max(0, Math.round(crop.y))));
      form.append("width", String(Math.round(crop.width)));
      form.append("height", String(Math.round(crop.height)));
    }
    return fetch("/admin/media/upload", {
      method: "POST",
      body: form,
      credentials: "same-origin",
    }).then(function (res) {
      return res.text().then(function (text) {
        var data = {};
        try { data = text ? JSON.parse(text) : {}; } catch (e) { data = { raw: text }; }
        if (!res.ok || data.ok === false) {
          throw new Error((data && data.error) || ("HTTP " + res.status));
        }
        return data;
      });
    });
  }

  function finishFile(err) {
    uploading = false;
    busy = false;
    // модалка всегда закрывается между файлами — иначе перекрывает кроп следующего
    hideLoading();
    index += 1;
    if (err) {
      var failed = queue[index - 1];
      setStatus("Ошибка (" + (failed && failed.name || "?") + "): " + err.message);
    }
    processNext();
  }

  function openCrop(file) {
    cropFailed = false;
    closeStage();
    // интерактивный кроп — без «вертушки»
    hideLoading();

    if (typeof Cropper === "undefined") {
      setStatus("Cropper недоступен — загружаю без кропа: " + file.name);
      showLoading("Загружаю " + file.name + "…");
      uploading = true;
      uploadOne(file, null)
        .then(function () { finishFile(null); })
        .catch(function (err) { finishFile(err); });
      return;
    }

    if (stage) {
      stage.hidden = false;
      document.body.style.overflow = "hidden";
    }
    if (!cropImage) {
      setStatus("Нет превью — загружаю целиком: " + file.name);
      showLoading("Загружаю " + file.name + "…");
      uploading = true;
      uploadOne(file, null)
        .then(function () { finishFile(null); })
        .catch(function (err) { finishFile(err); });
      return;
    }

    cropImage.onerror = function () {
      cropFailed = true;
      setStatus("Файл не открывается в браузере для кропа — загружаю целиком: " + file.name);
      showLoading("Загружаю " + file.name + "…");
      uploading = true;
      uploadOne(file, null)
        .then(function () { finishFile(null); })
        .catch(function (err) { finishFile(err); });
    };
    cropImage.onload = function () {
      if (cropFailed) return;
      try {
        if (cropper) cropper.destroy();
        cropper = new Cropper(cropImage, {
          viewMode: 1,
          autoCropArea: 0.9,
          background: false,
          responsive: true,
        });
      } catch (e) {
        cropper = null;
        setStatus("Кроп недоступен — нажмите «Пропустить»");
      }
    };
    var url = URL.createObjectURL(file);
    cropImage.src = url;
  }

  function processNext() {
    if (uploading) return;
    if (index >= queue.length) {
      busy = false;
      setStatus("Готово. Обновляю список…");
      showLoading("Готово — обновляю список…");
      window.location.reload();
      return;
    }

    var file = queue[index];
    var n = index + 1;
    var total = queue.length;

    if (isImage(file)) {
      busy = true;
      hideLoading(); // кроп следующего файла — без overlay
      setStatus("Фото " + n + " из " + total + ": " + file.name + " — кроп или пропуск");
      openCrop(file);
      return;
    }

    setStatus("Видео " + n + " из " + total + ": " + file.name + "…");
    showLoading("Загружаю видео " + n + "/" + total + ": " + file.name + "…");
    busy = true;
    uploading = true;
    uploadOne(file, null)
      .then(function () { finishFile(null); })
      .catch(function (err) { finishFile(err); });
  }

  function submitUpload(crop) {
    if (uploading) return;
    var file = queue[index];
    if (!file) return;
    busy = true;
    uploading = true;
    closeStage();
    var label = crop ? "Загружаю " : "Загружаю целиком ";
    setStatus(label + (index + 1) + "/" + queue.length + ": " + file.name);
    showLoading(label + (index + 1) + "/" + queue.length + ": " + file.name);
    uploadOne(file, crop)
      .then(function () { finishFile(null); })
      .catch(function (err) { finishFile(err); });
  }

  if (confirmBtn) {
    confirmBtn.addEventListener("click", function () {
      if (uploading) return;
      var crop = null;
      if (cropper) {
        try {
          var data = cropper.getData(true);
          crop = { x: data.x, y: data.y, width: data.width, height: data.height };
        } catch (e) {
          crop = null;
        }
      }
      submitUpload(crop);
    });
  }

  if (skipBtn) {
    skipBtn.addEventListener("click", function () {
      if (uploading) return;
      submitUpload(null);
    });
  }

  input.addEventListener("change", function () {
    var files = Array.prototype.slice.call(input.files || []);
    if (!files.length) return;
    if (busy || uploading) {
      setStatus("Идёт загрузка — дождитесь завершения");
      input.value = "";
      return;
    }
    queue = files;
    index = 0;
    busy = true;
    setStatus("Выбрано файлов: " + files.length + " — начинаю");
    processNext();
    input.value = "";
  });
})();
