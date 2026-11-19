/*
 * Chat de periodistas: conversacion tipo WhatsApp, sin librerias ni recursos externos.
 * El servidor envia solo seudonimos y la hora con precision de minuto.
 */
(function () {
  var raiz = document.querySelector('[data-yo]');
  var yo = raiz ? raiz.getAttribute('data-yo') : '';
  var socket = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws');
  var estado = document.getElementById('estado');
  var log = document.getElementById('log');
  var conectados = document.getElementById('conectados');
  var total = document.getElementById('total-conectados');
  var escribiendo = document.getElementById('escribiendo');
  var campo = document.getElementById('t');
  var formulario = document.getElementById('f');
  var temporizador = null;
  var ultimoAviso = 0;
  var esqueleto = document.getElementById('cargando-conversacion');
  var enviar = formulario ? formulario.querySelector('button') : null;

  // Estado de la conexion con texto y color (nunca solo color): conectando, conectado, error o desconectado
  function marcar(mensaje, clase) {
    if (!estado) return;
    estado.textContent = mensaje;
    estado.className = 'estado-conexion ' + clase;
  }

  function quitarEsqueleto() {
    if (esqueleto) {
      esqueleto.remove();
      esqueleto = null;
    }
    if (log) log.removeAttribute('aria-busy');
  }

  function horaLocal(iso) {
    var fecha = new Date(iso);
    if (isNaN(fecha.getTime())) return '';
    return fecha.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  function pintarMensaje(autor, texto, hora) {
    var propio = autor === yo;
    var li = document.createElement('li');
    li.className = 'mensaje' + (propio ? ' propio' : '');

    if (!propio) {
      var nombre = document.createElement('span');
      nombre.className = 'autor';
      nombre.textContent = autor;
      li.appendChild(nombre);
    }
    var burbuja = document.createElement('div');
    burbuja.className = 'burbuja';
    burbuja.textContent = texto;
    li.appendChild(burbuja);

    var marca = document.createElement('time');
    marca.className = 'hora';
    marca.textContent = horaLocal(hora);
    li.appendChild(marca);

    log.appendChild(li);
    log.scrollTop = log.scrollHeight;
  }

  function pintarIngreso(usuario) {
    var li = document.createElement('li');
    li.className = 'mensaje sistema';
    li.textContent = usuario + ' se unió a la sala';
    log.appendChild(li);
    log.scrollTop = log.scrollHeight;
  }

  function pintarConectados(usuarios) {
    conectados.replaceChildren();
    total.textContent = String(usuarios.length);
    if (usuarios.length === 0) {
      var li = document.createElement('li');
      li.className = 'suave';
      li.textContent = 'Nadie conectado';
      conectados.appendChild(li);
      return;
    }
    for (var i = 0; i < usuarios.length; i++) {
      var item = document.createElement('li');
      item.textContent = usuarios[i];
      conectados.appendChild(item);
    }
  }

  function avisarEscritura(activo) {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ tipo: 'escribiendo', activo: activo }));
    }
  }

  socket.onopen = function () {
    marcar('Conectado', 'ok');
    if (enviar) enviar.disabled = false;
    // Si la sala esta vacia no llega ningun mensaje: se quita el esqueleto igualmente
    setTimeout(quitarEsqueleto, 1500);
  };
  socket.onerror = function () { marcar('Error de conexión', 'error'); };
  socket.onclose = function () {
    marcar('Desconectado', 'error');
    quitarEsqueleto();
    if (enviar) enviar.disabled = true;
    if (conectados) conectados.replaceChildren();
    if (total) total.textContent = '0';
    if (escribiendo) escribiendo.textContent = '';
  };

  socket.onmessage = function (evento) {
    var dato;
    try { dato = JSON.parse(evento.data); } catch (error) { return; }
    if (!dato) return;
    quitarEsqueleto();

    if (dato.tipo === 'mensaje') {
      pintarMensaje(dato.autor || 'Periodista', dato.texto || '', dato.hora || '');
    } else if (dato.tipo === 'conectados') {
      pintarConectados(Array.isArray(dato.usuarios) ? dato.usuarios : []);
    } else if (dato.tipo === 'ingreso') {
      pintarIngreso(dato.usuario || 'Un periodista');
    } else if (dato.tipo === 'escribiendo') {
      escribiendo.textContent = dato.activo ? (dato.usuario + ' está escribiendo…') : '';
    }
  };

  campo.addEventListener('input', function () {
    var ahora = Date.now();
    if (ahora - ultimoAviso > 1000) {
      avisarEscritura(true);
      ultimoAviso = ahora;
    }
    clearTimeout(temporizador);
    temporizador = setTimeout(function () { avisarEscritura(false); }, 1500);
  });

  formulario.addEventListener('submit', function (ev) {
    ev.preventDefault();
    var texto = campo.value.trim();
    if (!texto || socket.readyState !== WebSocket.OPEN) return;
    socket.send(texto);
    campo.value = '';
    clearTimeout(temporizador);
    avisarEscritura(false);
  });
})();
