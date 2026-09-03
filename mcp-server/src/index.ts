import { createServer } from 'node:http';
import {
  localhostHostValidation,
  localhostOriginValidation,
  toNodeHandler,
} from '@modelcontextprotocol/node';
import { createMcpHandler } from '@modelcontextprotocol/server';
import { buildServer } from './server.js';

const port = Number(process.env.PORT ?? '3000');
const host = process.env.HOST ?? '127.0.0.1';

if (host !== '127.0.0.1' && host !== 'localhost') {
  throw new Error(
    'This bootstrap server intentionally binds only to localhost. For a ChatGPT remote app, deploy behind TLS + authentication and use a production HTTP adapter.',
  );
}

const handler = createMcpHandler(buildServer);
const nodeHandler = toNodeHandler(handler);
const validateHost = localhostHostValidation();
const validateOrigin = localhostOriginValidation();

createServer((req, res) => {
  if (!validateHost(req, res) || !validateOrigin(req, res)) return;
  void nodeHandler(req, res);
}).listen(port, host, () => {
  console.error(`[mega-chatgpt] MCP listening on http://${host}:${port}/mcp`);
});
