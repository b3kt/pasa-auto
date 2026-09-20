import { defineBoot } from '#q-app/wrappers'
import { Notify } from 'quasar'
import syncService from '../services/syncService.js'
import masterDataCache from '../utils/masterDataCache.js'

// Both IndexedDB stores are written on every request and were only ever cleared wholesale, at
// logout. Sweeping them keeps the origin inside its storage quota.
const MAINTENANCE_INTERVAL = 6 * 60 * 60 * 1000 // 6 hours

const runCacheMaintenance = async () => {
  const results = await Promise.allSettled([
    masterDataCache.clearExpired(),
    syncService.storage.pruneCachedData()
  ])
  results
    .filter(r => r.status === 'rejected')
    .forEach(r => console.warn('[offline] cache maintenance failed:', r.reason))
}

export default defineBoot(async ({ app }) => {
  // Initialize sync service without using composables
  try {
    await syncService.init()
    console.log('Sync service initialized successfully')
  } catch (error) {
    console.error('Failed to initialize sync service:', error)
  }

  // Once at start-up, then on a slow cadence for sessions that stay open for days
  runCacheMaintenance()
  setInterval(runCacheMaintenance, MAINTENANCE_INTERVAL)

  // Auto-sync when coming back online
  window.addEventListener('online', async () => {
    if (navigator.onLine) {
      try {
        await syncService.syncPendingRequests()
        syncService.updateLastSyncTime()
        Notify.create({
          type: 'positive',
          message: 'Data synced successfully',
          timeout: 3000
        })
      } catch (error) {
        console.error('Auto-sync failed:', error)
      }
    }
  })

  // Register service worker for PWA functionality
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.ready.then(registration => {
      console.log('Service worker registered:', registration.scope)

      // Check for updates periodically
      setInterval(() => {
        registration.update()
      }, 60 * 60 * 1000) // Check every hour
    }).catch(error => {
      console.error('Service worker registration failed:', error)
    })
  }

  // Add global properties for offline functionality
  app.config.globalProperties.$offline = {
    syncService,
    isOnline: () => navigator.onLine
  }
})
