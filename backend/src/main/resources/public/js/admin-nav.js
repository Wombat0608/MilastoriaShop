/* Админка: гамбургер-меню на мобильных (≤900px).
   На десктопе кнопка скрыта CSS, nav — горизонтальный. */

(function () {
  var toggle = document.getElementById("admin-nav-toggle");
  var nav = document.getElementById("admin-nav");
  if (!toggle || !nav) return;

  function setOpen(open) {
    nav.classList.toggle("is-open", !!open);
    toggle.setAttribute("aria-expanded", open ? "true" : "false");
  }

  function isOpen() {
    return nav.classList.contains("is-open");
  }

  toggle.addEventListener("click", function (e) {
    e.stopPropagation();
    setOpen(!isOpen());
  });

  nav.addEventListener("click", function (e) {
    if (e.target && e.target.closest && e.target.closest("a")) {
      setOpen(false);
    }
  });

  document.addEventListener("click", function (e) {
    if (!isOpen()) return;
    if (e.target && e.target.closest && e.target.closest("#admin-nav, #admin-nav-toggle")) return;
    setOpen(false);
  });

  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape") setOpen(false);
  });

  window.addEventListener("resize", function () {
    if (window.matchMedia && window.matchMedia("(min-width: 901px)").matches) {
      setOpen(false);
    }
  });
})();
