class BrowserCache {
  constructor() {
    this.cachePrefix = 'pasa-auto-'
    this.defaultTTL = 24 * 60 * 60 * 1000 // 24 hours in milliseconds
  }

  // Set item in localStorage with expiration
  setItem(key, data, ttl = this.defaultTTL) {
    const item = {
      data: data,
      timestamp: Date.now(),
      ttl: ttl
    }

    try {
      localStorage.setItem(this.cachePrefix + key, JSON.stringify(item))
      return true
    } catch (error) {
      console.error('Failed to set cache item:', error)
      // If localStorage is full, clear old items
      this.clearExpired()
      return false
    }
  }

  // Get item from localStorage, checking expiration
  getItem(key) {
    try {
      const item = localStorage.getItem(this.cachePrefix + key)
      if (!item) return null

      const parsed = JSON.parse(item)
      const now = Date.now()

      // Check if item is expired
      if (now - parsed.timestamp > parsed.ttl) {
        this.removeItem(key)
        return null
      }

      return parsed.data
    } catch (error) {
      console.error('Failed to get cache item:', error)
      return null
    }
  }

  // Remove item from localStorage
  removeItem(key) {
    try {
      localStorage.removeItem(this.cachePrefix + key)
      return true
    } catch (error) {
      console.error('Failed to remove cache item:', error)
      return false
    }
  }

  // Clear expired items from localStorage
  clearExpired() {
    try {
      const keys = Object.keys(localStorage)
      const now = Date.now()
      let clearedCount = 0

      keys.forEach(key => {
        if (key.startsWith(this.cachePrefix)) {
          try {
            const item = JSON.parse(localStorage.getItem(key))
            if (item && (now - item.timestamp > item.ttl)) {
              localStorage.removeItem(key)
              clearedCount++
            }
          } catch (error) {
            console.log('Failed to parse cache item:', error)
            // Remove malformed items
            localStorage.removeItem(key)
            clearedCount++
          }
        }
      })

      console.log(`Cleared ${clearedCount} expired cache items`)
      return clearedCount
    } catch (error) {
      console.error('Failed to clear expired items:', error)
      return 0
    }
  }

  // Clear all cache items
  clear() {
    try {
      const keys = Object.keys(localStorage)
      let clearedCount = 0

      keys.forEach(key => {
        if (key.startsWith(this.cachePrefix)) {
          localStorage.removeItem(key)
          clearedCount++
        }
      })

      console.log(`Cleared ${clearedCount} cache items`)
      return clearedCount
    } catch (error) {
      console.error('Failed to clear cache:', error)
      return 0
    }
  }

  // Get cache statistics
  getStats() {
    try {
      const keys = Object.keys(localStorage)
      let totalItems = 0
      let totalSize = 0
      let expiredItems = 0
      const now = Date.now()

      keys.forEach(key => {
        if (key.startsWith(this.cachePrefix)) {
          totalItems++
          const item = localStorage.getItem(key)
          totalSize += item.length

          try {
            const parsed = JSON.parse(item)
            if (now - parsed.timestamp > parsed.ttl) {
              expiredItems++
            }
          } catch (error) {
            console.log('Failed to parse cache item:', error)
            expiredItems++
          }
        }
      })

      return {
        totalItems,
        totalSize: (totalSize / 1024).toFixed(2) + ' KB',
        expiredItems,
        activeItems: totalItems - expiredItems
      }
    } catch (error) {
      console.error('Failed to get cache stats:', error)
      return null
    }
  }

  // Cache JavaScript files specifically
  cacheJsFile(url, content, ttl = 7 * 24 * 60 * 60 * 1000) { // 7 days
    return this.setItem('js-' + this.generateApiKey(url), {
      url: url,
      content: content,
      type: 'javascript',
      timestamp: Date.now()
    }, ttl)
  }

  // Get cached JavaScript file
  getCachedJsFile(url) {
    const cached = this.getItem('js-' + this.generateApiKey(url))
    if (cached && cached.type === 'javascript') {
      return cached.content
    }
    return null
  }

  // Cache CSS files specifically
  cacheCssFile(url, content, ttl = 7 * 24 * 60 * 60 * 1000) { // 7 days
    return this.setItem('css-' + this.generateApiKey(url), {
      url: url,
      content: content,
      type: 'css',
      timestamp: Date.now()
    }, ttl)
  }

  // Get cached CSS file
  getCachedCssFile(url) {
    const cached = this.getItem('css-' + this.generateApiKey(url))
    if (cached && cached.type === 'css') {
      return cached.content
    }
    return null
  }

  // Cache API response
  async cacheApiResponse(url, response, ttl = this.defaultTTL) {
    const cacheKey = this.generateApiKey(url)
    return this.setItem(cacheKey, {
      url: url,
      data: response,
      status: response.status,
      headers: { ...response.headers }
    }, ttl)
  }

  // Get cached API response
  getCachedApiResponse(url) {
    const cacheKey = this.generateApiKey(url)
    const cached = this.getItem(cacheKey)

    if (cached) {
      // Create a Response object from cached data
      return new Response(JSON.stringify(cached.data), {
        status: cached.status,
        headers: cached.headers
      })
    }

    return null
  }

  // Generate cache key for API calls
  generateApiKey(url) {
    return 'api-' + btoa(url).replace(/[+/=]/g, '')
  }

  // Cache component data
  cacheComponentData(componentName, data, ttl = this.defaultTTL) {
    return this.setItem('component-' + componentName, data, ttl)
  }

  // Get cached component data
  getCachedComponentData(componentName) {
    return this.getItem('component-' + componentName)
  }

  // Cache user preferences
  cacheUserPreferences(preferences) {
    return this.setItem('user-preferences', preferences, 30 * 24 * 60 * 60 * 1000) // 30 days
  }

  // Get cached user preferences
  getCachedUserPreferences() {
    return this.getItem('user-preferences')
  }

  // Cache application state
  cacheApplicationState(state) {
    return this.setItem('app-state', state, 7 * 24 * 60 * 60 * 1000) // 7 days
  }

  // Get cached application state
  getCachedApplicationState() {
    return this.getItem('app-state')
  }
}

// Create singleton instance
const browserCache = new BrowserCache()

// Auto-cleanup expired items every hour
setInterval(() => {
  browserCache.clearExpired()
}, 60 * 60 * 1000)

export default browserCache
