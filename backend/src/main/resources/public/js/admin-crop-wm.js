/* Watermark на кропе: drag + resize квадратным маркером + opacity.
   Дроби (wm_x, wm_y, wm_width) — относительно области кропа (выхода).
   ВАЖНО: writeInputs() обязан вызываться на «Готово» — иначе бэкенд
   не получит позицию и возьмёт default SE. */

(function () {
  var cropImage = document.getElementById("crop-image");
  var cropWrap = document.getElementById("crop-wrap");
  var stage = document.getElementById("crop-stage");
  var wmBox = document.getElementById("crop-wm");
  var wmHandle = document.getElementById("crop-wm-handle");
  var opacitySlider = document.getElementById("wm-opacity");
  var opacityVal = document.getElementById("wm-opacity-val");
  var xInput = document.getElementById("wm-x");
  var yInput = document.getElementById("wm-y");
  var wInput = document.getElementById("wm-width");
  var oInput = document.getElementById("wm-opacity-hidden");
  var confirmBtn = document.getElementById("crop-confirm");
  if (!cropImage || !wmBox || !stage) return;

  var defaultWidth = parseFloat(stage.getAttribute("data-wm-width") || "0.18");
  var defaultMargin = parseFloat(stage.getAttribute("data-wm-margin") || "0.02");
  var defaultOpacity = parseFloat(stage.getAttribute("data-wm-opacity") || "1");
  if (!(defaultWidth > 0)) defaultWidth = 0.18;
  if (!(defaultMargin >= 0)) defaultMargin = 0.02;
  if (!(defaultOpacity > 0)) defaultOpacity = 1;

  var state = { x: 0, y: 0, w: defaultWidth, o: defaultOpacity };
  var cropper = null;
  var mode = null;
  var start = null;

  window.__setCropCropper = function (c) {
    cropper = c;
    writeInputs();
    layoutFromState();
  };

  function getCropper() {
    if (cropper) return cropper;
    try {
      return cropImage.cropper || null;
    } catch (e) {
      return null;
    }
  }

  /** Rect кропа в CSS-пикселях внутри crop-wrap. */
  function cropRect() {
    var c = getCropper();
    if (!c) return null;
    var data;
    var canvas;
    var imgData;
    try {
      data = c.getData(true);
      canvas = c.getCanvasData();
      imgData = c.getImageData();
    } catch (e) {
      return null;
    }
    if (!data || !canvas || !imgData || !imgData.naturalWidth) return null;
    var sx = canvas.width / imgData.naturalWidth;
    var sy = canvas.height / imgData.naturalHeight;
    return {
      left: canvas.left + data.x * sx,
      top: canvas.top + data.y * sy,
      width: data.width * sx,
      height: data.height * sy,
    };
  }

  function wmPixelSize(cropW) {
    var img = wmBox.querySelector("img");
    var natW = (img && img.naturalWidth) || 780;
    var natH = (img && img.naturalHeight) || 208;
    var wPx = state.w * cropW;
    var hPx = natW ? (wPx / natW) * natH : wPx * 0.27;
    if (!(hPx > 0)) hPx = wPx * 0.27;
    return { w: wPx, h: hPx };
  }

  function layoutFromState() {
    var rect = cropRect();
    if (!rect || rect.width <= 1) {
      wmBox.hidden = true;
      return;
    }
    clampState();
    wmBox.hidden = false;
    var size = wmPixelSize(rect.width);
    var cropperEl = cropImage.parentElement || cropImage;
    var wrapBox = cropWrap.getBoundingClientRect();
    var cropperBox = cropperEl.getBoundingClientRect();
    var absLeft = (cropperBox.left - wrapBox.left) + rect.left + state.x * rect.width;
    var absTop = (cropperBox.top - wrapBox.top) + rect.top + state.y * rect.height;
    wmBox.style.left = absLeft + "px";
    wmBox.style.top = absTop + "px";
    wmBox.style.width = size.w + "px";
    wmBox.style.height = size.h + "px";
    var img = wmBox.querySelector("img");
    if (img) img.style.opacity = String(state.o);
  }

  function clampState() {
    state.w = Math.min(0.65, Math.max(0.05, state.w));
    state.x = Math.min(Math.max(0, 1 - state.w), Math.max(0, state.x));
    state.y = Math.min(0.92, Math.max(0, state.y));
    state.o = Math.min(1, Math.max(0.1, state.o));
  }

  function writeInputs() {
    if (xInput) xInput.value = state.x.toFixed(4);
    if (yInput) yInput.value = state.y.toFixed(4);
    if (wInput) wInput.value = state.w.toFixed(4);
    if (oInput) oInput.value = state.o.toFixed(3);
    if (opacitySlider && document.activeElement !== opacitySlider) {
      opacitySlider.value = String(Math.round(state.o * 100));
    }
    if (opacityVal) opacityVal.textContent = Math.round(state.o * 100) + "%";
  }

  function defaultSeState() {
    state.w = defaultWidth;
    state.o = defaultOpacity;
    state.x = Math.max(0, 1 - defaultWidth - defaultMargin);
    state.y = Math.max(0.05, 1 - defaultMargin - 0.22);
    clampState();
    writeInputs();
    layoutFromState();
  }

  function onDown(e, nextMode) {
    if (wmBox.hidden) return;
    e.preventDefault();
    e.stopPropagation();
    mode = nextMode;
    start = { x: e.clientX, y: e.clientY, wx: state.x, wy: state.y, ww: state.w };
    window.addEventListener("pointermove", onMove);
    window.addEventListener("pointerup", onUp);
  }

  function onMove(e) {
    if (!mode || !start) return;
    var rect = cropRect();
    if (!rect || rect.width <= 1) return;
    var dx = (e.clientX - start.x) / rect.width;
    var dy = (e.clientY - start.y) / rect.height;
    if (mode === "move") {
      state.x = start.wx + dx;
      state.y = start.wy + dy;
    } else {
      state.w = start.ww + dx;
    }
    clampState();
    writeInputs();
    layoutFromState();
  }

  function onUp() {
    mode = null;
    start = null;
    window.removeEventListener("pointermove", onMove);
    window.removeEventListener("pointerup", onUp);
  }

  wmBox.addEventListener("pointerdown", function (e) {
    if (e.target === wmHandle || (wmHandle && wmHandle.contains(e.target))) {
      onDown(e, "resize");
    } else {
      onDown(e, "move");
    }
  });

  if (opacitySlider) {
    opacitySlider.addEventListener("input", function () {
      state.o = parseInt(opacitySlider.value, 10) / 100;
      clampState();
      writeInputs();
      layoutFromState();
    });
  }

  cropImage.addEventListener("crop", function () {
    writeInputs();
    layoutFromState();
  });
  cropImage.addEventListener("ready", function () {
    defaultSeState();
  });
  document.addEventListener("cropper:ready", function () {
    writeInputs();
    layoutFromState();
  });

  // Готово: сначала зафиксировать wm-поля, потом submit (capture=true)
  if (confirmBtn) {
    confirmBtn.addEventListener("click", function () {
      writeInputs();
    }, true);
  }

  setInterval(function () {
    if (stage && !stage.hidden && !wmBox.hidden) {
      writeInputs();
      layoutFromState();
    }
  }, 400);

  var fileInput = document.getElementById("file-input");
  if (fileInput) {
    fileInput.addEventListener("change", function () {
      setTimeout(defaultSeState, 80);
    });
  }

  // поля должны быть заполнены сразу, даже если cropper ещё не ready
  writeInputs();
  layoutFromState();
})();
