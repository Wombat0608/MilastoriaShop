/* Milastoria — клиентский JS поверх серверного HTML.
   Никакой модели данных тут больше нет: только DOM и data-атрибуты,
   которые уже отрендерил jte. */

(function () {
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

  function initHeader() {
    const header = $("#site-header");
    if (!header) return;

    const onScroll = () => header.classList.toggle("is-scrolled", window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });

    const toggle = $("[data-menu-toggle]", header);
    if (toggle) {
      toggle.addEventListener("click", () => {
        const open = header.classList.toggle("is-open");
        toggle.setAttribute("aria-expanded", open ? "true" : "false");
        document.body.style.overflow = open ? "hidden" : "";
      });
    }

    $$("#mobile-menu a").forEach((a) =>
      a.addEventListener("click", () => {
        header.classList.remove("is-open");
        if (toggle) toggle.setAttribute("aria-expanded", "false");
        document.body.style.overflow = "";
      })
    );
  }

  function initWorkGallery() {
    const gallery = $("[data-work-gallery]");
    if (!gallery) return;

    const mainImg = $("[data-main-image]", gallery);
    const thumbs = $$(".thumb", gallery);
    const mainBtn = $("[data-lightbox-open]", gallery);
    const lightbox = $("#lightbox");
    const lbImg = lightbox ? $("img", lightbox) : null;

    function activateThumb(thumb) {
      thumbs.forEach((t) => t.classList.remove("is-active"));
      thumb.classList.add("is-active");
      if (mainImg) {
        mainImg.src = thumb.dataset.full;
        mainImg.alt = thumb.dataset.alt || "";
      }
    }

    thumbs.forEach((thumb) => thumb.addEventListener("click", () => activateThumb(thumb)));

    if (!lightbox || !lbImg || !mainBtn || thumbs.length === 0) return;

    function openAt(index) {
      const thumb = thumbs[(index + thumbs.length) % thumbs.length];
      lbImg.src = thumb.dataset.full;
      lbImg.alt = thumb.dataset.alt || "";
      lightbox.dataset.index = String(thumbs.indexOf(thumb));
      lightbox.classList.add("is-open");
      document.body.style.overflow = "hidden";
    }

    function close() {
      lightbox.classList.remove("is-open");
      document.body.style.overflow = "";
    }

    function step(delta) {
      const current = Number(lightbox.dataset.index || 0);
      openAt(current + delta);
    }

    mainBtn.addEventListener("click", () => {
      const activeIndex = thumbs.findIndex((t) => t.classList.contains("is-active"));
      openAt(activeIndex >= 0 ? activeIndex : 0);
    });

    const closeBtn = $(".lightbox__close", lightbox);
    const prevBtn = $(".lightbox__nav--prev", lightbox);
    const nextBtn = $(".lightbox__nav--next", lightbox);
    if (closeBtn) closeBtn.addEventListener("click", close);
    if (prevBtn) prevBtn.addEventListener("click", () => step(-1));
    if (nextBtn) nextBtn.addEventListener("click", () => step(1));
    lightbox.addEventListener("click", (e) => {
      if (e.target === lightbox) close();
    });
    document.addEventListener("keydown", (e) => {
      if (!lightbox.classList.contains("is-open")) return;
      if (e.key === "Escape") close();
      if (e.key === "ArrowLeft") step(-1);
      if (e.key === "ArrowRight") step(1);
    });

    // Свайп на телефоне: влево — следующее, вправо — предыдущее
    let touchStartX = 0;
    let touchStartY = 0;
    lightbox.addEventListener(
      "touchstart",
      (e) => {
        if (!e.changedTouches.length) return;
        touchStartX = e.changedTouches[0].screenX;
        touchStartY = e.changedTouches[0].screenY;
      },
      { passive: true }
    );
    lightbox.addEventListener(
      "touchend",
      (e) => {
        if (!e.changedTouches.length) return;
        const dx = e.changedTouches[0].screenX - touchStartX;
        const dy = e.changedTouches[0].screenY - touchStartY;
        if (Math.abs(dx) < 48 || Math.abs(dx) < Math.abs(dy)) return;
        step(dx < 0 ? 1 : -1);
      },
      { passive: true }
    );
  }

  document.addEventListener("DOMContentLoaded", () => {
    initHeader();
    initWorkGallery();
  });
})();
