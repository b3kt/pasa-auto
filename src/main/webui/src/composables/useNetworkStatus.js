import { ref, onMounted, onUnmounted } from 'vue'
import { Notify } from 'quasar'
import syncService from '../services/syncService.js'

export function useNetworkStatus() {
  const isOnline = ref(navigator.onLine)
  const connectionType = ref('unknown')
  const connectionSpeed = ref('unknown')
  const lastOnlineTime = ref(navigator.onLine ? Date.now() : null)
  const offlineDuration = ref(0)

  let networkCheckInterval = null
  let syncRetryTimeout = null

  const getConnectionInfo = () => {
    if ('connection' in navigator) {
      const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection

      if (connection) {
        connectionType.value = connection.type || 'unknown'
        connectionSpeed.value = connection.effectiveType || 'unknown'

        // Listen for connection changes
        connection.addEventListener('change', updateConnectionInfo)
      }
    }
  }

  const updateConnectionInfo = () => {
    if ('connection' in navigator) {
      const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
      if (connection) {
        connectionType.value = connection.type || 'unknown'
        connectionSpeed.value = connection.effectiveType || 'unknown'
      }
    }
  }

  const handleOnline = () => {
    const wasOffline = !isOnline.value
    isOnline.value = true

    if (wasOffline) {
      const currentTime = Date.now()
      if (lastOnlineTime.value) {
        offlineDuration.value = currentTime - lastOnlineTime.value
      }
      lastOnlineTime.value = currentTime

      Notify.create({
        type: 'positive',
        message: 'Connection restored! Syncing data...',
        position: 'top-right',
        timeout: 3000
      })

      // Trigger sync when coming back online
      triggerSync()
    }
  }

  const handleOffline = () => {
    isOnline.value = false

    Notify.create({
      type: 'warning',
      message: 'Connection lost. Working in offline mode.',
      position: 'top-right',
      timeout: 5000,
      actions: [
        {
          label: 'View Status',
          color: 'white',
          handler: () => {
            // Could open a dialog showing offline status
          }
        }
      ]
    })

    // Clear any pending sync retries
    if (syncRetryTimeout) {
      clearTimeout(syncRetryTimeout)
      syncRetryTimeout = null
    }
  }

  const triggerSync = async () => {
    try {
      // Wait a bit for connection to stabilize
      await new Promise(resolve => setTimeout(resolve, 1000))

      const success = await syncService.forceSyncAll()
      if (success) {
        syncService.updateLastSyncTime()
      }
    } catch (error) {
      console.error('Auto-sync failed:', error)

      // Retry after delay
      syncRetryTimeout = setTimeout(() => {
        if (isOnline.value) {
          triggerSync()
        }
      }, 30000) // 30 seconds retry
    }
  }

  const startNetworkMonitoring = () => {
    // Clear existing interval
    if (networkCheckInterval) {
      clearInterval(networkCheckInterval)
    }

    // Check network status every 30 seconds
    networkCheckInterval = setInterval(async () => {
      try {
        // Try to fetch a small resource to verify actual connectivity
        const response = await fetch('/favicon.ico', {
          method: 'HEAD',
          cache: 'no-cache'
        })

        const actuallyOnline = response.ok

        if (actuallyOnline !== isOnline.value) {
          if (actuallyOnline) {
            handleOnline()
          } else {
            handleOffline()
          }
        }
      } catch (error) {
        console.error('Network check failed:', error)
        if (isOnline.value) {
          handleOffline()
        }
      }
    }, 30000)
  }

  const stopNetworkMonitoring = () => {
    if (networkCheckInterval) {
      clearInterval(networkCheckInterval)
      networkCheckInterval = null
    }

    if (syncRetryTimeout) {
      clearTimeout(syncRetryTimeout)
      syncRetryTimeout = null
    }

    if ('connection' in navigator) {
      const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
      if (connection) {
        connection.removeEventListener('change', updateConnectionInfo)
      }
    }
  }

  const getNetworkQuality = () => {
    if (!isOnline.value) return 'offline'

    switch (connectionSpeed.value) {
      case 'slow-2g':
      case '2g':
        return 'poor'
      case '3g':
        return 'fair'
      case '4g':
        return 'good'
      default:
        return 'unknown'
    }
  }

  const isHighLatency = () => {
    const quality = getNetworkQuality()
    return quality === 'poor' || quality === 'offline'
  }

  const shouldUseOfflineMode = () => {
    return !isOnline.value || isHighLatency()
  }

  onMounted(() => {
    // Get initial connection info
    getConnectionInfo()

    // Add event listeners
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)

    // Start monitoring
    startNetworkMonitoring()
  })

  onUnmounted(() => {
    // Clean up
    stopNetworkMonitoring()
    window.removeEventListener('online', handleOnline)
    window.removeEventListener('offline', handleOffline)
  })

  return {
    isOnline,
    connectionType,
    connectionSpeed,
    lastOnlineTime,
    offlineDuration,
    getNetworkQuality,
    isHighLatency,
    shouldUseOfflineMode,
    triggerSync,
    startNetworkMonitoring,
    stopNetworkMonitoring
  }
}
