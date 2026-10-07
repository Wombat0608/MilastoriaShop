/* Прикрепление медиафайла-фото к лоту: кроп + watermark.
   Отправка: «Прикрепить» требует кроп ИЛИ явное «Без кропа» (no_crop=1).
   Через очередь (?queue=) после attach идёт следующий файл.
   ВАЖНО: если <img> уже complete (кэш) — сразу инициализируем Cropper,
   не ждём событие load (иначе рамка/WM «пропадают»). */

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
  var loadingHooked = false;

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
    hint.textContent = text || "";
    hint.hidden = !text;
  }

  /** Подсказка + кнопка «Прикрепить» прямо в сообщении (без скролла к форме). */
  function showHintWithSubmit(text) {
    var hint = hidden("attach-crop-hint");
    if (!hint) {
      hint = document.createElement("p");
      hint.id = "attach-crop-hint";
      hint.className = "admin-notice";
      form.insertBefore(hint, form.firstChild);
    }
    hint.textContent = "";
    hint.hidden = false;
    hint.appendChild(document.createTextNode(text + " "));
    var btn = document.createElement("button");
    btn.type = "submit";
    btn.className = "btn btn--fill";
    btn.textContent = "Прикрепить";
    hint.appendChild(btn);
    var picker = document.getElementById("attach-lot-picker");
    if (picker && picker.scrollIntoView) {
      picker.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }

  function lotSelected() {
    var el = document.getElementById("attach-lot-id");
    return !!(el && el.value);
  }

  /**
   * Лот уже выбран → сразу submit (следующий кроп в очереди).
   * Иначе — подсказка с кнопкой «Прикрепить» в самом сообщении.
   */
  function finishOrPrompt(nextText) {
    if (lotSelected()) {
      showHint(nextText || "Кроп и watermark готовы — прикрепляю…");
      if (form.requestSubmit) form.requestSubmit();
      else form.submit();
      return;
    }
    showHintWithSubmit(nextText || "Кроп и watermark готовы. Выберите лот и нажмите «Прикрепить».");
  }

  function closeStage() {
    if (!stage) return;
    stage.hidden = true;
    document.body.style.overflow = "";
  }

  function ensureCropper() {
    if (typeof Cropper === "undefined") {
      showHint("Cropper.js не загрузился. Обновите страницу (Ctrl+Shift+R) или нажмите «Без кропа».");
      return;
    }
    if (!imageEl) return;
    try {
      if (cropper) cropper.destroy();
      cropper = new Cropper(imageEl, {
        viewMode: 1,
        autoCropArea: 0.9,
        background: false,
        responsive: true,
        ready() {
          if (typeof window.__setCropCropper === "function") {
            window.__setCropCropper(cropper);
          }
          document.dispatchEvent(new CustomEvent("cropper:ready"));
        },
        crop() {
          if (typeof window.__setCropCropper === "function") {
            window.__setCropCropper(cropper);
          }
        },
      });
      // страховка: WM-раскладка и через ready Cropper, и сразу после new
      setTimeout(function () {
        if (typeof window.__setCropCropper === "function" && cropper) {
          window.__setCropCropper(cropper);
        }
        document.dispatchEvent(new CustomEvent("cropper:ready"));
      }, 50);
    } catch (e) {
      cropper = null;
      showHint("Кроп недоступен: " + e.message);
    }
  }

  function onImageLoad() {
    imageReady = true;
    ensureCropper();
  }

  function hookImageLoad() {
    if (!imageEl || loadingHooked) return;
    loadingHooked = true;
    imageEl.addEventListener("load", onImageLoad, { once: true });
    imageEl.addEventListener("error", function () {
      closeStage();
      showHint("Не удалось показать превью для кропа. Нажмите «Без кропа» — файл возьмётся целиком, watermark по умолчанию.");
    }, { once: true });
  }

  function openStage() {
    if (!stage || !imageEl) return;
    showHint("");
    stage.hidden = false;
    document.body.style.overflow = "hidden";

    // уже загружено (кэш / complete) — load не придёт
    if (imageEl.complete && imageEl.naturalWidth > 0) {
      imageReady = true;
      ensureCropper();
      return;
    }
    if (imageReady) {
      ensureCropper();
      return;
    }
    hookImageLoad();
    // если к моменту hook картинка уже complete
    if (imageEl.complete && imageEl.naturalWidth > 0) {
      onImageLoad();
    }
  }

  function applyNoCrop() {
    clearCropInputs();
    setVal("no_crop", "1");
    closeStage();
    if (cropper) {
      try { cropper.destroy(); } catch (e) { /* ignore */ }
      cropper = null;
    }
    finishOrPrompt("Без кропа: файл уйдёт целиком, watermark по умолчанию.");
  }

  if (openBtn) {
    openBtn.addEventListener("click", function () {
      setVal("no_crop", "");
      openStage();
    });
  }

  if (noCropBtn) {
    noCropBtn.addEventListener("click", applyNoCrop);
  }

  if (cancelBtn) {
    cancelBtn.addEventListener("click", applyNoCrop);
  }

  if (confirmBtn) {
    confirmBtn.addEventListener("click", function () {
      if (!cropper) {
        showHint("Cropper не готов. Нажмите «Без кропа» или дождитесь загрузки превью.");
        openStage();
        return;
      }
      var data = cropper.getData(true);
      setVal("crop-x", Math.max(0, data.x));
      setVal("crop-y", Math.max(0, data.y));
      setVal("crop-w", data.width);
      setVal("crop-h", data.height);
      setVal("no_crop", "");
      // WM-поля пишет admin-crop-wm.js (capture=true на той же кнопке)
      closeStage();
      // лот уже выбран → сразу attach, без промежуточного «нажмите Прикрепить»
      finishOrPrompt("Кроп и watermark готовы.");
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
      if (form.dataset.submitting === "1") {
        e.preventDefault();
      }
    });
  }

  if (imageEl) {
    setTimeout(openStage, 30);
  }
})();
