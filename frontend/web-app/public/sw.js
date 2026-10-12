// Service worker mínimo: hace la consola instalable y muestra una página sin conexión.
// No guarda datos: nunca intercepta /api/ y las pantallas siempre se piden a la red.
const CACHE = 'auditnet-offline-v1';
const OFFLINE_URL = '/offline.html';
const PRECACHE = [OFFLINE_URL, '/favicon.svg', '/fonts/jetbrains-mono-800.woff2'];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(PRECACHE)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET' || request.mode !== 'navigate') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin || url.pathname.startsWith('/api/')) return;
  // Red primero; si no hay conexión, la página "sin conexión" guardada al instalar.
  event.respondWith(fetch(request).catch(() => caches.match(OFFLINE_URL)));
});
