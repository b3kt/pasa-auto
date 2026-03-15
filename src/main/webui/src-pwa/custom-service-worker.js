import { precacheAndRoute, cleanupOutdatedCaches } from 'workbox-precaching'
import { registerRoute } from 'workbox-routing'
import { NetworkFirst, CacheFirst, StaleWhileRevalidate } from 'workbox-strategies'
import { CacheableResponsePlugin } from 'workbox-cacheable-response'
import { ExpirationPlugin } from 'workbox-expiration'

// Precache all assets for offline functionality
precacheAndRoute(self.__WB_MANIFEST)
cleanupOutdatedCaches()

// Install event - pre-cache critical assets
self.addEventListener('install', (event) => {
  console.log('Service Worker installing...', event)
  self.skipWaiting()
})

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  console.log('Service Worker activating...')
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.filter((cacheName) => {
          // Keep only current caches
          return cacheName.startsWith('vue-assets-') ||
                 cacheName.startsWith('static-assets-') ||
                 cacheName.startsWith('html-') ||
                 cacheName.startsWith('api-') ||
                 cacheName.startsWith('google-fonts-')
        }).map((cacheName) => {
          return caches.delete(cacheName)
        })
      )
    })
  )
})

// Cache API calls with NetworkFirst strategy
registerRoute(
  ({ url }) => url.origin.includes('trycloudflare.com'),
  new NetworkFirst({
    cacheName: 'api-cache',
    plugins: [
      new CacheableResponsePlugin({
        statuses: [0, 200]
      }),
      new ExpirationPlugin({
        maxEntries: 100,
        maxAgeSeconds: 60 * 60 * 24 // 24 hours
      })
    ]
  })
)

// Cache Vue app assets (JS, CSS, Vue files) with StaleWhileRevalidate for performance
registerRoute(
  ({ request }) =>
    request.destination === 'script' ||
    request.destination === 'style' ||
    request.url.includes('.js') ||
    request.url.includes('.mjs') ||
    request.url.includes('.ts') ||
    request.url.includes('.vue') ||
    request.url.includes('.css') ||
    request.url.includes('.scss') ||
    request.url.includes('.sass'),
  new StaleWhileRevalidate({
    cacheName: 'vue-assets-cache',
    plugins: [
      new CacheableResponsePlugin({
        statuses: [0, 200]
      }),
      new ExpirationPlugin({
        maxEntries: 200, // Increased for more files
        maxAgeSeconds: 60 * 60 * 24 * 7 // 7 days
      })
    ]
  })
)

// Cache Quasar framework files and extras
registerRoute(
  ({ request }) =>
    request.url.includes('/node_modules/@quasar/') ||
    request.url.includes('/node_modules/quasar/') ||
    request.url.includes('quasar_lang_') ||
    request.url.includes('quasar_dist_quasar') ||
    request.url.includes('@quasar_app-vite_wrappers') ||
    request.url.includes('roboto-font') ||
    request.url.includes('material-icons') ||
    request.url.includes('mdi-v7'),
  new CacheFirst({
    cacheName: 'quasar-framework-cache',
    plugins: [
      new CacheableResponsePlugin({
        statuses: [0, 200]
      }),
      new ExpirationPlugin({
        maxEntries: 100,
        maxAgeSeconds: 60 * 60 * 24 * 30 // 30 days for framework files
      })
    ]
  })
)

// Cache Vite chunks and dependencies
registerRoute(
  ({ request }) =>
    request.url.includes('chunk-') ||
    request.url.includes('/node_modules/.q-cache/') ||
    request.url.includes('/node_modules/vue/') ||
    request.url.includes('/node_modules/vue-router/') ||
    request.url.includes('/node_modules/pinia/') ||
    request.url.includes('/node_modules/jwt-decode/') ||
    request.url.includes('/node_modules/axios/'),
  new CacheFirst({
    cacheName: 'vite-chunks-cache',
    plugins: [
      new CacheableResponsePlugin({
        statuses: [0, 200]
      }),
      new ExpirationPlugin({
        maxEntries: 150,
        maxAgeSeconds: 60 * 60 * 24 * 30 // 30 days for chunks
      })
    ]
  })
)

// Cache CSS files separately for better performance
registerRoute(
  ({ request }) =>
    request.destination === 'style' ||
    request.url.includes('.css') ||
    request.url.includes('.scss') ||
    request.url.includes('.sass') ||
    request.url.includes('.less'),
  new StaleWhileRevalidate({
    cacheName: 'css-cache',
    plugins: [
      new CacheableResponsePlugin({
        statuses: [0, 200]
      }),
      new ExpirationPlugin({
        maxEntries: 50,
        maxAgeSeconds: 60 * 60 * 24 * 7 // 7 days for CSS
      })
    ]
  })
)

// Cache static assets (images, fonts) with CacheFirst strategy
registerRoute(
  ({ request }) =>
    request.destination === 'image' ||
    request.destination === 'font' ||
    request.url.includes('.png') ||
    request.url.includes('.jpg') ||
    request.url.includes('.jpeg') ||
    request.url.includes('.gif') ||
    request.url.includes('.svg') ||
    request.url.includes('.webp') ||
    request.url.includes('.ico') ||
    request.url.includes('.woff') ||
    request.url.includes('.woff2') ||
    request.url.includes('.ttf') ||
    request.url.includes('.eot'),
  new CacheFirst({
    cacheName: 'static-assets-cache',
    plugins: [
      new CacheableResponsePlugin({
        statuses: [0, 200]
      }),
      new ExpirationPlugin({
        maxEntries: 100,
        maxAgeSeconds: 60 * 60 * 24 * 30 // 30 days
      })
    ]
  })
)

