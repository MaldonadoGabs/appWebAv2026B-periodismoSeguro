/*
 * HU-21: avisos de noticias nuevas en cualquier pantalla.
 * El socket es publico y no identifica al lector: no hay cookies, IP ni datos personales en el cliente.
 */
(function () {
  var contenedor = document.getElementById('avisos');
  if (!contenedor || typeof WebSocket === 'undefined') return;

  var esquema = location.protocol === 'https:' ? 'wss://' : 'ws://';
  var socket = new WebSocket(esquema + location.host + '/avisos-ws');

  socket.onmessage = function (evento) {
    var aviso;
    try { aviso = JSON.parse(evento.data); } catch (error) { return; }
    if (!aviso || aviso.tipo !== 'noticia' || !aviso.url) return;

    var item = document.createElement('li');
    item.className = 'aviso-noticia';

    var texto = document.createElement('span');
    texto.textContent = 'Nueva noticia: ' + (aviso.titulo || '') + ' · ' + (aviso.autor || '');
    item.appendChild(texto);

    var enlace = document.createElement('a');
    enlace.href = aviso.url;
    enlace.textContent = 'Ver nota';
    item.appendChild(enlace);

    var cerrar = document.createElement('button');
    cerrar.type = 'button';
    cerrar.className = 'aviso-cerrar';
    cerrar.setAttribute('aria-label', 'Cerrar aviso');
    cerrar.textContent = '×';
    cerrar.onclick = function () { item.remove(); };
    item.appendChild(cerrar);

    contenedor.prepend(item);
    setTimeout(function () { item.remove(); }, 12000);
  };
})();
