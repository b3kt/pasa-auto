import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'
import { useAuthStore } from 'stores/auth-store'

// The customer portal talks exclusively to /api/customer/* (see boot/axios.js in ../../webui for the
// staff app's interceptor set). There is no lookup cache and no offline queue here: every request
// goes to the network, and offline reads are answered by the PWA service worker from the GET
// responses it cached while online (see src-pwa/custom-service-worker.js). Changes need the network.

// Always origin-relative: in production the bundle is served from the same origin as the API, and in
// development the portal is reached through Quarkus (http://<host>:8080/app/, proxied to 5174) or
// directly on 5174, whose dev server proxies /api to Quarkus (see quasar.config.js). Hardcoding
// localhost here would break every device other than the dev machine.
const api = axios.create({
  baseURL: '/',
  headers: {
    'Content-Type': 'application/json'
  }
})

// Endpoints that must never carry (or try to renew) an access token. The refresh token lives in an
// HttpOnly cookie the browser sends to /api/customer/auth, so /api/auth/refresh must stay token-free.
const PUBLIC_AUTH_URLS = [
  '/api/customer/auth/login',
  '/api/customer/auth/register',
  '/api/customer/auth/refresh'
]
const isPublicAuthUrl = (url) => PUBLIC_AUTH_URLS.some(p => url.startsWith(p))

// Set by the boot function below; used to send the customer back to the login page when the
// session ends
let appRouter = null

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

    return config
  }
)

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const requestUrl = originalRequest?.url || ''

    // The Owner turned the portal off (or the backend is down) while a customer was using it
    if (requestUrl.startsWith('/api/customer/') && error.response?.status === 503) {
      if (appRouter && appRouter.currentRoute.value.path !== '/unavailable') {
        appRouter.replace({ path: '/unavailable' })
      }
      return Promise.reject(error)
    }

    // Access token rejected: rotate it and retry once, then give up on the session
    if (error.response?.status === 401 && originalRequest && !isPublicAuthUrl(requestUrl)) {
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

    return Promise.reject(error)
  }
)

export default defineBoot(({ app, router }) => {
  appRouter = router

  app.config.globalProperties.$axios = axios
  app.config.globalProperties.$api = api
})

export { api }
