import { ref, onMounted, onUnmounted } from 'vue'
import { Notify } from 'quasar'

class OfflineStorage {
  constructor() {
    this.db = null
    this.dbName = 'pasaAutoOfflineDB'
    this.dbVersion = 1
    this.isOnline = navigator.onLine
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

export function useOfflineStorage() {
  const storage = new OfflineStorage()
  const isOnline = ref(navigator.onLine)
  const isInitialized = ref(false)

  const init = async () => {
    try {
      await storage.initDB()
      isInitialized.value = true
      console.log('Offline storage initialized')
    } catch (error) {
      console.error('Failed to initialize offline storage:', error)
      Notify.create({
        type: 'negative',
        message: 'Failed to initialize offline storage',
        position: 'top-right'
      })
    }
  }

  const handleOnline = () => {
    isOnline.value = true
    Notify.create({
      type: 'positive',
      message: 'Connection restored. Syncing data...',
      position: 'top-right'
    })
    // Trigger sync when coming back online
    if ('serviceWorker' in navigator && 'sync' in window.ServiceWorkerRegistration.prototype) {
      navigator.serviceWorker.ready.then(registration => {
        return registration.sync.register('sync-data')
      })
    }
  }

  const handleOffline = () => {
    isOnline.value = false
    Notify.create({
      type: 'warning',
      message: 'Connection lost. Working in offline mode.',
      position: 'top-right',
      timeout: 3000
    })
  }

  onMounted(() => {
    init()
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
  })

  onUnmounted(() => {
    window.removeEventListener('online', handleOnline)
    window.removeEventListener('offline', handleOffline)
  })

  return {
    storage,
    isOnline,
    isInitialized,
    init
  }
}
