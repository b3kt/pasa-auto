import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'
import { Notify } from 'quasar'
import syncService from '../services/syncService.js'
import browserCache from '../utils/browserCache.js'
import masterDataCache from '../utils/masterDataCache.js'

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
  '/api/pazaauto/kendaraan/jenis/distinct'
])

// When a write (POST/PUT/DELETE/PATCH) succeeds on a URL under a given prefix,
// all cache entries with those prefixes are invalidated.
const WRITE_INVALIDATION_MAP = [
  { prefix: '/api/pazaauto/jasa',          invalidate: ['/api/pazaauto/jasa'] },
  { prefix: '/api/pazaauto/barang',        invalidate: ['/api/pazaauto/barang'] },
  { prefix: '/api/pazaauto/supplier',      invalidate: ['/api/pazaauto/supplier'] },
  { prefix: '/api/pazaauto/pelanggan',     invalidate: ['/api/pazaauto/pelanggan'] },
  { prefix: '/api/pazaauto/karyawan-posisi', invalidate: ['/api/pazaauto/karyawan-posisi'] },
  { prefix: '/api/roles',                  invalidate: ['/api/roles'] },
  {
    prefix: '/api/pazaauto/kendaraan',
    invalidate: [
      '/api/pazaauto/kendaraan',
      '/api/pazaauto/kendaraan/merk/distinct',
      '/api/pazaauto/kendaraan/jenis/distinct'
    ]
  }
]

function getLookupInvalidationUrls(writeUrl) {
  for (const { prefix, invalidate } of WRITE_INVALIDATION_MAP) {
    if (writeUrl === prefix || writeUrl.startsWith(prefix + '/')) {
      return invalidate
    }
  }
  return []
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

// Add request interceptor to include auth token and handle offline mode
api.interceptors.request.use(
  async (config) => {
    const token = localStorage.getItem('auth_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // Check IndexedDB lookup cache for specific GET endpoints
    if (config.method?.toUpperCase() === 'GET' && LOOKUP_CACHE_URLS.has(config.url)) {
      try {
        const cached = await masterDataCache.get(config.url)
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
      const cachedResponse = browserCache.getCachedApiResponse(config.url)
      if (cachedResponse) {
        Notify.create({
          type: 'info',
          message: 'Showing cached data (offline)',
          position: 'top-right',
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
        const cacheKey = btoa(`${config.method}:${config.url}`).replace(/[+/=]/g, '')
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
          position: 'top-right',
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

// Flag to prevent multiple simultaneous refresh attempts
let isRefreshing = false
let failedQueue = []

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

// Add response interceptor to handle 401 errors, caching, and offline responses
api.interceptors.response.use(
  async (response) => {
    const method = response.config?.method?.toUpperCase()

    // Store successful GET responses for cacheable lookup endpoints
    if (method === 'GET' && response.status === 200
        && !response.config?._fromLookupCache
        && LOOKUP_CACHE_URLS.has(response.config?.url)) {
      try {
        await masterDataCache.set(response.config.url, response.data, response.config.url)
      } catch (e) {
        console.warn('[axios] lookup cache write error:', e)
      }
    }

    // Invalidate lookup + paginated caches on successful writes
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)
        && response.status >= 200 && response.status < 300) {
      try {
        const toInvalidate = getLookupInvalidationUrls(response.config?.url)
        if (toInvalidate.length > 0) {
          await Promise.all(toInvalidate.map(url => masterDataCache.invalidatePrefix(url)))
        }
      } catch (e) {
        console.warn('[axios] lookup cache invalidation error:', e)
      }
    }

    // Cache successful GET responses in both service worker and browser cache
    if (method === 'GET' && response.status === 200) {
      try {
        // Cache in service worker storage
        const cacheKey = btoa(`${response.config.method}:${response.config.url}`).replace(/[+/=]/g, '')
        await syncService.storage.storeData(cacheKey, response.data, response.config.url)

        // Cache in browser localStorage with longer TTL
        await browserCache.cacheApiResponse(response.config.url, response, 60 * 60 * 1000) // 1 hour
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
      const cachedResponse = browserCache.getCachedApiResponse(originalRequest.url)
      if (cachedResponse) {
        Notify.create({
          type: 'info',
          message: 'Showing cached data (offline)',
          position: 'top-right',
          timeout: 2000
        })
        return cachedResponse
      }
      
      // Try to get cached data for GET requests from service worker
      if (originalRequest.method?.toUpperCase() === 'GET') {
        try {
          const cacheKey = btoa(`${originalRequest.method}:${originalRequest.url}`).replace(/[+/=]/g, '')
          const cachedData = await syncService.storage.getData(cacheKey)
          
          if (cachedData) {
            Notify.create({
              type: 'info',
              message: 'Showing cached data (offline)',
              position: 'top-right',
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
            position: 'top-right',
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
        position: 'top-right',
        timeout: 3000
      })
    }
    
    // If it's a 401 and not a refresh request itself, try to refresh
    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/api/auth/refresh')) {
      if (isRefreshing) {
        // If already refreshing, queue this request
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return api(originalRequest)
        }).catch(err => {
          return Promise.reject(err)
        })
      }
      
      originalRequest._retry = true
      isRefreshing = true
      
      try {
        // Try to refresh the token
        const refreshToken = localStorage.getItem('refresh_token')
        if (refreshToken) {
          const response = await api.post('/api/auth/refresh', { refreshToken })
          const tokenData = response.data?.data
          
          if (tokenData?.token) {
            // Update tokens in localStorage
            localStorage.setItem('auth_token', tokenData.token)
            localStorage.setItem('refresh_token', tokenData.refreshToken)
            
            // Update the authorization header
            api.defaults.headers.common['Authorization'] = `Bearer ${tokenData.token}`
            originalRequest.headers.Authorization = `Bearer ${tokenData.token}`
            
            processQueue(null, tokenData.token)
            
            // Retry the original request
            return api(originalRequest)
          }
        }
      } catch (refreshError) {
        processQueue(refreshError, null)
        // Refresh failed, redirect to login
        delete api.defaults.headers.common['Authorization']
        localStorage.removeItem('auth_token')
        localStorage.removeItem('refresh_token')
        localStorage.removeItem('auth_user')
        
        // Use hash-based route for SPA
        if (!window.location.hash.includes('/login')) {
          window.location.href = '/#/login'
        }
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
      
      // If no refresh token, redirect to login
      delete api.defaults.headers.common['Authorization']
      localStorage.removeItem('auth_token')
      localStorage.removeItem('refresh_token')
      localStorage.removeItem('auth_user')
      if (!window.location.hash.includes('/login')) {
        window.location.href = '/#/login'
      }
    }
    return Promise.reject(error)
  }
)

export default defineBoot(({ app }) => {
  // for use inside Vue files (Options API) through this.$axios and this.$api

  app.config.globalProperties.$axios = axios
  // ^ ^ ^ this will allow you to use this.$axios (for Vue Options API form)
  //       so you won't necessarily have to import axios in each vue file

  app.config.globalProperties.$api = api
  // ^ ^ ^ this will allow you to use this.$api (for Vue Options API form)
  //       so you can easily perform requests against your app's API
})

export { api }
