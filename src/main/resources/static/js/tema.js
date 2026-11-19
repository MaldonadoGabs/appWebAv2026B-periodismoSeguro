/*
 * Interruptor de tema. El tema claro es SIEMPRE el predeterminado: no se sigue la preferencia del sistema.
 * El oscuro solo se activa cuando la persona enciende el interruptor, y esa eleccion se recuerda en este navegador.
 * Sin dependencias ni recursos externos.
 */
(function () {
  var raiz = document.documentElement;
  var oscuro = false;
  try {
    oscuro = localStorage.getItem("tema") === "oscuro";
  } catch (e) { /* sin almacenamiento: se queda en claro */ }
  raiz.setAttribute("data-tema", oscuro ? "oscuro" : "claro");

  function pintar(boton) {
    boton.setAttribute("aria-checked", oscuro ? "true" : "false");
  }

  document.addEventListener("DOMContentLoaded", function () {
    var interruptor = document.getElementById("tema");
    if (!interruptor) return;
    pintar(interruptor);
    interruptor.addEventListener("click", function () {
      oscuro = !oscuro;
      raiz.setAttribute("data-tema", oscuro ? "oscuro" : "claro");
      pintar(interruptor);
      try { localStorage.setItem("tema", oscuro ? "oscuro" : "claro"); } catch (e) { /* ignorar */ }
    });
  });
})();
