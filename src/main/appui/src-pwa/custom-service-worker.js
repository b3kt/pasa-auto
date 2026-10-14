/*
 * Service worker for the customer portal, built by Quasar with Workbox in "InjectManifest" mode
 * (quasar.config file > pwa > workboxMode). self.__WB_MANIFEST is replaced at build time with the
 * list of built assets, which are precached below.
 *
 * Unlike the staff app (../webui/src-pwa/custom-service-worker.js), this worker keeps no runtime
 * cache for the API: a customer only ever reads their own record (see src/boot/axios.js), so caching
 * /api responses would leave personal data in the browser's Cache Storage. Only the app shell is
 * precached, and it is served as the navigation fallback so the portal still loads while offline;
 * API requests always go straight to the network.
 */
import { clientsClaim } from 'workbox-core'
import { precacheAndRoute, cleanupOutdatedCaches, createHandlerBoundToURL } from 'workbox-precaching'
import { registerRoute, NavigationRoute } from 'workbox-routing'

self.skipWaiting()
clientsClaim()

// Precache everything the build emitted (index.html and the hashed JS/CSS/font assets)
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
