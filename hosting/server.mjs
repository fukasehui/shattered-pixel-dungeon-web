import { createServer } from 'node:http';
import { createReadStream } from 'node:fs';
import { stat } from 'node:fs/promises';
import { resolve, sep, extname } from 'node:path';

const root = resolve(process.env.STATIC_ROOT || './public');
const types = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.json': 'application/json',
  '.wasm': 'application/wasm', '.png': 'image/png', '.jpg': 'image/jpeg',
  '.webp': 'image/webp', '.svg': 'image/svg+xml', '.ico': 'image/x-icon',
  '.mp3': 'audio/mpeg', '.ogg': 'audio/ogg', '.wav': 'audio/wav',
  '.ttf': 'font/ttf', '.woff2': 'font/woff2', '.zip': 'application/zip',
  '.txt': 'text/plain; charset=utf-8', '.fnt': 'text/plain; charset=utf-8',
};
const server = createServer(async (req, res) => {
  const fail = (status) => { res.writeHead(status); res.end(); };
  if (!['GET', 'HEAD'].includes(req.method)) return fail(405);
  let pathname;
  try { pathname = decodeURIComponent(new URL(req.url, 'http://local').pathname); }
  catch { return fail(400); }
  const parts = pathname.replaceAll('\\', '/').split('/');
  if (parts.some(part => part.startsWith('.')) || pathname.includes('\0')) return fail(403);
  let file = resolve(root, `.${pathname}`);
  if (file !== root && !file.startsWith(root + sep)) return fail(403);
  try {
    let info = await stat(file);
    if (info.isDirectory()) {
      if (!pathname.endsWith('/')) {
        const url = new URL(req.url, 'http://local');
        res.writeHead(308, { Location: url.pathname + '/' + url.search });
        return res.end();
      }
      file = resolve(file, 'index.html');
      info = await stat(file);
    }
    if (!info.isFile()) return fail(404);
    const contentType = types[extname(file).toLowerCase()] || 'application/octet-stream';
    let compressed = false;
    if (/\bgzip\b/.test(req.headers['accept-encoding'] || '')) {
      try {
        const gzipInfo = await stat(file + '.gz');
        if (gzipInfo.isFile()) { file += '.gz'; info = gzipInfo; compressed = true; }
      } catch {}
    }
    const etag = `W/"${info.size.toString(16)}-${Math.trunc(info.mtimeMs).toString(16)}"`;
    if ((req.headers['if-none-match'] || '').split(/\s*,\s*/).includes(etag)) {
      res.writeHead(304, { ETag: etag, 'Cache-Control': 'no-cache', Vary: 'Accept-Encoding' });
      return res.end();
    }
    res.writeHead(200, {
      'Content-Type': contentType,
      'Content-Length': info.size,
      ...(compressed ? { 'Content-Encoding': 'gzip' } : {}),
      'Vary': 'Accept-Encoding',
      'ETag': etag,
      'Cache-Control': 'no-cache',
      'X-Content-Type-Options': 'nosniff',
      'Referrer-Policy': 'same-origin',
    });
    if (req.method === 'HEAD') return res.end();
    createReadStream(file).on('error', () => res.destroy()).pipe(res);
  } catch (error) {
    fail(['ENOENT', 'ENOTDIR'].includes(error.code) ? 404 : 500);
  }
});
server.listen(Number(process.env.PORT || 8081), process.env.HOST || '0.0.0.0', () => {
  console.log(JSON.stringify({ port: server.address().port, root }));
});
function shutdown() {
  server.close(() => process.exit(0));
  server.closeAllConnections();
  setTimeout(() => process.exit(0), 3000).unref();
}
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);
