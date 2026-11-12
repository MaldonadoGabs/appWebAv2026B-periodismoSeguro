/* Tema claro/oscuro. Sin dependencias ni recursos externos. */
(function () {
  var raiz = document.documentElement;
  try {
    var guardado = localStorage.getItem("tema");
    if (guardado === "claro" || guardado === "oscuro") raiz.setAttribute("data-tema", guardado);
  } catch (e) { /* sin almacenamiento: se usa el tema del sistema */ }

  document.addEventListener("DOMContentLoaded", function () {
    var boton = document.getElementById("tema");
    if (!boton) return;
    boton.addEventListener("click", function () {
      var actual = raiz.getAttribute("data-tema");
      var oscuroSistema = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
      var esOscuro = actual ? actual === "oscuro" : oscuroSistema;
      var nuevo = esOscuro ? "claro" : "oscuro";
      raiz.setAttribute("data-tema", nuevo);
      try { localStorage.setItem("tema", nuevo); } catch (e) { /* ignorar */ }
    });
  });
})();
