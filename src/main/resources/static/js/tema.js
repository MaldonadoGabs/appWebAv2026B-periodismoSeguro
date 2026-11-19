/*
 * Interruptor de tema. Cada visita abre SIEMPRE en claro: no se sigue la preferencia del sistema y se descarta
 * cualquier eleccion guardada de visitas anteriores. El oscuro solo se enciende con el interruptor y dura mientras
 * la persona navega en esta pestana (sessionStorage); al volver otro dia o en otra pestana, vuelve el claro.
 * Sin dependencias ni recursos externos.
 */
(function () {
  var raiz = document.documentElement;
  var oscuro = false;
  try {
    // Eleccion permanente de versiones anteriores: se borra para que nunca fuerce el oscuro
    localStorage.removeItem("tema");
    oscuro = sessionStorage.getItem("tema") === "oscuro";
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
      try { sessionStorage.setItem("tema", oscuro ? "oscuro" : "claro"); } catch (e) { /* ignorar */ }
    });
  });
})();
