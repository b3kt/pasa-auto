import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'

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

// Add request interceptor to include auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
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

// Add response interceptor to handle 401 errors with token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    
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
