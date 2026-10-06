/* Промо-слайдер hero: автоплей 30с, стрелки ←/→.
   Без стрелок и автоплея, если слайдов ≤ 1.
   Видео: autoplay muted playsinline, без controls; pause при уходе со слайда. */

(function () {
  var root = document.getElementById("hero-slider");
  if (!root) return;

  var slides = Array.prototype.slice.call(root.querySelectorAll(".hero-slide"));
  if (slides.length <= 1) return; // один слайд — статичная шапка, без UI

  var prevBtn = root.querySelector("[data-hero-prev]");
  var nextBtn = root.querySelector("[data-hero-next]");
  var dotsWrap = root.querySelector("[data-hero-dots]");
  var index = 0;
  var timer = null;
  var AUTO_MS = 30000;
  var reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  function show(i) {
    index = (i + slides.length) % slides.length;
    slides.forEach(function (slide, n) {
      var active = n === index;
      slide.classList.toggle("is-active", active);
      slide.setAttribute("aria-hidden", active ? "false" : "true");
      var video = slide.querySelector("video");
      if (video) {
        if (active) {
          try {
            video.currentTime = 0;
            var p = video.play();
            if (p && p.catch) p.catch(function () {});
          } catch (e) { /* ignore */ }
        } else {
          try { video.pause(); } catch (e) { /* ignore */ }
        }
      }
    });
    if (dotsWrap) {
      Array.prototype.slice.call(dotsWrap.children).forEach(function (dot, n) {
        dot.classList.toggle("is-active", n === index);
        dot.setAttribute("aria-current", n === index ? "true" : "false");
      });
    }
  }

  function next() { show(index + 1); }
  function prev() { show(index - 1); }

  function startTimer() {
    if (reduceMotion) return;
    stopTimer();
    timer = window.setInterval(next, AUTO_MS);
  }

  function stopTimer() {
    if (timer) {
      window.clearInterval(timer);
      timer = null;
    }
  }

  if (nextBtn) nextBtn.addEventListener("click", function () { next(); startTimer(); });
  if (prevBtn) prevBtn.addEventListener("click", function () { prev(); startTimer(); });

  if (dotsWrap) {
    slides.forEach(function (_, n) {
      var btn = document.createElement("button");
      btn.type = "button";
      btn.className = "hero-dot";
      btn.setAttribute("aria-label", "Слайд " + (n + 1));
      btn.addEventListener("click", function () { show(n); startTimer(); });
      dotsWrap.appendChild(btn);
    });
  }

  // свайпы (мобильные)
  var touchX = null;
  root.addEventListener("touchstart", function (e) {
    touchX = e.changedTouches && e.changedTouches[0] ? e.changedTouches[0].clientX : null;
  }, { passive: true });
  root.addEventListener("touchend", function (e) {
    if (touchX == null || !e.changedTouches || !e.changedTouches[0]) return;
    var dx = e.changedTouches[0].clientX - touchX;
    touchX = null;
    if (Math.abs(dx) < 40) return;
    if (dx < 0) next(); else prev();
    startTimer();
  }, { passive: true });

  document.addEventListener("visibilitychange", function () {
    if (document.visibilityState === "visible") startTimer();
    else stopTimer();
  });

  root.addEventListener("mouseenter", stopTimer);
  root.addEventListener("mouseleave", startTimer);

  show(0);
  startTimer();
})();
