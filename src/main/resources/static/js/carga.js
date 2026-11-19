/*
 * Estados de carga de toda la aplicacion: barra superior, velo con mensaje ("Abriendo el chat..."),
 * botones ocupados ("Entrando...") y aviso si una pagina tarda. Sin librerias ni recursos externos.
 * Un enlace o formulario puede fijar su mensaje con data-carga="...", y el texto del boton con data-carga-boton="...".
 * Con "reducir movimiento" no hay animaciones (ver estilo.css), pero los mensajes se siguen mostrando.
 */
(function () {
  var ENLACES = [
    ['/login', 'Abriendo el inicio de sesión…'],
    ['/registro', 'Abriendo el registro…'],
    ['/recuperar', 'Abriendo la recuperación de contraseña…'],
    ['/inicio', 'Abriendo tu panel…'],
    ['/mis-notas', 'Cargando tus notas…'],
    ['/noticias/nueva', 'Abriendo el editor…'],
    ['/chat', 'Abriendo el chat…']
  ];

  // [fragmento de la ruta, mensaje del velo, texto del boton]
  var FORMULARIOS = [
    ['/login', 'Verificando tus credenciales…', 'Entrando…'],
    ['/registro', 'Creando tu cuenta…', 'Registrando…'],
    ['/logout', 'Cerrando sesión…', 'Cerrando…'],
    ['/recuperar', 'Enviando el enlace de recuperación…', 'Enviando…'],
    ['/restablecer', 'Guardando tu nueva contraseña…', 'Guardando…'],
    ['/noticias/nueva', 'Publicando tu noticia…', 'Publicando…'],
    ['/editar', 'Guardando los cambios…', 'Guardando…'],
    ['/eliminar', 'Eliminando la noticia…', 'Eliminando…'],
    ['/comentar', 'Publicando tu comentario…', 'Comentando…'],
    ['/reaccionar', 'Registrando tu reacción…', 'Enviando…']
  ];

  var RETRASO_IMPLICITO = 250;
  var TARDA_MAS_DE = 8000;

  var velo = null;
  var barra = null;
  var texto = null;
  var cancelar = null;
  var retraso = null;
  var lento = null;

  function crear() {
    if (velo) return;
    barra = document.createElement('div');
    barra.className = 'barra-carga';
    barra.setAttribute('aria-hidden', 'true');
    barra.hidden = true;

    velo = document.createElement('div');
    velo.className = 'velo-carga';
    velo.setAttribute('role', 'status');
    velo.setAttribute('aria-live', 'polite');
    velo.hidden = true;

    var caja = document.createElement('div');
    caja.className = 'velo-caja';
    var rueda = document.createElement('span');
    rueda.className = 'rueda';
    rueda.setAttribute('aria-hidden', 'true');
    texto = document.createElement('p');
    cancelar = document.createElement('button');
    cancelar.type = 'button';
    cancelar.className = 'boton';
    cancelar.textContent = 'Cancelar';
    cancelar.hidden = true;
    cancelar.addEventListener('click', function () {
      try { window.stop(); } catch (e) { /* ignorar */ }
      terminar();
    });

    caja.appendChild(rueda);
    caja.appendChild(texto);
    caja.appendChild(cancelar);
    velo.appendChild(caja);
    document.body.appendChild(barra);
    document.body.appendChild(velo);
  }

  function abrirVelo(mensaje) {
    texto.textContent = mensaje;
    velo.hidden = false;
  }

  function mostrar(mensaje, inmediato) {
    crear();
    // La pagina actual se desvanece un poco mientras carga la siguiente (solo con efectos activos, ver estilo.css)
    document.documentElement.classList.add("saliendo");
    document.body.setAttribute('aria-busy', 'true');
    barra.hidden = false;
    clearTimeout(retraso);
    clearTimeout(lento);
    if (inmediato) {
      abrirVelo(mensaje);
    } else {
      retraso = setTimeout(function () { abrirVelo(mensaje); }, RETRASO_IMPLICITO);
    }
    lento = setTimeout(function () {
      abrirVelo(mensaje.replace(/…$/, '') + '. Está tardando más de lo normal…');
      cancelar.hidden = false;
    }, TARDA_MAS_DE);
  }

  function liberarBotones() {
    var ocupados = document.querySelectorAll('.cargando');
    for (var i = 0; i < ocupados.length; i++) {
      var b = ocupados[i];
      if (b.hasAttribute('data-etiqueta')) {
        b.textContent = b.getAttribute('data-etiqueta');
        b.removeAttribute('data-etiqueta');
      }
      b.classList.remove('cargando');
      b.removeAttribute('aria-disabled');
      b.removeAttribute('aria-busy');
    }
    var formularios = document.querySelectorAll('form[data-enviando]');
    for (var j = 0; j < formularios.length; j++) formularios[j].removeAttribute('data-enviando');
  }

  function terminar() {
    clearTimeout(retraso);
    clearTimeout(lento);
    if (velo) { velo.hidden = true; cancelar.hidden = true; }
    if (barra) barra.hidden = true;
    document.body.removeAttribute("aria-busy");
    document.documentElement.classList.remove("saliendo");
    liberarBotones();
  }

  function ocupar(boton, etiqueta) {
    boton.classList.add('cargando');
    boton.setAttribute('aria-disabled', 'true');
    boton.setAttribute('aria-busy', 'true');
    // Solo se cambia la etiqueta si el boton es texto simple (no pisa contadores ni iconos)
    if (etiqueta && boton.children.length === 0) {
      boton.setAttribute('data-etiqueta', boton.textContent);
      boton.textContent = boton.getAttribute('data-carga-boton') || etiqueta;
    }
  }

  function buscar(tabla, ruta) {
    for (var i = 0; i < tabla.length; i++) {
      if (ruta === tabla[i][0] || ruta.indexOf(tabla[i][0]) !== -1) return tabla[i];
    }
    return null;
  }

  function textoEnlace(ruta) {
    for (var i = 0; i < ENLACES.length; i++) {
      if (ruta === ENLACES[i][0]) return ENLACES[i][1];
    }
    if (ruta.indexOf('/noticias/') === 0) return 'Cargando la noticia…';
    if (ruta === '/') return 'Cargando las noticias…';
    return null;
  }

  document.addEventListener('click', function (ev) {
    if (ev.defaultPrevented || ev.button !== 0 || ev.metaKey || ev.ctrlKey || ev.shiftKey || ev.altKey) return;
    var enlace = ev.target && ev.target.closest ? ev.target.closest('a[href]') : null;
    if (!enlace || enlace.target === '_blank' || enlace.hasAttribute('download')) return;
    var url;
    try { url = new URL(enlace.href, location.href); } catch (e) { return; }
    if (url.origin !== location.origin) return;
    if (url.pathname === location.pathname && url.search === location.search && url.hash) return;
    if (/\.(png|jpe?g|gif|webp|svg|pdf|zip|css|js|woff2)$/i.test(url.pathname)) return;

    var explicito = enlace.getAttribute('data-carga');
    var mensaje = explicito || textoEnlace(url.pathname);
    if (enlace.classList.contains('boton')) enlace.classList.add('cargando');
    mostrar(mensaje || 'Cargando…', !!explicito || (!!mensaje && ENLACES.some(function (e) { return e[0] === url.pathname; })));
  });

  document.addEventListener('submit', function (ev) {
    if (ev.defaultPrevented) return;
    var formulario = ev.target;
    if (!formulario || formulario.tagName !== 'FORM') return;
    if (formulario.hasAttribute('data-enviando')) { ev.preventDefault(); return; }

    var ruta = '';
    try { ruta = new URL(formulario.action, location.href).pathname; } catch (e) { /* ignorar */ }
    var metodo = (formulario.getAttribute('method') || 'get').toLowerCase();
    var fila = buscar(FORMULARIOS, ruta);
    var mensaje = formulario.getAttribute('data-carga') || (fila ? fila[1] : (metodo === 'get' ? 'Buscando…' : 'Enviando…'));
    var etiqueta = fila ? fila[2] : (metodo === 'get' ? 'Buscando…' : 'Enviando…');

    var archivos = formulario.querySelectorAll('input[type="file"]');
    for (var i = 0; i < archivos.length; i++) {
      if (archivos[i].files && archivos[i].files.length > 0) {
        mensaje = 'Subiendo las imágenes y ' + mensaje.charAt(0).toLowerCase() + mensaje.slice(1);
        break;
      }
    }

    var boton = ev.submitter || formulario.querySelector('button[type="submit"], button:not([type])');
    formulario.setAttribute('data-enviando', '1');
    if (boton) ocupar(boton, etiqueta);
    mostrar(mensaje, metodo !== 'get' || !!formulario.getAttribute('data-carga'));
  });

  // Al volver con "atras" el navegador puede restaurar la pagina con el velo puesto
  window.addEventListener('pageshow', terminar);
})();
