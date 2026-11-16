const ws = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws');
const estado = document.getElementById('estado');
const log = document.getElementById('log');
ws.onopen = () => estado.textContent = 'conectado';
ws.onerror = () => estado.textContent = 'error';
ws.onclose = () => estado.textContent = 'desconectado';
ws.onmessage = e => {
  const li = document.createElement('li');
  li.textContent = e.data;
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
