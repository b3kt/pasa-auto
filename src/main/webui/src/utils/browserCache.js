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
      
      // Handle quota exceeded error
      if (error.name === 'QuotaExceededError' || error.code === 22) {
        console.log('Storage quota exceeded, clearing cache...')
        
        // First try to clear expired items
        const clearedCount = this.clearExpired()
        
        // If still not enough space, clear oldest items
        if (clearedCount === 0) {
          this.clearOldestItems(10) // Clear 10 oldest items
        }
        
        // Try again
        try {
          localStorage.setItem(this.cachePrefix + key, JSON.stringify(item))
          console.log('Successfully set cache item after cleanup')
          return true
        } catch (retryError) {
          console.error('Still failed to set cache item after cleanup:', retryError)
          // If still failing, clear more aggressively
          this.clearOldestItems(50) // Clear 50 oldest items
          try {
            localStorage.setItem(this.cachePrefix + key, JSON.stringify(item))
            return true
          } catch (finalError) {
            console.error('Final attempt failed, clearing all cache:', finalError)
            this.clear() // Clear everything as last resort
            return false
          }
        }
      }
      
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

  // Clear oldest items from localStorage
  clearOldestItems(count = 10) {
    try {
      const keys = Object.keys(localStorage)
      const cacheItems = []

      keys.forEach(key => {
        if (key.startsWith(this.cachePrefix)) {
          try {
            const item = JSON.parse(localStorage.getItem(key))
            if (item && item.timestamp) {
              cacheItems.push({
                key: key,
                timestamp: item.timestamp
              })
            }
          } catch (error) {
            // Remove malformed items
            localStorage.removeItem(key)
          }
        }
      })

      // Sort by timestamp (oldest first)
      cacheItems.sort((a, b) => a.timestamp - b.timestamp)

      // Remove oldest items
      let clearedCount = 0
      const itemsToRemove = Math.min(count, cacheItems.length)
      
      for (let i = 0; i < itemsToRemove; i++) {
        localStorage.removeItem(cacheItems[i].key)
        clearedCount++
      }

      console.log(`Cleared ${clearedCount} oldest cache items`)
      return clearedCount
    } catch (error) {
      console.error('Failed to clear oldest items:', error)
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

  // Check storage usage and manage cache proactively
  checkStorageUsage() {
    try {
      // Estimate localStorage usage (rough approximation)
      let totalSize = 0
      const keys = Object.keys(localStorage)
      
      keys.forEach(key => {
        const value = localStorage.getItem(key)
        if (value) {
          totalSize += key.length + value.length
        }
      })

      // Convert to KB (each character is roughly 1 byte)
      const sizeInKB = totalSize / 1024
      
      // localStorage typically has 5-10MB limit, warn at 4MB
      if (sizeInKB > 4096) {
        console.log(`Storage usage is high: ${sizeInKB.toFixed(2)} KB, cleaning up...`)
        this.clearExpired()
        
        // If still high, clear oldest items
        if (sizeInKB > 5120) { // 5MB
          this.clearOldestItems(20)
        }
      }

      return {
        sizeInKB: sizeInKB.toFixed(2),
        totalItems: keys.length,
        usagePercent: ((sizeInKB / 5120) * 100).toFixed(1) // Assuming 5MB limit
      }
    } catch (error) {
      console.error('Failed to check storage usage:', error)
      return null
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

// Auto-cleanup expired items and check storage usage every 30 minutes
setInterval(() => {
  browserCache.clearExpired()
  browserCache.checkStorageUsage()
}, 30 * 60 * 1000)

export default browserCache
