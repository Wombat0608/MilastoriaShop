/* Кроп обложки раздела: выбор файла → кроп без watermark → submit. */

(function () {
  var fileInput = document.getElementById("cover-file");
  var stage = document.getElementById("cover-crop-stage");
  var cropImage = document.getElementById("cover-crop-image");
  var confirmBtn = document.getElementById("cover-crop-confirm");
  var cancelBtn = document.getElementById("cover-crop-cancel");
  var form = document.getElementById("category-form");
  var thumb = document.getElementById("cover-thumb");
  var xInput = document.getElementById("cover-x");
  var yInput = document.getElementById("cover-y");
  var wInput = document.getElementById("cover-w");
  var hInput = document.getElementById("cover-h");
  if (!fileInput || !stage || !form) return;

  var cropper = null;

  function closeStage() {
    stage.hidden = true;
    document.body.style.overflow = "";
    if (cropper) {
      cropper.destroy();
      cropper = null;
    }
  }

  fileInput.addEventListener("change", function () {
    var file = fileInput.files[0];
    if (!file) return;
    var url = URL.createObjectURL(file);
    cropImage.src = url;
    stage.hidden = false;
    document.body.style.overflow = "hidden";
    if (thumb) {
      thumb.innerHTML = "";
      var prev = document.createElement("img");
      prev.src = url;
      prev.alt = "Новая обложка";
      thumb.appendChild(prev);
    }
  });

  cropImage.addEventListener("load", function () {
    if (cropper) cropper.destroy();
    cropper = new Cropper(cropImage, {
      viewMode: 1,
      autoCropArea: 1,
      background: false,
      responsive: true,
      aspectRatio: NaN, // свободный кроп
    });
  });

  if (cancelBtn) {
    cancelBtn.addEventListener("click", function () {
      fileInput.value = "";
      // вернуть прежний src превью не восстанавливаем — submit без файла не тронет cover
      if (thumb) {
        var img = thumb.querySelector("img");
        if (img) img.removeAttribute("src");
      }
      closeStage();
    });
  }

  if (confirmBtn) {
    confirmBtn.addEventListener("click", function () {
      if (!cropper) {
        form.submit();
        return;
      }
      var data = cropper.getData(true);
      if (xInput) xInput.value = Math.max(0, data.x);
      if (yInput) yInput.value = Math.max(0, data.y);
      if (wInput) wInput.value = data.width;
      if (hInput) hInput.value = data.height;
      closeStage();
      form.submit();
    });
  }
})();
