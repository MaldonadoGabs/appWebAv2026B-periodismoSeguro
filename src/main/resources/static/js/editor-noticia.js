/* HU-09: editor de noticias. Sin dependencias, sin CDN y sin recursos de otros dominios. */
(function () {
  "use strict";

  var formulario = document.getElementById("form-noticia");
  if (!formulario) return;

  var editor = document.getElementById("editor");
  var campo = document.getElementById("cuerpo");
  var titulo = document.getElementById("titulo");
  var subtitulo = document.getElementById("subtitulo");
  var prevTitulo = document.getElementById("prev-titulo");
  var prevSubtitulo = document.getElementById("prev-subtitulo");
  var prevCuerpo = document.getElementById("prev-cuerpo");
  var entrada = document.getElementById("imagenes");
  var miniaturas = document.getElementById("miniaturas");

  /* Etiquetas permitidas en la vista previa; se copian sin ningun atributo. */
  var PERMITIDAS = {
    P: 1, DIV: 1, BR: 1, B: 1, STRONG: 1, I: 1, EM: 1, U: 1,
    UL: 1, OL: 1, LI: 1, BLOCKQUOTE: 1, H2: 1, H3: 1, Q: 1, CODE: 1, SPAN: 1
  };

  function htmlSeguro(html) {
    var doc = new DOMParser().parseFromString("<div>" + (html || "") + "</div>", "text/html");
    var raiz = doc.body.firstChild;
    var salida = document.createElement("div");
    if (!raiz) return "";

    function copiar(origen, destino) {
      Array.prototype.forEach.call(origen.childNodes, function (nodo) {
        if (nodo.nodeType === 3) {
          destino.appendChild(document.createTextNode(nodo.nodeValue));
        } else if (nodo.nodeType === 1) {
          if (!PERMITIDAS[nodo.tagName]) {
            copiar(nodo, destino); /* descarta la etiqueta, conserva el texto */
          } else {
            var copia = document.createElement(nodo.tagName.toLowerCase());
            destino.appendChild(copia);
            copiar(nodo, copia);
          }
        }
      });
    }

    copiar(raiz, salida);
    return salida.innerHTML;
  }

  function pintarVistaPrevia() {
    if (prevTitulo) prevTitulo.textContent = (titulo && titulo.value) || "Encabezado";
    if (prevSubtitulo) prevSubtitulo.textContent = (subtitulo && subtitulo.value) || "Subtitulo";
    if (prevCuerpo) prevCuerpo.innerHTML = htmlSeguro(campo.value);
  }

  function sincronizar() {
    campo.value = editor.innerHTML;
    pintarVistaPrevia();
  }

  if (editor && campo) {
    editor.innerHTML = htmlSeguro(campo.value);
    editor.addEventListener("input", sincronizar);
    formulario.addEventListener("submit", sincronizar);
    if (titulo) titulo.addEventListener("input", pintarVistaPrevia);
    if (subtitulo) subtitulo.addEventListener("input", pintarVistaPrevia);
    pintarVistaPrevia();
  }

  Array.prototype.forEach.call(document.querySelectorAll("[data-comando]"), function (boton) {
    boton.addEventListener("click", function () {
      editor.focus();
      var comando = boton.getAttribute("data-comando");
      var valor = boton.getAttribute("data-valor") || null;
      document.execCommand(comando, false, valor);
      sincronizar();
    });
  });

  /* Imagenes: se pueden quitar antes de guardar, para modificar la nota en el editor. */
  if (entrada && miniaturas) {
    var seleccion = [];

    function aplicar() {
      try {
        var dt = new DataTransfer();
        seleccion.forEach(function (archivo) { dt.items.add(archivo); });
        entrada.files = dt.files;
      } catch (e) {
        /* Navegador sin DataTransfer: se conservan los archivos elegidos, sin poder quitarlos. */
      }
    }

    function pintar() {
      miniaturas.innerHTML = "";
      seleccion.forEach(function (archivo, indice) {
        var li = document.createElement("li");
        var img = document.createElement("img");
        img.src = URL.createObjectURL(archivo);
        img.alt = "Imagen seleccionada " + (indice + 1);
        img.onload = function () { URL.revokeObjectURL(img.src); };

        var quitar = document.createElement("button");
        quitar.type = "button";
        quitar.className = "tema";
        quitar.textContent = "Quitar";
        quitar.addEventListener("click", function () {
          seleccion.splice(indice, 1);
          aplicar();
          pintar();
        });

        li.appendChild(img);
        li.appendChild(quitar);
        miniaturas.appendChild(li);
      });
    }

    entrada.addEventListener("change", function () {
      seleccion = Array.prototype.slice.call(entrada.files);
      pintar();
    });
  }
})();
