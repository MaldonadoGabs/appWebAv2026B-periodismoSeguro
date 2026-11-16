const ws = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws');
const estado = document.getElementById('estado');
const log = document.getElementById('log');
const conectados = document.getElementById('conectados');
const total = document.getElementById('total-conectados');

function mostrarSinConexion() {
  conectados.replaceChildren();
  const li = document.createElement('li');
  li.className = 'suave';
  li.textContent = 'Sin conexión';
  conectados.appendChild(li);
  total.textContent = '0';
}

function pintarConectados(usuarios) {
  conectados.replaceChildren();
  total.textContent = String(usuarios.length);
  if (usuarios.length === 0) {
    const li = document.createElement('li');
    li.className = 'suave';
    li.textContent = 'Nadie conectado';
    conectados.appendChild(li);
    return;
  }
  for (const usuario of usuarios) {
    const li = document.createElement('li');
    li.textContent = usuario;
    conectados.appendChild(li);
  }
}

ws.onopen = () => estado.textContent = 'conectado';
ws.onerror = () => estado.textContent = 'error';
ws.onclose = () => {
  estado.textContent = 'desconectado';
  mostrarSinConexion();
};
ws.onmessage = e => {
  let evento = null;
  try { evento = JSON.parse(e.data); } catch { /* compatibilidad con texto plano */ }
  if (evento && evento.tipo === 'conectados') {
    pintarConectados(Array.isArray(evento.usuarios) ? evento.usuarios : []);
    return;
  }
  const texto = evento && evento.tipo === 'mensaje' ? evento.texto : e.data;
  const li = document.createElement('li');
  li.textContent = texto;
  log.prepend(li);
};
document.getElementById('f').onsubmit = ev => {
  ev.preventDefault();
  const t = document.getElementById('t');
  const texto = t.value.trim();
  if (texto && ws.readyState === WebSocket.OPEN) {
    ws.send(texto);
    t.value = '';
  }
};
