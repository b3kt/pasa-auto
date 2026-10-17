/*
 * Service worker for the customer portal, built by Quasar with Workbox in "InjectManifest" mode
 * (quasar.config file > pwa > workboxMode). self.__WB_MANIFEST is replaced at build time with the
 * list of built assets, which are precached below.
 *
 * Offline support has three layers:
 *  1. The app shell (index.html, hashed JS/CSS, fonts, icons) is precached, and index.html is served
 *     as the navigation fallback, so the portal loads with no network at all.
 *  2. Any other same-origin static resource under the portal is cached on first use.
 *  3. The customer's own read-only API data (GET /api/customer/*) and the public portal switch are
 *     cached network-first, so pages visited online can be shown again offline.
 *
 * The API cache holds personal data, so it is kept to the minimum: GETs only, never the auth
 * endpoints or the rate-limited plate lookups, a short expiry, and it is deleted by the page on
 * every sign-in and sign-out (see CUSTOMER_DATA_CACHE in src/stores/auth-store.js).
 */
import { clientsClaim } from 'workbox-core'
import { precacheAndRoute, cleanupOutdatedCaches, createHandlerBoundToURL } from 'workbox-precaching'
import { registerRoute, NavigationRoute } from 'workbox-routing'
import { NetworkFirst, StaleWhileRevalidate } from 'workbox-strategies'
import { CacheableResponsePlugin } from 'workbox-cacheable-response'
import { ExpirationPlugin } from 'workbox-expiration'

// Keep in sync with src/stores/auth-store.js, which deletes this cache on sign-in and sign-out
const CUSTOMER_DATA_CACHE = 'customer-data'
const PORTAL_STATUS_CACHE = 'portal-status'
const STATIC_CACHE = 'portal-static'

// How long a slow network gets before the cached copy is shown instead
const NETWORK_TIMEOUT_SECONDS = 5

self.skipWaiting()
clientsClaim()

// 1. Precache everything the build emitted (index.html and the hashed JS/CSS/font assets)
precacheAndRoute(self.__WB_MANIFEST)
cleanupOutdatedCaches()

// Serve the precached app shell for navigations so a deep link or refresh works offline. The API
// and the service worker/Workbox files themselves are excluded.
if (process.env.PROD) {
  registerRoute(
    new NavigationRoute(createHandlerBoundToURL(process.env.PWA_FALLBACK_HTML), {
      denylist: [
        new RegExp(process.env.PWA_SERVICE_WORKER_REGEX),
        /workbox-(.)*\.js$/,
        /^\/api\//
      ]
    })
  )
}

// 2. Same-origin static resources the precache does not list (e.g. assets added outside the build)
const scopePath = new URL(self.registration.scope).pathname
registerRoute(
  ({ url, request }) =>
    url.origin === self.location.origin &&
    url.pathname.startsWith(scopePath) &&
    ['image', 'font', 'style', 'script', 'manifest'].includes(request.destination),
  new StaleWhileRevalidate({
    cacheName: STATIC_CACHE,
    plugins: [
      new CacheableResponsePlugin({ statuses: [200] }),
      new ExpirationPlugin({ maxEntries: 60, maxAgeSeconds: 60 * 60 * 24 * 30 })
    ]
  })
)

// 3a. The portal on/off switch. The router checks it before every navigation and fails closed, so
// without a cached copy every offline navigation would land on /unavailable.
registerRoute(
  ({ url }) => url.origin === self.location.origin && url.pathname === '/api/public/customer-access-status',
  new NetworkFirst({
    cacheName: PORTAL_STATUS_CACHE,
    networkTimeoutSeconds: NETWORK_TIMEOUT_SECONDS,
    plugins: [new CacheableResponsePlugin({ statuses: [200] })]
  }),
  'GET'
)

// 3b. The customer's own records. Lookups that only make sense against live data (plate checks are
// rate-limited and feed a claim that needs the network anyway) and the auth endpoints are skipped.
const UNCACHED_CUSTOMER_PATHS = [
  /^\/api\/customer\/auth\//,
  /^\/api\/customer\/vehicles\/check-nopol/,
  /^\/api\/customer\/link\/search-by-nopol\//
]

registerRoute(
  ({ url }) =>
    url.origin === self.location.origin &&
    url.pathname.startsWith('/api/customer/') &&
    !UNCACHED_CUSTOMER_PATHS.some((pattern) => pattern.test(url.pathname)),
  new NetworkFirst({
    cacheName: CUSTOMER_DATA_CACHE,
    networkTimeoutSeconds: NETWORK_TIMEOUT_SECONDS,
    // Online requests carry an Authorization header and offline ones do not (the access token is
    // never persisted), so a "Vary: Authorization" response must still match the offline request
    matchOptions: { ignoreVary: true },
    plugins: [
      new CacheableResponsePlugin({ statuses: [200] }),
      new ExpirationPlugin({ maxEntries: 50, maxAgeSeconds: 60 * 60 * 24 * 7 })
    ]
  }),
  'GET'
)
