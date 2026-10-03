/* Админка — просмотр фото из сетки лота (та же страница /admin/lots/{id}/photos).
   Открывает full-вариант (path_full) в lightbox; prev/next по порядку карточек. */

(function () {
  var grid = document.getElementById("photo-grid");
  var box = document.getElementById("admin-photo-view");
  if (!grid || !box) return;

  var img = document.getElementById("pv-img");
  var caption = document.getElementById("pv-caption");
  var closeBtn = document.getElementById("pv-close");
  var prevBtn = document.getElementById("pv-prev");
  var nextBtn = document.getElementById("pv-next");
  var buttons = Array.prototype.slice.call(grid.querySelectorAll(".admin-photo-view"));
  if (!img || buttons.length === 0) return;

  var index = 0;

  function show(i) {
    if (i < 0) i = buttons.length - 1;
    if (i >= buttons.length) i = 0;
    index = i;
    var btn = buttons[index];
    img.src = btn.getAttribute("data-full") || "";
    img.alt = btn.getAttribute("data-alt") || "";
    if (caption) {
      caption.textContent = img.alt
        ? img.alt + " · " + (index + 1) + " / " + buttons.length
        : (index + 1) + " / " + buttons.length;
    }
    box.classList.add("is-open");
    box.hidden = false;
    document.body.style.overflow = "hidden";
  }

  function close() {
    box.classList.remove("is-open");
    box.hidden = true;
    img.removeAttribute("src");
    document.body.style.overflow = "";
  }

  buttons.forEach(function (btn, i) {
    btn.addEventListener("click", function (e) {
      e.preventDefault();
      e.stopPropagation();
      show(i);
    });
  });

  closeBtn.addEventListener("click", function (e) {
    e.stopPropagation();
    close();
  });
  prevBtn.addEventListener("click", function (e) {
    e.stopPropagation();
    show(index - 1);
  });
  nextBtn.addEventListener("click", function (e) {
    e.stopPropagation();
    show(index + 1);
  });

  // клик по подложке — закрыть; по кнопкам/картинке — нет
  box.addEventListener("click", function (e) {
    if (e.target === box) close();
  });

  document.addEventListener("keydown", function (e) {
    if (!box.classList.contains("is-open")) return;
    if (e.key === "Escape") close();
    else if (e.key === "ArrowLeft") show(index - 1);
    else if (e.key === "ArrowRight") show(index + 1);
  });
})();
