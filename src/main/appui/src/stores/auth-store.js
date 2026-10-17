import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'

// Refresh this many seconds before the access token actually expires
const EXPIRY_SKEW_SECONDS = 30

// Cache Storage bucket the service worker keeps the customer's own API responses in (see
// src-pwa/custom-service-worker.js). It is personal data, so it is dropped whenever the signed-in
// customer may change: on sign-in, registration and every way a session ends.
const CUSTOMER_DATA_CACHE = 'customer-data'

const purgeCustomerDataCache = () => {
  if (!('caches' in window)) return
  caches.delete(CUSTOMER_DATA_CACHE).catch((error) => {
    console.error('Failed to clear cached customer data:', error)
  })
}

// Shared in-flight refresh, so concurrent callers (parallel 401s, guards) trigger a single request
let refreshPromise = null

const decodeToken = (token) => {
  try {
    return jwtDecode(token)
  } catch {
    return null
  }
}

// The remaining localStorage key is kept as customer_auth_user even though the app lives in its own
// bundle: the refresh token is an HttpOnly cookie scoped to /api/customer/auth, so renaming the key
// would strand every customer who signed in before the portal was split out of the staff app.
export const useAuthStore = defineStore('auth', {
  state: () => {
    // The access token is intentionally NOT persisted: page scripts (and any injected code) must not
    // be able to read it back out of storage. A logged-in session is restored on startup by
    // initializeAuth(), which exchanges the HttpOnly refresh cookie for a fresh access token.
    localStorage.removeItem('customer_auth_token') // drop any copy left behind by older builds
    const userJson = localStorage.getItem('customer_auth_user')
    let user = null

    if (userJson) {
      try {
        user = JSON.parse(userJson)
      } catch (e) {
        console.error('Failed to parse customer user data from localStorage', e)
      }
    }

    return {
      token: null,
      // The refresh token lives in an HttpOnly cookie the browser sends to /api/customer/auth
      user,
      isAuthenticated: false,
      initialized: false,
      // Whether the backend can be reached: kept current by the online/offline listeners in
      // boot/auth.js and by every refresh attempt, since a device can be "online" (captive Wi-Fi, a
      // server that is down) without reaching the API
      online: navigator.onLine
    }
  },

  getters: {
    isLoggedIn: (state) => !!state.token && state.isAuthenticated,
    // Offline the access token cannot be minted (it is never persisted), but a customer who was
    // signed in may still read the pages the service worker cached. Nothing can be changed until the
    // session is renewed back online.
    isOfflineSession: (state) => !state.online && !state.token && !!state.user,
    canBrowse() {
      return this.isLoggedIn || this.isOfflineSession
    },
    isLinked: (state) => !!state.user?.linked
  },

  actions: {
    applySession({ token, email, fullName, linked }) {
      const claims = decodeToken(token) || {}
      this.token = token
      this.user = {
        email: email || claims.email || this.user?.email,
        fullName: fullName || claims.fullName || this.user?.fullName,
        linked: linked ?? (claims.pelangganId !== undefined)
      }
      this.isAuthenticated = true

      // Only non-sensitive profile data is persisted; the access token stays in memory
      localStorage.setItem('customer_auth_user', JSON.stringify(this.user))
    },

    clearSession() {
      this.token = null
      this.user = null
      this.isAuthenticated = false
      localStorage.removeItem('customer_auth_token')
      localStorage.removeItem('customer_auth_user')
      purgeCustomerDataCache()
    },

    setOnline(online) {
      this.online = online
    },

    isTokenExpired() {
      const exp = decodeToken(this.token)?.exp
      if (!exp) return true
      return Date.now() / 1000 >= exp - EXPIRY_SKEW_SECONDS
    },

    async register(payload) {
      try {
        const { api } = await import('boot/axios')
        const response = await api.post('/api/customer/auth/register', payload)
        const data = response.data?.data
        if (data?.token) {
          purgeCustomerDataCache()
          this.applySession(data)
          return { success: true }
        }
        return { success: false, error: response.data?.message || null }
      } catch (error) {
        return { success: false, error: error.response?.data?.error || null }
      }
    },

    /** @param identifier email or phone number */
    async login(identifier, password) {
      try {
        const { api } = await import('boot/axios')
        const response = await api.post('/api/customer/auth/login', { identifier, password })
        const data = response.data?.data
        if (data?.token) {
          purgeCustomerDataCache()
          this.applySession(data)
          return { success: true }
        }
        return { success: false, error: response.data?.message || null }
      } catch (error) {
        return { success: false, error: error.response?.data?.error || null }
      }
    },

    async logout() {
      const token = this.token
      try {
        const { api } = await import('boot/axios')
        if (token) {
          // The server reads the refresh cookie, revokes that session and clears the cookie
          await api.post('/api/customer/auth/logout', null, { headers: { Authorization: `Bearer ${token}` } })
        }
      } catch (error) {
        console.error('Customer logout error:', error)
      } finally {
        this.clearSession()
      }
    },

    // Exchange the refresh token for a new access token. Resolves true when the session was extended.
    refreshAccessToken() {
      if (refreshPromise) return refreshPromise

      refreshPromise = (async () => {
        try {
          const { api } = await import('boot/axios')
          // No body: the browser sends the HttpOnly refresh cookie
          const response = await api.post('/api/customer/auth/refresh', {})
          const data = response.data?.data
          this.online = true
          if (data?.token) {
            this.applySession(data)
            return true
          }
          return false
        } catch (error) {
          console.error('Customer token refresh failed:', error)
          // No response at all: the backend was not reached, which says nothing about the session
          this.online = !!error.response
          return false
        } finally {
          refreshPromise = null
        }
      })()
      return refreshPromise
    },

    // Make sure a usable access token is available, refreshing it when it has expired.
    // Resolves false when the session could not be renewed.
    async ensureValidToken() {
      if (!this.token) return false
      if (!this.isTokenExpired()) return true
      if (!navigator.onLine) return true
      if (await this.refreshAccessToken()) return true
      // Unreachable backend: keep the expired token for now (requests fall back to the service
      // worker's cache) instead of ending the session; a 401 later triggers a real refresh
      return !this.online
    },

    // Turn a stored customer back into a signed-in session from the refresh cookie. Only a refresh
    // the backend actually rejects ends the session; while it cannot be reached, the customer stays
    // in an offline session (cached pages only). Resolves true when signed in.
    async resumeSession() {
      if (this.token) return true
      if (!this.user) return false
      if (!navigator.onLine) {
        this.online = false
        return false
      }
      if (await this.refreshAccessToken()) return true
      if (this.online) {
        // The previous session's refresh cookie is gone or rejected: drop it
        this.clearSession()
      }
      return false
    },

    markLinked() {
      if (this.user) {
        this.user.linked = true
        localStorage.setItem('customer_auth_user', JSON.stringify(this.user))
      }
    },

    async initializeAuth() {
      if (this.initialized) return
      this.initialized = true

      // A fresh access token is minted from the HttpOnly refresh cookie. Without a stored customer
      // a refresh is still attempted (the cookie may outlive localStorage).
      if (this.user) {
        await this.resumeSession()
      } else if (!this.token && navigator.onLine) {
        await this.refreshAccessToken()
      }
    }
  }
})
