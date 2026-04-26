import { defineBoot } from '#q-app/wrappers'
import { Notify } from 'quasar'
import syncService from '../services/syncService.js'

export default defineBoot(async ({ app }) => {
  // Initialize sync service without using composables
  try {
    await syncService.init()
    console.log('Sync service initialized successfully')
  } catch (error) {
    console.error('Failed to initialize sync service:', error)
  }

  // Auto-sync when coming back online
  window.addEventListener('online', async () => {
    if (navigator.onLine) {
      try {
        await syncService.syncPendingRequests()
        syncService.updateLastSyncTime()
        Notify.create({
          type: 'positive',
          message: 'Data synced successfully',
          position: 'top-right',
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
