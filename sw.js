// Blue ON · Service Worker
// Estratégia: cache-first para o app shell, network-first com fallback para HTML, cache de imagens externas autorizadas
const VERSION = 'blueon-v3.3.0-jarvis-bpi-voz';
const IMG_HOSTS = ['themealdb.com','www.themealdb.com','images.openfoodfacts.org','static.openfoodfacts.org','wger.de','www.wger.de'];
const CORE_CACHE = `core-${VERSION}`;
const RUNTIME_CACHE = `runtime-${VERSION}`;

const CORE_ASSETS = [
  './',
  './blue-on-app.html',
  './manifest.json',
  './icon-192.png',
  './icon-512.png',
  './apple-touch-icon.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CORE_CACHE)
      .then((cache) => cache.addAll(CORE_ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((k) => ![CORE_CACHE, RUNTIME_CACHE].includes(k))
          .map((k) => caches.delete(k))
      )
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);

  // Imagens de hosts autorizados (TheMealDB, Open Food Facts): cache-first com fetch em background
  if (url.origin !== location.origin) {
    if (IMG_HOSTS.includes(url.hostname) && request.destination === 'image') {
      event.respondWith(
        caches.match(request).then((cached) => {
          if (cached) return cached;
          return fetch(request).then((response) => {
            if (response && response.status === 200) {
              const copy = response.clone();
              caches.open(RUNTIME_CACHE).then((c) => c.put(request, copy));
            }
            return response;
          }).catch(() => new Response('', {status: 404}));
        })
      );
      return;
    }
    return; // outros pedidos externos passam direto
  }

  // Navegação (HTML): network-first com fallback ao cache
  if (request.mode === 'navigate' || request.destination === 'document') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copy = response.clone();
          caches.open(RUNTIME_CACHE).then((c) => c.put(request, copy));
          return response;
        })
        .catch(() => caches.match(request).then((r) => r || caches.match('./blue-on-app.html')))
    );
    return;
  }

  // Outros assets: cache-first
  event.respondWith(
    caches.match(request).then((cached) => {
      if (cached) return cached;
      return fetch(request).then((response) => {
        if (response && response.status === 200 && response.type === 'basic') {
          const copy = response.clone();
          caches.open(RUNTIME_CACHE).then((c) => c.put(request, copy));
        }
        return response;
      });
    })
  );
});

// Mensagem para forçar atualização (chamável da app via SW.update)
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') self.skipWaiting();
});
