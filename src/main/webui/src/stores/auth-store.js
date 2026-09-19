import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'
import { clearUserCaches } from 'src/utils/clearUserCaches'

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

export const useAuthStore = defineStore('auth', {
  state: () => {
    const token = localStorage.getItem('auth_token')
    const userJson = localStorage.getItem('auth_user')
    let user = null

    if (userJson) {
      try {
        user = JSON.parse(userJson)
      } catch (e) {
        console.error('Failed to parse user data from localStorage', e)
      }
    }

    return {
      token: token || null,
      refreshToken: localStorage.getItem('refresh_token') || null,
      user: user,
      isAuthenticated: !!token // Set to true if token exists
    }
  },

  getters: {
    isLoggedIn: (state) => !!state.token && state.isAuthenticated
  },

  actions: {
    // Store a freshly issued token pair and derive the user from the access token claims
    applySession({ token, refreshToken, username, email }) {
      const claims = decodeToken(token) || {}
      this.token = token
      this.refreshToken = refreshToken
      this.user = {
        username: username || claims.upn || claims.sub || this.user?.username,
        email: email || claims.email || this.user?.email,
        roles: claims.groups,
        karyawanId: claims.karyawanId,
        karyawanNama: claims.karyawanNama,
        // Temporary password: the server refuses everything but changing it until it is changed
        mustChangePassword: claims.pwd_change === true
      }
      this.isAuthenticated = true

      localStorage.setItem('auth_token', this.token)
      localStorage.setItem('refresh_token', this.refreshToken)
      localStorage.setItem('auth_user', JSON.stringify(this.user))
    },

    // Drop the session locally without calling the server (used on logout and when the session can't be renewed).
    // Also wipes the user's cached data; returns a promise that resolves once the caches are cleared.
    clearSession() {
      this.token = null
      this.refreshToken = null
      this.user = null
      this.isAuthenticated = false
      localStorage.removeItem('auth_token')
      localStorage.removeItem('refresh_token')
      localStorage.removeItem('auth_user')
      return clearUserCaches()
    },

    isTokenExpired() {
      const exp = decodeToken(this.token)?.exp
      if (!exp) return true
      return Date.now() / 1000 >= exp - EXPIRY_SKEW_SECONDS
    },

    async login(username, password) {
      try {
        // Import api dynamically to avoid circular dependency
        const { api } = await import('boot/axios')
        const response = await api.post('/api/auth/login', {
          username,
          password
        })

        const data = response.data?.data
        if (data?.token) {
          this.applySession(data)
          return { success: true }
        }
        return { success: false, error: response.data?.message || 'Login failed' }
      } catch (error) {
        console.error('Login error:', error)
        return {
          success: false,
          error: error.response?.data?.error || 'Login failed'
        }
      }
    },

    async logout() {
      const token = this.token
      const refreshToken = localStorage.getItem('refresh_token') || this.refreshToken
      try {
        const { api } = await import('boot/axios')
        if (token) {
          // Revokes this session's refresh token on the server
          await api.post(
            '/api/auth/logout',
            refreshToken ? { refreshToken } : null,
            { headers: { Authorization: `Bearer ${token}` } }
          )
        }
      } catch (error) {
        console.error('Logout error:', error)
      } finally {
        await this.clearSession()
      }
    },

    // Change the current user's password. The server ends other sessions and returns a fresh token pair.
    async changePassword(currentPassword, newPassword) {
      try {
        const { api } = await import('boot/axios')
        const response = await api.post('/api/auth/change-password', { currentPassword, newPassword })
        const data = response.data?.data
        if (data?.token) {
          this.applySession(data)
          return { success: true }
        }
        return { success: false, error: response.data?.message || 'Failed to change password' }
      } catch (error) {
        return {
          success: false,
          error: error.response?.data?.message || error.response?.data?.error || 'Failed to change password'
        }
      }
    },

    // Exchange the refresh token for a new token pair. Resolves true when the session was extended.
    refreshAccessToken() {
      if (refreshPromise) return refreshPromise

      // Refresh tokens rotate on every use; another tab may already have replaced ours, so prefer the shared copy
      const refreshToken = localStorage.getItem('refresh_token') || this.refreshToken
      if (!refreshToken) return Promise.resolve(false)

      refreshPromise = (async () => {
        try {
          const { api } = await import('boot/axios')
          const response = await api.post('/api/auth/refresh', { refreshToken })
          const data = response.data?.data
          if (data?.token) {
            this.applySession(data)
            return true
          }
          return false
        } catch (error) {
          console.error('Token refresh failed:', error)
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
      // Offline the refresh can't reach the server; keep the session for the offline mode
      if (!navigator.onLine) return true
      return this.refreshAccessToken()
    },

    async fetchUserInfo() {
      try {
        const { api } = await import('boot/axios')
        const response = await api.get('/api/auth/me')
        this.user = response.data
        this.isAuthenticated = true
        return response.data
      } catch (error) {
        console.error('Fetch user info error:', error)
        throw error
      }
    },

    async initializeAuth() {
      // Renew an expired token restored from localStorage right away; drop the session if that fails
      if (this.token && !(await this.ensureValidToken())) {
        console.warn('Stored session expired and could not be refreshed')
        this.clearSession()
      }
    }
  }
})
