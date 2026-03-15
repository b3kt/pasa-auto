import { Notify } from 'quasar'

class OfflineStorage {
  constructor() {
    this.db = null
    this.dbName = 'pasaAutoOfflineDB'
    this.dbVersion = 1
  }

  async initDB() {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.dbName, this.dbVersion)

      request.onerror = () => reject(request.error)
      request.onsuccess = () => {
        this.db = request.result
        resolve(this.db)
      }

      request.onupgradeneeded = (event) => {
        const db = event.target.result

        // Store for offline data
        if (!db.objectStoreNames.contains('offlineData')) {
          const offlineStore = db.createObjectStore('offlineData', { keyPath: 'id' })
          offlineStore.createIndex('endpoint', 'endpoint')
          offlineStore.createIndex('timestamp', 'timestamp')
        }

        // Store for pending requests
        if (!db.objectStoreNames.contains('pendingRequests')) {
          const pendingStore = db.createObjectStore('pendingRequests', { 
            keyPath: 'id', 
            autoIncrement: true 
          })
          pendingStore.createIndex('method', 'method')
          pendingStore.createIndex('timestamp', 'timestamp')
        }

        // Store for sync status
        if (!db.objectStoreNames.contains('syncStatus')) {
          const syncStore = db.createObjectStore('syncStatus', { keyPath: 'id' })
          syncStore.createIndex('status', 'status')
        }
      }
    })
  }

  async storeData(key, data, endpoint = null) {
    if (!this.db) await this.initDB()
    
    const transaction = this.db.transaction(['offlineData'], 'readwrite')
    const store = transaction.objectStore('offlineData')
    
    const record = {
      id: key,
      data: data,
      endpoint: endpoint,
      timestamp: Date.now(),
      synced: false
    }
    
    return new Promise((resolve, reject) => {
      const request = store.put(record)
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
  }

  async getData(key) {
    if (!this.db) await this.initDB()
    
    const transaction = this.db.transaction(['offlineData'], 'readonly')
    const store = transaction.objectStore('offlineData')
    
    return new Promise((resolve, reject) => {
      const request = store.get(key)
      request.onsuccess = () => resolve(request.result?.data || null)
      request.onerror = () => reject(request.error)
    })
  }

  async getAllData() {
    if (!this.db) await this.initDB()
    
    const transaction = this.db.transaction(['offlineData'], 'readonly')
    const store = transaction.objectStore('offlineData')
    
    return new Promise((resolve, reject) => {
      const request = store.getAll()
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
  }

  async storePendingRequest(requestData) {
    if (!this.db) await this.initDB()
    
    const transaction = this.db.transaction(['pendingRequests'], 'readwrite')
    const store = transaction.objectStore('pendingRequests')
    
    const record = {
      ...requestData,
      timestamp: Date.now(),
      retryCount: 0
    }
    
    return new Promise((resolve, reject) => {
      const request = store.add(record)
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
  }

  async getPendingRequests() {
    if (!this.db) await this.initDB()
    
    const transaction = this.db.transaction(['pendingRequests'], 'readonly')
    const store = transaction.objectStore('pendingRequests')
    
    return new Promise((resolve, reject) => {
      const request = store.getAll()
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
  }

  async removePendingRequest(id) {
    if (!this.db) await this.initDB()
    
    const transaction = this.db.transaction(['pendingRequests'], 'readwrite')
    const store = transaction.objectStore('pendingRequests')
    
    return new Promise((resolve, reject) => {
      const request = store.delete(id)
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
  }

  async clearAllData() {
    if (!this.db) await this.initDB()
    
    const stores = ['offlineData', 'pendingRequests', 'syncStatus']
    const promises = stores.map(storeName => {
      return new Promise((resolve, reject) => {
        const transaction = this.db.transaction([storeName], 'readwrite')
        const store = transaction.objectStore(storeName)
        const request = store.clear()
        request.onsuccess = () => resolve()
        request.onerror = () => reject(request.error)
      })
    })
    
    return Promise.all(promises)
  }

  async getStorageSize() {
    if ('storage' in navigator && 'estimate' in navigator.storage) {
      const estimate = await navigator.storage.estimate()
      return {
        quota: estimate.quota,
        usage: estimate.usage,
        usageDetails: estimate.usageDetails
      }
    }
    return null
  }
}

class SyncService {
  constructor() {
    this.isSyncing = false
    this.syncQueue = []
    this.retryAttempts = 3
    this.retryDelay = 1000 // 1 second
    this.storage = new OfflineStorage()
  }

  async init() {
    await this.storage.initDB()
  }

  async syncPendingRequests() {
    if (this.isSyncing || !navigator.onLine) {
      return false
    }

    this.isSyncing = true

    try {
      const pendingRequests = await this.storage.getPendingRequests()

      if (pendingRequests.length === 0) {
        this.isSyncing = false
        return true
      }

      Notify.create({
        type: 'info',
        message: `Syncing ${pendingRequests.length} pending requests...`,
        position: 'top-right'
      })

      let successCount = 0
      let failureCount = 0

      for (const request of pendingRequests) {
        try {
          const success = await this.syncRequest(request)
          if (success) {
            await this.storage.removePendingRequest(request.id)
            successCount++
          } else {
            failureCount++
            // Update retry count
            request.retryCount = (request.retryCount || 0) + 1
            if (request.retryCount >= this.retryAttempts) {
              await this.storage.removePendingRequest(request.id)
              console.error('Max retry attempts reached for request:', request.url)
            }
          }
        } catch (error) {
          console.error('Error syncing request:', error)
          failureCount++
        }
      }

      if (successCount > 0) {
        Notify.create({
          type: 'positive',
          message: `Successfully synced ${successCount} requests`,
          position: 'top-right'
        })
      }

      if (failureCount > 0) {
        Notify.create({
          type: 'warning',
          message: `${failureCount} requests failed to sync`,
          position: 'top-right'
        })
      }

      return successCount > 0
    } catch (error) {
      console.error('Sync failed:', error)
      Notify.create({
        type: 'negative',
        message: 'Sync failed. Please try again.',
        position: 'top-right'
      })
      return false
    } finally {
      this.isSyncing = false
    }
  }

  async syncRequest(request) {
    const { url, method, headers, body } = request

    try {
      const response = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          ...headers
        },
        body: method !== 'GET' && method !== 'DELETE' ? body : undefined
      })

      if (response.ok || response.status === 202) {
        // Update local cache with server response if needed
        if (method === 'POST' || method === 'PUT') {
          try {
            const responseData = await response.json()
            const cacheKey = this.generateCacheKey(url, method)
            await this.storage.storeData(cacheKey, responseData, url)
          } catch (e) {
            // Ignore JSON parsing errors for non-JSON responses
            console.error('Failed to parse response:', e)
          }
        }
        return true
      } else {
        console.warn(`Request failed with status ${response.status}:`, url)
        return false
      }
    } catch (error) {
      console.error('Network error during sync:', error)
      return false
    }
  }

  async syncDataFromServer() {
    if (!navigator.onLine) {
      return false
    }

    try {
      // Get all cached data that needs syncing
      const allData = await this.storage.getAllData()
      const unsyncedData = allData.filter(item => !item.synced && item.endpoint)

      for (const item of unsyncedData) {
        try {
          const response = await fetch(item.endpoint)
          if (response.ok) {
            const freshData = await response.json()
            await this.storage.storeData(item.id, freshData, item.endpoint)

            // Mark as synced
            const transaction = this.storage.db.transaction(['offlineData'], 'readwrite')
            const store = transaction.objectStore('offlineData')
            const record = await store.get(item.id)
            if (record) {
              record.synced = true
              record.timestamp = Date.now()
              await store.put(record)
            }
          }
        } catch (error) {
          console.error(`Failed to sync data for ${item.endpoint}:`, error)
        }
      }

      return true
    } catch (error) {
      console.error('Data sync failed:', error)
      return false
    }
  }

  generateCacheKey(url, method) {
    const urlObj = new URL(url)
    const key = `${method}:${urlObj.pathname}${urlObj.search}`
    return btoa(key).replace(/[+/=]/g, '')
  }

  async forceSyncAll() {
    if (!navigator.onLine) {
      Notify.create({
        type: 'warning',
        message: 'No internet connection available',
        position: 'top-right'
      })
      return false
    }

    Notify.create({
      type: 'info',
      message: 'Starting full sync...',
      position: 'top-right'
    })

    const requestsSynced = await this.syncPendingRequests()
    const dataSynced = await this.syncDataFromServer()

    if (requestsSynced || dataSynced) {
      Notify.create({
        type: 'positive',
        message: 'Full sync completed successfully',
        position: 'top-right'
      })
      return true
    } else {
      Notify.create({
        type: 'warning',
        message: 'No data to sync or sync failed',
        position: 'top-right'
      })
      return false
    }
  }

  async getSyncStatus() {
    try {
      const pendingRequests = await this.storage.getPendingRequests()
      const allData = await this.storage.getAllData()
      const unsyncedData = allData.filter(item => !item.synced && item.endpoint)

      return {
        pendingRequests: pendingRequests.length,
        unsyncedData: unsyncedData.length,
        isOnline: navigator.onLine,
        isSyncing: this.isSyncing,
        lastSyncTime: localStorage.getItem('lastSyncTime') || null
      }
    } catch (error) {
      console.error('Failed to get sync status:', error)
      return {
        pendingRequests: 0,
        unsyncedData: 0,
        isOnline: navigator.onLine,
        isSyncing: this.isSyncing,
        lastSyncTime: null
      }
    }
  }

  updateLastSyncTime() {
    localStorage.setItem('lastSyncTime', new Date().toISOString())
  }

  async clearAllOfflineData() {
    try {
      await this.storage.clearAllData()
      localStorage.removeItem('lastSyncTime')

      Notify.create({
        type: 'positive',
        message: 'All offline data cleared',
        position: 'top-right'
      })

      return true
    } catch (error) {
      console.error('Failed to clear offline data:', error)
      Notify.create({
        type: 'negative',
        message: 'Failed to clear offline data',
        position: 'top-right'
      })
      return false
    }
  }
}

// Create singleton instance
const syncService = new SyncService()

export default syncService

// Auto-initialize when imported
syncService.init().catch(console.error)
