/*
 * Efectos de movimiento de Periodismo Seguro: las noticias y los paneles entran en fila al verse, la barra de
 * "Ultima hora" se puede pausar y hay un interruptor para apagar todo. Solo transform y opacity (baratos en movil).
 * Respeta "reducir movimiento" del sistema y no usa librerias ni recursos externos.
 * Se carga sin defer para poner la clase antes del primer pintado y evitar un parpadeo.
 */
(function () {
  var raiz = document.documentElement;
  var apagado = false;
  try { apagado = localStorage.getItem("efectos") === "off"; } catch (e) { /* sin almacenamiento: efectos activos */ }
  var reducir = !!(window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches);
  var puedeObservar = typeof IntersectionObserver !== "undefined";

  if (apagado) raiz.setAttribute("data-efectos", "off");
  var activos = !apagado && !reducir && puedeObservar;
  if (activos) raiz.classList.add("con-efectos");

  var SELECTOR = ".tarjeta, .rejilla .panel, .pilares li, .feed .nota";
  var observador = null;
  var revelados = 0;

  function mostrar(el, retraso) {
    el.style.transitionDelay = retraso + "ms";
    el.classList.add("visible");
    // El retraso solo es para la entrada: se quita para que el efecto al pasar el puntero sea inmediato
    setTimeout(function () { el.style.transitionDelay = ""; }, retraso + 700);
  }

  function revelar() {
    var objetivos = document.querySelectorAll(SELECTOR);
    observador = new IntersectionObserver(function (entradas) {
      var lote = 0;
      for (var i = 0; i < entradas.length; i++) {
        if (!entradas[i].isIntersecting) continue;
        mostrar(entradas[i].target, Math.min(lote, 8) * 70);
        observador.unobserve(entradas[i].target);
        lote++;
        revelados++;
      }
    }, { threshold: 0.12 });
    for (var j = 0; j < objetivos.length; j++) observador.observe(objetivos[j]);

    // Seguro: si el navegador no llega a avisar (pestana oculta, cuadros limitados), se muestra todo
    setTimeout(function () {
      if (revelados > 0) return;
      for (var k = 0; k < objetivos.length; k++) objetivos[k].classList.add("visible");
    }, 2500);
  }

  function activarPausa() {
    var boton = document.querySelector(".uh-pausa");
    var pista = document.querySelector(".uh-pista");
    if (!boton || !pista) return;
    boton.addEventListener("click", function () {
      var pausada = pista.classList.toggle("pausada");
      boton.setAttribute("aria-pressed", pausada ? "true" : "false");
      boton.textContent = pausada ? "Reanudar" : "Pausar";
    });
  }

  function activarInterruptor() {
    var interruptor = document.getElementById("efectos");
    if (!interruptor) return;
    if (reducir) {
      interruptor.setAttribute("aria-checked", "false");
      interruptor.disabled = true;
      interruptor.setAttribute("title", "Tu sistema pide reducir el movimiento");
      return;
    }
    interruptor.setAttribute("aria-checked", apagado ? "false" : "true");
    interruptor.addEventListener("click", function () {
      apagado = !apagado;
      interruptor.setAttribute("aria-checked", apagado ? "false" : "true");
      try { localStorage.setItem("efectos", apagado ? "off" : "on"); } catch (e) { /* ignorar */ }
      if (apagado) {
        raiz.setAttribute("data-efectos", "off");
        raiz.classList.remove("con-efectos");
      } else {
        raiz.removeAttribute("data-efectos");
        // Lo que ya se ve no se vuelve a ocultar: solo se animara lo que llegue despues
        var presentes = document.querySelectorAll(SELECTOR);
        for (var i = 0; i < presentes.length; i++) presentes[i].classList.add("visible");
        if (puedeObservar) raiz.classList.add("con-efectos");
      }
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    if (activos) revelar();
    activarPausa();
    activarInterruptor();
  });
})();
