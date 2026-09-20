import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'
import { Notify } from 'quasar'
import syncService from '../services/syncService.js'
import browserCache from '../utils/browserCache.js'
import masterDataCache from '../utils/masterDataCache.js'
import { invalidateRelatedCaches } from '../utils/cacheInvalidation.js'
import { useAuthStore } from 'stores/auth-store'

// ── Lookup cache config ────────────────────────────────────────────────────
// Exact GET URLs that should be transparently cached in IndexedDB
const LOOKUP_CACHE_URLS = new Set([
  '/api/pazaauto/supplier',
  '/api/pazaauto/pelanggan',
  '/api/pazaauto/karyawan-posisi',
  '/api/roles',
  '/api/pazaauto/jasa',
  '/api/pazaauto/barang',
  '/api/pazaauto/kendaraan',
  '/api/pazaauto/kendaraan/merk/distinct',
  '/api/pazaauto/kendaraan/jenis/distinct',
  '/api/pazaauto/merk-kendaraan',
  '/api/pazaauto/sparepart',
  '/api/pazaauto/karyawan',
  '/api/system-parameters'
])

// Endpoint path of a request, without any query string
function requestPath(config) {
  return String(config?.url || '').split('?')[0]
}

// Cache key for a request: its path plus a stable (sorted) query string. Axios keeps query params
// in `config.params`, so keying on the URL alone would let requests that differ only by their
// params - a page, a filter, a parent id - share (and overwrite) one cache entry.
function requestCacheUrl(config) {
  const [path, inlineQuery = ''] = String(config?.url || '').split('?')
  const search = new URLSearchParams(inlineQuery)
  const params = config?.params

  if (params instanceof URLSearchParams) {
    params.forEach((value, key) => search.append(key, value))
  } else if (params && typeof params === 'object') {
    Object.entries(params)
      .filter(([, value]) => value !== undefined && value !== null)
      .forEach(([key, value]) => search.append(key, value))
  }

  search.sort()
  const query = search.toString()
  return query ? `${path}?${query}` : path
}

// Be careful when using SSR for cross-request state pollution
// due to creating a Singleton instance here;
// If any client changes this (global) instance, it might be a
// good idea to move this instance creation inside of the
// "export default () => {}" function below (which runs individually
// for each client)

// Determine base URL based on environment
const getBaseURL = () => {
  if (process.env.DEV) {
    // In development, Quinoa proxies API requests to Quarkus backend
    return 'http://localhost:8080'
  }
  // In production, use the same origin
  return '/'
}

const api = axios.create({
  baseURL: getBaseURL(),
  headers: {
    'Content-Type': 'application/json'
  }
})

// Auth endpoints that must never carry (or try to renew) an access token
// /api/auth/config is read on the login page before anyone is signed in. It has to be here:
// with proactive authentication on, a stale token in localStorage would make it 401 and tear the
// session down before @PermitAll is ever consulted.
const PUBLIC_AUTH_URLS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/config']
const isPublicAuthUrl = (url) => PUBLIC_AUTH_URLS.some(p => url.startsWith(p))

// Set by the boot function below; used to send the user to the login page when the session ends
let appRouter = null

// Session could not be renewed: drop it locally and go to the login page
const endSession = () => {
  useAuthStore().clearSession()
  const current = appRouter?.currentRoute.value
  if (current?.path === '/login') return
  if (appRouter) {
    appRouter.replace({ path: '/login', query: { expired: 'true' } })
  } else {
    window.location.hash = '#/login?expired=true'
  }
}

class SessionExpiredError extends Error {
  constructor() {
    super('Session expired')
    this.name = 'SessionExpiredError'
  }
}

