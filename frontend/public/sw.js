const CACHE_VERSION = "vivent-pwa-v1";
const PRECACHE_CACHE = `${CACHE_VERSION}-precache`;
const STATIC_CACHE = `${CACHE_VERSION}-static`;

const PRECACHE_URLS = [
  "/offline.html",
  "/favicon-16x16.png",
  "/favicon-32x32.png",
  "/apple-touch-icon.png",
  "/icons/icon-192x192.png",
  "/icons/icon-512x512.png",
  "/icons/icon-maskable-512x512.png",
  "/brand/vivent-icon.png",
  "/brand/vivent-banner.png",
];

function isSameOrigin(url) {
  return url.origin === self.location.origin;
}

function isApiRequest(url) {
  return url.pathname.startsWith("/api/");
}

function isStaticAsset(url) {
  return (
    isSameOrigin(url) &&
    (url.pathname.startsWith("/_next/static/") ||
      url.pathname.startsWith("/icons/") ||
      url.pathname.startsWith("/brand/") ||
      url.pathname === "/favicon-16x16.png" ||
      url.pathname === "/favicon-32x32.png" ||
      url.pathname === "/apple-touch-icon.png")
  );
}

async function deleteOldCaches() {
  const cacheNames = await caches.keys();
  const currentCaches = new Set([PRECACHE_CACHE, STATIC_CACHE]);

  await Promise.all(cacheNames.filter((cacheName) => !currentCaches.has(cacheName)).map((cacheName) => caches.delete(cacheName)));
}

async function getOfflineFallback() {
  const cache = await caches.open(PRECACHE_CACHE);
  const fallback = await cache.match("/offline.html");

  return fallback || new Response("Vivent is offline.", { status: 503, headers: { "Content-Type": "text/plain" } });
}

async function staleWhileRevalidate(request) {
  const cache = await caches.open(STATIC_CACHE);
  const cachedResponse = await cache.match(request);

  const networkResponsePromise = fetch(request)
    .then((response) => {
      if (response.ok) {
        cache.put(request, response.clone());
      }

      return response;
    })
    .catch(() => cachedResponse || Response.error());

  return cachedResponse || networkResponsePromise;
}

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(PRECACHE_CACHE).then((cache) => cache.addAll(PRECACHE_URLS)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (event) => {
  event.waitUntil(deleteOldCaches().then(() => self.clients.claim()));
});

self.addEventListener("message", (event) => {
  if (event.data?.type === "SKIP_WAITING") {
    self.skipWaiting();
  }
});

self.addEventListener("fetch", (event) => {
  const { request } = event;

  if (request.method !== "GET") {
    return;
  }

  const url = new URL(request.url);

  if (!isSameOrigin(url) || isApiRequest(url)) {
    return;
  }

  if (request.mode === "navigate") {
    event.respondWith(fetch(request).catch(() => getOfflineFallback()));
    return;
  }

  if (isStaticAsset(url)) {
    event.respondWith(staleWhileRevalidate(request));
  }
});
