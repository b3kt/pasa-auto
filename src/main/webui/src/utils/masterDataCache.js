const DB_NAME = 'pasa-auto-master-cache'
const DB_VERSION = 1
const STORE_NAME = 'entries'
const DEFAULT_TTL = 24 * 60 * 60 * 1000 // 24 hours

class MasterDataCache {
  constructor() {
    this._db = null
  }

  async _getDb() {
    if (this._db) return this._db
    return new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, DB_VERSION)
      req.onupgradeneeded = (e) => {
        const db = e.target.result
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          const store = db.createObjectStore(STORE_NAME, { keyPath: 'key' })
          store.createIndex('prefix', 'prefix', { unique: false })
          store.createIndex('expiresAt', 'expiresAt', { unique: false })
        }
      }
      req.onsuccess = (e) => {
        this._db = e.target.result
        resolve(this._db)
      }
      req.onerror = (e) => reject(e.target.error)
    })
  }

  async get(key) {
    try {
      const db = await this._getDb()
      return new Promise((resolve, reject) => {
        const tx = db.transaction(STORE_NAME, 'readonly')
        const req = tx.objectStore(STORE_NAME).get(key)
        req.onsuccess = (e) => {
          const entry = e.target.result
          if (!entry) return resolve(null)
          if (Date.now() > entry.expiresAt) {
            this.delete(key)
            return resolve(null)
          }
          resolve(entry.data)
        }
        req.onerror = (e) => reject(e.target.error)
      })
    } catch (e) {
      console.warn('[masterDataCache] get error:', e)
      return null
    }
  }

  async set(key, data, prefix = '', ttl = DEFAULT_TTL) {
    try {
      const db = await this._getDb()
      return new Promise((resolve, reject) => {
        const tx = db.transaction(STORE_NAME, 'readwrite')
        const req = tx.objectStore(STORE_NAME).put({
          key,
          prefix,
          data,
          cachedAt: Date.now(),
          expiresAt: Date.now() + ttl
        })
        req.onsuccess = () => resolve(true)
        req.onerror = (e) => reject(e.target.error)
      })
    } catch (e) {
      console.warn('[masterDataCache] set error:', e)
      return false
    }
  }

  async delete(key) {
    try {
      const db = await this._getDb()
      return new Promise((resolve, reject) => {
        const tx = db.transaction(STORE_NAME, 'readwrite')
        const req = tx.objectStore(STORE_NAME).delete(key)
        req.onsuccess = () => resolve(true)
        req.onerror = (e) => reject(e.target.error)
      })
    } catch (e) {
      console.warn('[masterDataCache] delete error:', e)
      return false
    }
  }

  // Remove all cache entries for a given baseApiUrl prefix
  async invalidatePrefix(prefix) {
    try {
      const db = await this._getDb()
      return new Promise((resolve, reject) => {
        const tx = db.transaction(STORE_NAME, 'readwrite')
        const idx = tx.objectStore(STORE_NAME).index('prefix')
        const req = idx.openCursor(IDBKeyRange.only(prefix))
        let count = 0
        req.onsuccess = (e) => {
          const cursor = e.target.result
          if (cursor) {
            cursor.delete()
            count++
            cursor.continue()
          } else {
            resolve(count)
          }
        }
        req.onerror = (e) => reject(e.target.error)
      })
    } catch (e) {
      console.warn('[masterDataCache] invalidatePrefix error:', e)
      return 0
    }
  }

  async clearAll() {
    try {
      const db = await this._getDb()
      return new Promise((resolve, reject) => {
        const tx = db.transaction(STORE_NAME, 'readwrite')
        const req = tx.objectStore(STORE_NAME).clear()
        req.onsuccess = () => resolve(true)
        req.onerror = (e) => reject(e.target.error)
      })
    } catch (e) {
      console.warn('[masterDataCache] clearAll error:', e)
      return false
    }
  }

  async getStats() {
    try {
      const db = await this._getDb()
      return new Promise((resolve, reject) => {
        const tx = db.transaction(STORE_NAME, 'readonly')
        const req = tx.objectStore(STORE_NAME).getAll()
        req.onsuccess = (e) => {
          const all = e.target.result
          const now = Date.now()
          const byPrefix = {}
          all.forEach((entry) => {
            if (!byPrefix[entry.prefix]) byPrefix[entry.prefix] = { total: 0, active: 0 }
            byPrefix[entry.prefix].total++
            if (entry.expiresAt > now) byPrefix[entry.prefix].active++
          })
          resolve({
            total: all.length,
            active: all.filter((e) => e.expiresAt > now).length,
            expired: all.filter((e) => e.expiresAt <= now).length,
            byPrefix
          })
        }
        req.onerror = (e) => reject(e.target.error)
      })
    } catch (e) {
      console.warn('[masterDataCache] getStats error:', e)
      return { total: 0, active: 0, expired: 0, byPrefix: {} }
    }
  }
}

export default new MasterDataCache()
