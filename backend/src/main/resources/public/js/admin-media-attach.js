/* Прикрепление медиафайла-фото к лоту: открывает кроп+watermark
   (admin-crop-wm.js), после «Готово» уходят crop-поля; «Без кропа»
   очищает их (бэкенд возьмёт full + default WM). */

(function () {
  var openBtn = document.getElementById("attach-crop-open");
  var stage = document.getElementById("crop-stage");
  var form = document.getElementById("attach-form");
  var confirmBtn = document.getElementById("crop-confirm");
  var cancelBtn = document.getElementById("crop-cancel");
  var imageEl = document.getElementById("crop-image");
  if (!form || !stage) return;

  var cropper = null;
  var imageReady = false;

  function clearCropInputs() {
    ["crop-x", "crop-y", "crop-w", "crop-h", "wm-x", "wm-y", "wm-width"].forEach(function (id) {
      var el = document.getElementById(id);
      if (el) el.value = "";
    });
  }

  function openStage() {
    if (!imageEl) return;
    stage.hidden = false;
    document.body.style.overflow = "hidden";
    if (!imageReady) {
      imageEl.addEventListener("load", onImageLoad, { once: true });
      // HEIC/AVIF в десктопных браузерах может не декодироваться
      imageEl.addEventListener("error", function () {
        closeStage();
        var hint = document.getElementById("attach-crop-hint");
        if (!hint) {
          hint = document.createElement("p");
          hint.id = "attach-crop-hint";
          hint.className = "admin-hint";
          form.insertBefore(hint, form.firstChild);
        }
        hint.textContent = "Не удалось показать превью для кропа (формат может не открываться в браузере). "
          + "Прикрепите без кропа — файл возьмётся целиком, watermark по умолчанию.";
      }, { once: true });
    } else {
      ensureCropper();
    }
  }

  function closeStage() {
    stage.hidden = true;
    document.body.style.overflow = "";
  }

  function onImageLoad() {
    imageReady = true;
    ensureCropper();
  }

  function ensureCropper() {
    if (cropper) cropper.destroy();
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
  }

  if (openBtn) {
    openBtn.addEventListener("click", function () {
      openStage();
    });
  }

  if (cancelBtn) {
    cancelBtn.addEventListener("click", function () {
      clearCropInputs();
      closeStage();
      if (cropper) {
        cropper.destroy();
        cropper = null;
      }
    });
  }

  if (confirmBtn) {
    confirmBtn.addEventListener("click", function () {
      if (!cropper) return;
      var data = cropper.getData(true);
      var x = document.getElementById("crop-x");
      var y = document.getElementById("crop-y");
      var w = document.getElementById("crop-w");
      var h = document.getElementById("crop-h");
      if (x) x.value = Math.max(0, data.x);
      if (y) y.value = Math.max(0, data.y);
      if (w) w.value = data.width;
      if (h) h.value = data.height;
      closeStage();
    });
  }

  // для фото кроп+watermark предлагаем сразу (требование: «должно снова предлагаться»)
  if (imageEl) {
    setTimeout(openStage, 50);
  }
})();