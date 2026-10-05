/* Прикрепление медиафайла-фото к лоту: кроп + watermark.
   Отправка: «Прикрепить» требует кроп ИЛИ явное «Без кропа» (no_crop=1).
   Через очередь (?queue=) после attach идёт следующий файл. */

(function () {
  var openBtn = document.getElementById("attach-crop-open");
  var stage = document.getElementById("crop-stage");
  var form = document.getElementById("attach-form");
  var confirmBtn = document.getElementById("crop-confirm");
  var cancelBtn = document.getElementById("crop-cancel");
  var noCropBtn = document.getElementById("attach-no-crop");
  var imageEl = document.getElementById("crop-image");
  if (!form) return;

  var cropper = null;
  var imageReady = false;
  var cropDone = false;
  var noCrop = false;

  function hidden(id) {
    return document.getElementById(id);
  }

  function setVal(id, value) {
    var el = hidden(id);
    if (el) el.value = value;
  }

  function clearCropInputs() {
    ["crop-x", "crop-y", "crop-w", "crop-h", "wm-x", "wm-y", "wm-width"].forEach(function (id) {
      var el = hidden(id);
      if (el) el.value = "";
    });
  }

  function showHint(text) {
    var hint = hidden("attach-crop-hint");
    if (!hint) {
      hint = document.createElement("p");
      hint.id = "attach-crop-hint";
      hint.className = "admin-error";
      form.insertBefore(hint, form.firstChild);
    }
    hint.textContent = text;
  }

  function openStage() {
    if (!stage || !imageEl) return;
    showHint("");
    stage.hidden = false;
    document.body.style.overflow = "hidden";
    if (!imageReady) {
      imageEl.addEventListener("load", onImageLoad, { once: true });
      imageEl.addEventListener("error", function () {
        closeStage();
        showHint("Не удалось показать превью для кропа. Нажмите «Без кропа» — файл возьмётся целиком, watermark по умолчанию.");
      }, { once: true });
    } else {
      ensureCropper();
    }
  }

  function closeStage() {
    if (!stage) return;
    stage.hidden = true;
    document.body.style.overflow = "";
  }

  function onImageLoad() {
    imageReady = true;
    ensureCropper();
  }

  function ensureCropper() {
    if (typeof Cropper === "undefined") {
      showHint("Cropper.js не загрузился. Нажмите «Без кропа» или обновите страницу.");
      return;
    }
    if (cropper) cropper.destroy();
    try {
      cropper = new Cropper(imageEl, {
        viewMode: 1,
        autoCropArea: 0.9,
        background: false,
        responsive: true,
        crop() {
          if (typeof window.__setCropCropper === "function") {
            window.__setCropCropper(cropper);
          }
          document.dispatchEvent(new CustomEvent("cropper:ready"));
        },
      });
    } catch (e) {
      cropper = null;
      showHint("Кроп недоступен: " + e.message);
    }
  }

  function applyNoCrop() {
    noCrop = true;
    cropDone = false;
    clearCropInputs();
    setVal("no_crop", "1");
    closeStage();
    if (cropper) {
      try { cropper.destroy(); } catch (e) { /* ignore */ }
      cropper = null;
    }
    showHint("Без кропа: файл уйдёт целиком, watermark по умолчанию.");
  }

  if (openBtn) {
    openBtn.addEventListener("click", function () {
      noCrop = false;
      setVal("no_crop", "");
      openStage();
    });
  }

  if (noCropBtn) {
    noCropBtn.addEventListener("click", function () {
      applyNoCrop();
    });
  }

  if (cancelBtn) {
    cancelBtn.addEventListener("click", function () {
      // «Без кропа / закрыть» — явный отказ от интерактивного кропа
      applyNoCrop();
    });
  }

  if (confirmBtn) {
    confirmBtn.addEventListener("click", function () {
      if (!cropper) {
        showHint("Cropper не готов. Нажмите «Без кропа» или дождитесь загрузки превью.");
        return;
      }
      var data = cropper.getData(true);
      setVal("crop-x", Math.max(0, data.x));
      setVal("crop-y", Math.max(0, data.y));
      setVal("crop-w", data.width);
      setVal("crop-h", data.height);
      cropDone = true;
      noCrop = false;
      setVal("no_crop", "");
      // WM-поля пишет admin-crop-wm.js на том же кнопке (writeInputs)
      closeStage();
      showHint("Кроп и watermark готовы — нажмите «Прикрепить».");
    });
  }

  if (form) {
    form.addEventListener("submit", function (e) {
      var isImage = form.getAttribute("data-kind") !== "video";
      if (!isImage) return;
      var cropFilled = hidden("crop-w") && hidden("crop-w").value
        && hidden("crop-x") && hidden("crop-x").value !== "";
      var noCropVal = hidden("no_crop") && hidden("no_crop").value === "1";
      if (!cropFilled && !noCropVal) {
        e.preventDefault();
        showHint("Для фото сначала выполните кроп с watermark или нажмите «Без кропа».");
        openStage();
        return;
      }
      // повторный submit
      if (form.dataset.submitting === "1") {
        e.preventDefault();
      }
    });
  }

  if (imageEl) {
    setTimeout(openStage, 80);
  }
})();