import masterDataCache from './masterDataCache.js'
import browserCache from './browserCache.js'
import syncService from '../services/syncService.js'

// Service worker Cache Storage buckets holding API responses (app shell assets are kept for offline start-up)
const API_CACHE_PREFIX = 'api-'

// Remove all cached server data from the browser so the next user doesn't see the previous user's data.
// Pending offline requests are kept so unsynced work isn't lost.
export async function clearUserCaches() {
  const tasks = [
    masterDataCache.clearAll(),
    Promise.resolve().then(() => browserCache.clear()),
    syncService.storage.clearCachedData(),
    Promise.resolve().then(() => localStorage.removeItem('lastSyncTime'))
  ]

  if (typeof caches !== 'undefined') {
    tasks.push(
      caches.keys().then(names =>
        Promise.all(names.filter(name => name.startsWith(API_CACHE_PREFIX)).map(name => caches.delete(name))))
    )
  }

  const results = await Promise.allSettled(tasks)
  results
    .filter(r => r.status === 'rejected')
    .forEach(r => console.warn('[clearUserCaches] failed to clear a cache:', r.reason))
}
