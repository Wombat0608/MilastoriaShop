/* Админка — выбор фото → кроп на весь экран → отправка формы.
   Cropper.js делает всю работу с жестами (включая pinch на iPad),
   этот файл только соединяет его с обычной HTML-формой. */

(function () {
  const fileInput = document.getElementById("file-input");
  if (!fileInput) return; // на этой странице нет загрузки (напр. логин)

  const cropStage = document.getElementById("crop-stage");
  const cropImage = document.getElementById("crop-image");
  const confirmBtn = document.getElementById("crop-confirm");
  const cancelBtn = document.getElementById("crop-cancel");
  const form = document.getElementById("upload-form");
  const xInput = document.getElementById("crop-x");
  const yInput = document.getElementById("crop-y");
  const wInput = document.getElementById("crop-w");
  const hInput = document.getElementById("crop-h");

  let cropper = null;

  function openCropStage(file) {
    cropImage.src = URL.createObjectURL(file);
    cropStage.hidden = false;
    document.body.style.overflow = "hidden";
  }

  function closeCropStage() {
    cropStage.hidden = true;
    document.body.style.overflow = "";
    if (cropper) {
      cropper.destroy();
      cropper = null;
    }
  }

  fileInput.addEventListener("change", () => {
    const file = fileInput.files[0];
    if (!file) return;
    openCropStage(file);
  });

  cropImage.addEventListener("load", () => {
    if (cropper) cropper.destroy();
    cropper = new Cropper(cropImage, {
      viewMode: 1,
      autoCropArea: 1,
      background: false,
      responsive: true,
      crop() {
        // layout watermark-рамки (admin-crop-wm.js)
        if (typeof window.__setCropCropper === "function") {
          window.__setCropCropper(cropper);
        }
        document.dispatchEvent(new CustomEvent("cropper:ready"));
      },
    });
  });

  cancelBtn.addEventListener("click", () => {
    fileInput.value = "";
    closeCropStage();
  });

  confirmBtn.addEventListener("click", () => {
    if (!cropper) return;
    const data = cropper.getData(true); // true = округлить до целых пикселей
    xInput.value = Math.max(0, data.x);
    yInput.value = Math.max(0, data.y);
    wInput.value = data.width;
    hInput.value = data.height;
    // wm-поля уже должны быть в hidden (admin-crop-wm.js, capture)
    form.submit();
  });
})();