// Cache HTML pages with NetworkFirst for fresh content
registerRoute(
  ({ request }) =>
    request.destination === 'document' ||
    request.url.includes('.html'),
  new NetworkFirst({
    cacheName: 'html-cache',
    plugins: [
      new CacheableResponsePlugin({
        statuses: [0, 200]
      }),
      new ExpirationPlugin({
        maxEntries: 20,
        maxAgeSeconds: 60 * 60 * 24 // 24 hours
      })
    ]
  })
)

// Cache Google Fonts with CacheFirst strategy
registerRoute(
  ({ url }) => url.origin.includes('fonts.googleapis.com') || url.origin.includes('fonts.gstatic.com'),
  new CacheFirst({
    cacheName: 'google-fonts-cache',
    plugins: [
      new CacheableResponsePlugin({
        statuses: [0, 200]
      }),
      new ExpirationPlugin({
        maxEntries: 10,
        maxAgeSeconds: 60 * 60 * 24 * 365 // 1 year
      })
    ]
  })
)

// Cache manifest and other PWA files
registerRoute(
  ({ request }) =>
    request.url.includes('manifest.json') ||
    request.url.includes('sw.js') ||
    request.url.includes('favicon'),
  new CacheFirst({
    cacheName: 'pwa-cache',
    plugins: [
      new CacheableResponsePlugin({
        statuses: [0, 200]
      }),
      new ExpirationPlugin({
        maxEntries: 10,
        maxAgeSeconds: 60 * 60 * 24 * 30 // 30 days
      })
    ]
  })
)

// Fallback for offline navigation
registerRoute(
  ({ request }) => request.mode === 'navigate',
  async ({ request }) => {
    try {
      // Try network first
      return await fetch(request)
    } catch (error) {
      console.error('Failed to fetch request:', error)
      // If offline, try cache
      const cachedResponse = await caches.match(request)
      if (cachedResponse) {
        return cachedResponse
      }

      // If nothing in cache, return offline fallback
      return new Response(`
        <!DOCTYPE html>
        <html>
          <head>
            <title>Offline - PASA AUTO</title>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
              body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
              .offline { color: #666; }
              .retry { background: #1976d2; color: white; padding: 10px 20px; border: none; cursor: pointer; }
            </style>
          </head>
          <body>
            <div class="offline">
              <h1>You're offline</h1>
              <p>PASA AUTO is running in offline mode.</p>
              <p>Cached content is available.</p>
              <button class="retry" onclick="window.location.reload()">Retry Connection</button>
            </div>
          </body>
        </html>
      `, {
        status: 200,
        statusText: 'OK',
        headers: { 'Content-Type': 'text/html' }
      })
    }
  }
)

// Background sync for offline actions
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-data') {
    event.waitUntil(syncData())
  }
})

// Handle offline POST/PUT/DELETE requests
self.addEventListener('fetch', (event) => {
  if (event.request.method === 'POST' || event.request.method === 'PUT' || event.request.method === 'DELETE') {
    event.respondWith(handleMutationRequest(event.request))
  }
})

async function handleMutationRequest(request) {
  try {
    // Try to make the request
    const response = await fetch(request.clone())
    return response
  } catch (error) {
    console.error('Failed to make request:', error)
    // If offline, store the request for later sync
    const requestData = {
      url: request.url,
      method: request.method,
      headers: Object.fromEntries(request.headers.entries()),
      body: await request.text(),
      timestamp: Date.now()
    }

    // Store in IndexedDB for later sync
    const db = await openDB()
    await db.add('offlineRequests', requestData)

    return new Response(JSON.stringify({
      message: 'Request stored for offline sync',
      offline: true
    }), {
      status: 202,
      headers: { 'Content-Type': 'application/json' }
    })
  }
}

async function syncData() {
  try {
    const db = await openDB()
    const offlineRequests = await db.getAll('offlineRequests')

    for (const requestData of offlineRequests) {
      try {
        const response = await fetch(requestData.url, {
          method: requestData.method,
          headers: requestData.headers,
          body: requestData.body
        })

        if (response.ok) {
          // Remove synced request from IndexedDB
          await db.delete('offlineRequests', requestData.id)
          console.log('Successfully synced request:', requestData.url)
        }
      } catch (error) {
        console.error('Failed to sync request:', requestData.url, error)
      }
    }
  } catch (error) {
    console.error('Sync failed:', error)
  }
}

// IndexedDB helper
function openDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('offlineDB', 1)

    request.onerror = () => reject(request.error)
    request.onsuccess = () => resolve(request.result)

    request.onupgradeneeded = (event) => {
      const db = event.target.result
      if (!db.objectStoreNames.contains('offlineRequests')) {
        const store = db.createObjectStore('offlineRequests', {
          keyPath: 'id',
          autoIncrement: true
        })
        store.createIndex('timestamp', 'timestamp')
      }
    }
  })
}

// Push notification for sync status
self.addEventListener('push', (event) => {
  if (event.data) {
    const data = event.data.json()
    event.waitUntil(
      self.registration.showNotification(data.title, {
        body: data.body,
        icon: '/icons/favicon-128x128.png'
      })
    )
  }
})

// Listen for message from client
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting()
  }
})
