import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'

// Refresh this many seconds before the access token actually expires
const EXPIRY_SKEW_SECONDS = 30

// Shared in-flight refresh, so concurrent callers (parallel 401s, guards) trigger a single request
let refreshPromise = null

const decodeToken = (token) => {
  try {
    return jwtDecode(token)
  } catch {
    return null
  }
}

// The localStorage keys are kept as customer_auth_* even though the app lives in its own bundle:
// the refresh token is an HttpOnly cookie scoped to /api/customer/auth, so renaming the keys would
// strand every customer who signed in before the portal was split out of the staff app.
export const useAuthStore = defineStore('auth', {
  state: () => {
    const token = localStorage.getItem('customer_auth_token')
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
      token: token || null,
      // The refresh token lives in an HttpOnly cookie the browser sends to /api/customer/auth
      user,
      isAuthenticated: !!token
    }
  },

  getters: {
    isLoggedIn: (state) => !!state.token && state.isAuthenticated,
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

      localStorage.setItem('customer_auth_token', this.token)
      localStorage.setItem('customer_auth_user', JSON.stringify(this.user))
    },

    clearSession() {
      this.token = null
      this.user = null
      this.isAuthenticated = false
      localStorage.removeItem('customer_auth_token')
      localStorage.removeItem('customer_auth_user')
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
          this.applySession(data)
          return { success: true }
        }
        return { success: false, error: response.data?.message || 'Registration failed' }
      } catch (error) {
        return { success: false, error: error.response?.data?.error || 'Registration failed' }
      }
    },

    async login(email, password) {
      try {
        const { api } = await import('boot/axios')
        const response = await api.post('/api/customer/auth/login', { email, password })
        const data = response.data?.data
        if (data?.token) {
          this.applySession(data)
          return { success: true }
        }
        return { success: false, error: response.data?.message || 'Login failed' }
      } catch (error) {
        return { success: false, error: error.response?.data?.error || 'Login failed' }
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
          if (data?.token) {
            this.applySession(data)
            return true
          }
          return false
        } catch (error) {
          console.error('Customer token refresh failed:', error)
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
      return this.refreshAccessToken()
    },

    markLinked() {
      if (this.user) {
        this.user.linked = true
        localStorage.setItem('customer_auth_user', JSON.stringify(this.user))
      }
    },

    async initializeAuth() {
      // Renew an expired token restored from localStorage right away; drop the session if that fails
      if (this.token && !(await this.ensureValidToken())) {
        console.warn('Stored customer session expired and could not be refreshed')
        this.clearSession()
      }
    }
  }
})
