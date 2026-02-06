import { defineStore } from 'pinia'
import { api } from 'boot/axios'
import { jwtDecode } from 'jwt-decode'

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
    async login(username, password) {
      try {
        const response = await api.post('/api/auth/login', {
          username,
          password
        })

        const tokenObject = response.data

        if (tokenObject.data.token) {
          this.token = tokenObject.data.token
          this.refreshToken = tokenObject.data.refreshToken
          this.user = {
            username: tokenObject.data.username,
            email: tokenObject.data.email,
            roles: jwtDecode(tokenObject.data.token).groups,
            karyawanId: jwtDecode(tokenObject.data.token).karyawanId,
            karyawanNama: jwtDecode(tokenObject.data.token).karyawanNama
          }
          this.isAuthenticated = true

          // Store tokens and user in localStorage
          localStorage.setItem('auth_token', this.token)
          localStorage.setItem('refresh_token', this.refreshToken)
          localStorage.setItem('auth_user', JSON.stringify(this.user))

          // Set default authorization header for all requests
          api.defaults.headers.common['Authorization'] = `Bearer ${this.token}`

          return { success: true }
        }
      } catch (error) {
        console.error('Login error:', error)
        return {
          success: false,
          error: error.response?.data?.error || 'Login failed'
        }
      }
    },

    async logout() {
      try {
        await api.post('/api/auth/logout')
      } catch (error) {
        console.error('Logout error:', error)
      } finally {
        this.token = null
        this.refreshToken = null
        this.user = null
        this.isAuthenticated = false
        localStorage.removeItem('auth_token')
        localStorage.removeItem('refresh_token')
        localStorage.removeItem('auth_user')
        delete api.defaults.headers.common['Authorization']
      }
    },
    
    async refreshAccessToken() {
      const refreshToken = this.refreshToken || localStorage.getItem('refresh_token')
      if (!refreshToken) {
        return false
      }
      
      try {
        const response = await api.post('/api/auth/refresh', { refreshToken })
        const tokenObject = response.data
        
        if (tokenObject.data.token) {
          this.token = tokenObject.data.token
          this.refreshToken = tokenObject.data.refreshToken
          
          // Update localStorage
          localStorage.setItem('auth_token', this.token)
          localStorage.setItem('refresh_token', this.refreshToken)
          
          // Update authorization header
          api.defaults.headers.common['Authorization'] = `Bearer ${this.token}`
          
          return true
        }
      } catch (error) {
        console.error('Token refresh failed:', error)
        return false
      }
      return false
    },

    async fetchUserInfo() {
      try {
        const response = await api.get('/api/auth/me')
        this.user = response.data
        this.isAuthenticated = true
        return response.data
      } catch (error) {
        console.error('Fetch user info error:', error)
        // If token is invalid, logout
        if (error.response?.status === 401) {
          this.logout()
        }
        throw error
      }
    },

    initializeAuth() {
      // Restore token from localStorage on app initialization
      if (this.token) {
        api.defaults.headers.common['Authorization'] = `Bearer ${this.token}`

        // If we don't have user info, fetch it to verify token is still valid
        if (!this.user) {
          this.fetchUserInfo().catch(() => {
            // Token is invalid, clear it
            console.warn('Token is invalid or expired, logging out')
            this.logout()
          })
        }
      }
    }
  }
})

