import { WebSocketServer } from 'ws';

const port = Number(process.env.PORT || 8765);
const token = process.env.NOVA_GATEWAY_TOKEN || '';
const clients = new Map();
const wss = new WebSocketServer({ port });

function send(ws, value) {
  if (ws.readyState === ws.OPEN) ws.send(JSON.stringify(value));
}

wss.on('connection', (ws, req) => {
  const supplied = req.headers['x-nova-token'] || '';
  if (token && supplied !== token) { ws.close(1008, 'unauthorized'); return; }

  let nodeId = null;
  ws.on('message', raw => {
    let message;
    try { message = JSON.parse(raw.toString()); } catch { return; }
    if (message.type === 'register') {
      nodeId = String(message.nodeId || '');
      if (!nodeId) { ws.close(1008, 'node_id_required'); return; }
      clients.set(nodeId, ws);
      send(ws, { type: 'registered', nodeId });
      return;
    }
    if (message.targetNodeId) {
      const target = clients.get(String(message.targetNodeId));
      if (target) send(target, message);
    }
  });

  ws.on('close', () => { if (nodeId && clients.get(nodeId) === ws) clients.delete(nodeId); });
});

console.log(`NOVA gateway listening on :${port}`);
