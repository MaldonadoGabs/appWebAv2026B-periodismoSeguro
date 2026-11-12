const ws = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws');
const estado = document.getElementById('estado');
const log = document.getElementById('log');
ws.onopen = () => estado.textContent = 'conectado';
ws.onclose = () => estado.textContent = 'desconectado';
ws.onmessage = e => { const li = document.createElement('li'); li.textContent = e.data; log.prepend(li); };
document.getElementById('f').onsubmit = ev => {
  ev.preventDefault();
  const t = document.getElementById('t');
  if (t.value && ws.readyState === 1) { ws.send(t.value); t.value = ''; }
};