// Add request interceptor to include auth token and handle offline mode
api.interceptors.request.use(
  async (config) => {
    const url = config.url || ''

    if (isPublicAuthUrl(url)) {
      return config
    }

    // Renew an expired access token before sending, so requests don't fail with 401 first
    const authStore = useAuthStore()
    if (authStore.token && !(await authStore.ensureValidToken())) {
      endSession()
      throw new SessionExpiredError()
    }
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }

    // Skip caching for auth endpoints
    if (url.startsWith('/api/auth')) {
      return config
    }

    // Check IndexedDB lookup cache for specific GET endpoints
    if (config.method?.toUpperCase() === 'GET' && LOOKUP_CACHE_URLS.has(requestPath(config))) {
      try {
        const cached = await masterDataCache.get(requestCacheUrl(config))
        if (cached) {
          config.adapter = () => Promise.resolve({
            data: cached,
            status: 200,
            statusText: 'OK (Cached)',
            headers: {},
            config: { ...config, _fromLookupCache: true },
            request: {}
          })
          return config
        }
      } catch (e) {
        console.warn('[axios] lookup cache read error:', e)
      }
    }

    // Check browser cache first for GET requests
    if (config.method?.toUpperCase() === 'GET' && !navigator.onLine) {
      const cachedResponse = browserCache.getCachedApiResponse(requestCacheUrl(config))
      if (cachedResponse) {
        Notify.create({
          type: 'info',
          message: 'Showing cached data (offline)',
          timeout: 2000
        })
        
        // Return cached response immediately
        return Promise.resolve(cachedResponse)
      }
    }

    // Handle offline mode - check if we should use offline functionality
    if (!navigator.onLine && ['GET', 'HEAD'].includes(config.method?.toUpperCase())) {
      // Try to get cached data for GET requests when offline
      try {
        const cacheKey = btoa(`${config.method}:${requestCacheUrl(config)}`).replace(/[+/=]/g, '')
        const cachedData = await syncService.storage.getData(cacheKey)
        
        if (cachedData) {
          // Return cached data immediately
          return Promise.resolve({
            data: cachedData,
            status: 200,
            statusText: 'OK (Offline)',
            headers: {},
            config,
            request: {},
            fromCache: true
          })
        }
      } catch (error) {
        console.warn('Failed to get cached data:', error)
      }
    }

    // Store request for potential offline sync if it's a mutation
    if (!navigator.onLine && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(config.method?.toUpperCase())) {
      try {
        await syncService.storage.storePendingRequest({
          url: config.url,
          method: config.method,
          headers: config.headers,
          body: JSON.stringify(config.data)
        })
        
        Notify.create({
          type: 'info',
          message: 'Request saved for offline sync',
          timeout: 2000
        })
        
        // Return immediate success response
        return Promise.resolve({
          data: { message: 'Request saved for offline sync', offline: true },
          status: 202,
          statusText: 'Accepted (Offline)',
          headers: {},
          config,
          request: {},
          fromCache: true
        })
      } catch (error) {
        console.error('Failed to store offline request:', error)
      }
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Add response interceptor to handle 401 errors, caching, and offline responses
api.interceptors.response.use(
  async (response) => {
    const method = response.config?.method?.toUpperCase()
    const url = response.config?.url || ''

    // Skip caching for auth endpoints
    if (url.startsWith('/api/auth')) {
      return response
    }

    // Store successful GET responses for cacheable lookup endpoints
    if (method === 'GET' && response.status === 200
        && !response.config?._fromLookupCache
        && LOOKUP_CACHE_URLS.has(requestPath(response.config))) {
      try {
        // Keyed by URL + params, but grouped under the plain endpoint so invalidation still finds it
        await masterDataCache.set(requestCacheUrl(response.config), response.data, requestPath(response.config))
      } catch (e) {
        console.warn('[axios] lookup cache write error:', e)
      }
    }

    // A successful write makes the cached responses of this endpoint - and of every entity
    // related to it - stale, so drop them from every browser cache before the caller refetches
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)
        && response.status >= 200 && response.status < 300) {
      try {
        await invalidateRelatedCaches(url)
      } catch (e) {
        console.warn('[axios] cache invalidation error:', e)
      }
    }

    // Cache successful GET responses in both service worker and browser cache
    if (method === 'GET' && response.status === 200) {
      try {
        // Cache in service worker storage
        const cacheUrl = requestCacheUrl(response.config)
        const cacheKey = btoa(`${response.config.method}:${cacheUrl}`).replace(/[+/=]/g, '')
        await syncService.storage.storeData(cacheKey, response.data, cacheUrl)

        // Cache in browser localStorage with longer TTL
        await browserCache.cacheApiResponse(cacheUrl, response, 60 * 60 * 1000) // 1 hour
      } catch (error) {
        console.warn('Failed to cache response:', error)
      }
    }

    return response
  },
  async (error) => {
    const originalRequest = error.config
    
    // Handle network errors (offline)
    if (!error.response && error.code === 'NETWORK_ERROR') {
      // Try browser cache first
      const cachedResponse = browserCache.getCachedApiResponse(requestCacheUrl(originalRequest))
      if (cachedResponse) {
        Notify.create({
          type: 'info',
          message: 'Showing cached data (offline)',
          timeout: 2000
        })
        return cachedResponse
      }
      
      // Try to get cached data for GET requests from service worker
      if (originalRequest.method?.toUpperCase() === 'GET') {
        try {
          const cacheKey = btoa(`${originalRequest.method}:${requestCacheUrl(originalRequest)}`).replace(/[+/=]/g, '')
          const cachedData = await syncService.storage.getData(cacheKey)
          
          if (cachedData) {
            Notify.create({
              type: 'info',
              message: 'Showing cached data (offline)',
              timeout: 2000
            })
            
            return Promise.resolve({
              data: cachedData,
              status: 200,
              statusText: 'OK (Cached)',
              headers: {},
              config: originalRequest,
              request: {},
              fromCache: true
            })
          }
        } catch (cacheError) {
          console.warn('Failed to get cached data:', cacheError)
        }
      }
      
      // Store mutation requests for later sync
      if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(originalRequest.method?.toUpperCase())) {
        try {
          await syncService.storage.storePendingRequest({
            url: originalRequest.url,
            method: originalRequest.method,
            headers: originalRequest.headers,
            body: JSON.stringify(originalRequest.data)
          })
          
          Notify.create({
            type: 'info',
            message: 'Request saved for offline sync',
            timeout: 2000
          })
          
          return Promise.resolve({
            data: { message: 'Request saved for offline sync', offline: true },
            status: 202,
            statusText: 'Accepted (Offline)',
            headers: {},
            config: originalRequest,
            request: {},
            fromCache: true
          })
        } catch (storageError) {
          console.error('Failed to store offline request:', storageError)
        }
      }
      
      Notify.create({
        type: 'negative',
        message: 'Network error. Working in offline mode.',
        timeout: 3000
      })
    }
    
    // Access token rejected: try to extend the session with the refresh token, then retry once.
    // If the session can't be renewed (or the retry is rejected too), log out.
    if (error.response?.status === 401 && originalRequest && !isPublicAuthUrl(originalRequest.url || '')) {
      if (originalRequest._retry) {
        endSession()
        return Promise.reject(error)
      }
      originalRequest._retry = true

      if (await useAuthStore().refreshAccessToken()) {
        // The request interceptor attaches the new token
        return api(originalRequest)
      }
      endSession()
    }

    // Authenticated but the role doesn't allow this endpoint
    if (error.response?.status === 403) {
      Notify.create({
        type: 'warning',
        message: 'Anda tidak memiliki akses untuk tindakan ini'
      })
    }
    return Promise.reject(error)
  }
)

export default defineBoot(({ app, router }) => {
  appRouter = router

  // for use inside Vue files (Options API) through this.$axios and this.$api

  app.config.globalProperties.$axios = axios
  // ^ ^ ^ this will allow you to use this.$axios (for Vue Options API form)
  //       so you won't necessarily have to import axios in each vue file

  app.config.globalProperties.$api = api
  // ^ ^ ^ this will allow you to use this.$api (for Vue Options API form)
  //       so you can easily perform requests against your app's API
})

export { api }
