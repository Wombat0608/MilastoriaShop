/* Медиатека: выбор файлов пачкой → последовательный кроп фото (skip) →
   POST по одному файлу. Вертушка — window.AdminLoading.
   Watermark здесь НЕ предлагается. */

(function () {
  var input = document.getElementById("media-files");
  if (!input) return;

  var loading = window.AdminLoading;
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
  var loadingActive = false;

  function isImage(file) {
    return (file.type && file.type.indexOf("image/") === 0) || /\.(jpe?g|png|gif|webp|heic|heif|avif|bmp)$/i.test(file.name);
  }

  function setStatus(text) {
    if (!statusEl) return;
    statusEl.hidden = !text;
    statusEl.textContent = text || "";
  }

  function showLoading(text) {
    if (!loading) return;
    if (!loadingActive) {
      loading.show(text || "Загружаю…");
      loadingActive = true;
    } else {
      loading.update(text);
    }
  }

  function hideLoading() {
    if (!loading || !loadingActive) return;
    loading.hideAll();
    loadingActive = false;
  }

  function closeStage() {
    if (!stage) return;
    stage.hidden = true;
    document.body.style.overflow = "";
    if (cropper) {
      cropper.destroy();
      cropper = null;
    }
    cropFailed = false;
  }

  function openCrop(file) {
    cropFailed = false;
    if (cropImage) {
      var url = URL.createObjectURL(file);
      cropImage.onerror = function () {
        cropFailed = true;
        setStatus("Файл не открывается в браузере для кропа — загружаю целиком: " + file.name);
        showLoading("Загружаю " + file.name + "…");
        uploadOne(file, null)
          .then(function () {
            index += 1;
            busy = false;
            processNext();
          })
          .catch(function (err) {
            index += 1;
            busy = false;
            setStatus("Ошибка (" + file.name + "): " + err.message);
            processNext();
          });
      };
      cropImage.src = url;
    }
    if (stage) {
      stage.hidden = false;
      document.body.style.overflow = "hidden";
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
      return res.json().then(function (data) {
        if (!res.ok || !data.ok) {
          throw new Error((data && data.error) || ("HTTP " + res.status));
        }
        return data;
      });
    });
  }

  function processNext() {
    if (busy) return;
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
      setStatus("Фото " + n + " из " + total + ": " + file.name + " — кроп или пропуск");
      openCrop(file);
      return;
    }

    setStatus("Видео " + n + " из " + total + ": " + file.name + "…");
    showLoading("Загружаю видео " + n + "/" + total + ": " + file.name + "…");
    uploadOne(file, null)
      .then(function () {
        index += 1;
        processNext();
      })
      .catch(function (err) {
        index += 1;
        setStatus("Ошибка (" + file.name + "): " + err.message);
        processNext();
      });
  }

  if (cropImage) {
    cropImage.addEventListener("load", function () {
      if (cropFailed) return;
      if (cropper) cropper.destroy();
      cropper = new Cropper(cropImage, {
        viewMode: 1,
        autoCropArea: 0.9,
        background: false,
        responsive: true,
      });
    });
  }

  if (confirmBtn) {
    confirmBtn.addEventListener("click", function () {
      if (cropFailed) return;
      if (busy && !cropper) return;
      var file = queue[index];
      var crop = null;
      if (cropper) {
        var data = cropper.getData(true);
        crop = { x: data.x, y: data.y, width: data.width, height: data.height };
      }
      busy = true;
      closeStage();
      setStatus("Загружаю " + (index + 1) + "/" + queue.length + ": " + file.name);
      showLoading("Загружаю " + (index + 1) + "/" + queue.length + ": " + file.name);
      uploadOne(file, crop)
        .then(function () {
          index += 1;
          busy = false;
          processNext();
        })
        .catch(function (err) {
          index += 1;
          busy = false;
          setStatus("Ошибка (" + file.name + "): " + err.message);
          processNext();
        });
    });
  }

  if (skipBtn) {
    skipBtn.addEventListener("click", function () {
      if (cropFailed) return;
      var file = queue[index];
      busy = true;
      closeStage();
      setStatus("Пропускаю кроп, загружаю " + (index + 1) + "/" + queue.length + ": " + file.name);
      showLoading("Загружаю " + (index + 1) + "/" + queue.length + ": " + file.name);
      uploadOne(file, null)
        .then(function () {
          index += 1;
          busy = false;
          processNext();
        })
        .catch(function (err) {
          index += 1;
          busy = false;
          setStatus("Ошибка (" + file.name + "): " + err.message);
          processNext();
        });
    });
  }

  input.addEventListener("change", function () {
    var files = Array.prototype.slice.call(input.files || []);
    if (!files.length) return;
    if (busy) {
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
