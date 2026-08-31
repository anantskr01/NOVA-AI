import WebSocket from 'ws';

export function connectNode(url, nodeId, token = '', onMessage = () => {}) {
  const ws = new WebSocket(url, { headers: token ? { 'x-nova-token': token } : {} });
  ws.on('open', () => ws.send(JSON.stringify({ type: 'register', nodeId })));
  ws.on('message', data => {
    try { onMessage(JSON.parse(data.toString()), ws); } catch { }
  });
  return ws;
}
