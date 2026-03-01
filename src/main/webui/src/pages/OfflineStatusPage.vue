<template>
  <div class="q-pa-md">
    <div class="row justify-center">
      <div class="col-12 col-md-8 col-lg-6">
        <q-card>
          <q-card-section>
            <div class="text-h6 q-mb-md">
              <q-icon
                :name="isOnline ? 'wifi' : 'wifi_off'"
                :color="isOnline ? 'positive' : 'negative'"
                class="q-mr-sm"
              />
              Network Status
            </div>

            <q-list>
              <q-item>
                <q-item-section avatar>
                  <q-icon
                    :name="isOnline ? 'check_circle' : 'error'"
                    :color="isOnline ? 'positive' : 'negative'"
                  />
                </q-item-section>
                <q-item-section>
                  <q-item-label>Connection Status</q-item-label>
                  <q-item-label caption>
                    {{ isOnline ? 'Online' : 'Offline' }}
                  </q-item-label>
                </q-item-section>
              </q-item>

              <q-item>
                <q-item-section avatar>
                  <q-icon name="router" color="primary" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>Connection Type</q-item-label>
                  <q-item-label caption>
                    {{ connectionType || 'Unknown' }}
                  </q-item-label>
                </q-item-section>
              </q-item>

              <q-item>
                <q-item-section avatar>
                  <q-icon name="speed" color="primary" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>Connection Speed</q-item-label>
                  <q-item-label caption>
                    {{ connectionSpeed || 'Unknown' }}
                  </q-item-label>
                </q-item-section>
              </q-item>

              <q-item>
                <q-item-section avatar>
                  <q-icon name="sync" color="primary" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>Sync Status</q-item-label>
                  <q-item-label caption>
                    {{ syncStatusText }}
                  </q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-card-section>

          <q-separator />

          <q-card-section>
            <div class="text-h6 q-mb-md">Offline Actions</div>

            <div class="q-gutter-md">
              <q-btn
                color="primary"
                icon="sync"
                label="Force Sync Now"
                :loading="isSyncing"
                @click="handleForceSync"
                :disable="!isOnline"
              />

              <q-btn
                color="warning"
                icon="clear"
                label="Clear Offline Data"
                @click="handleClearData"
              />

              <q-btn
                color="info"
                icon="info"
                label="Storage Info"
                @click="showStorageInfo"
              />
            </div>
          </q-card-section>

          <q-separator />

          <q-card-section>
            <div class="text-h6 q-mb-md">Pending Requests</div>

            <div v-if="pendingRequests.length === 0" class="text-grey-6">
              No pending requests
            </div>

            <q-list v-else dense>
              <q-item
                v-for="request in pendingRequests.slice(0, 5)"
                :key="request.id"
              >
                <q-item-section avatar>
                  <q-icon
                    :name="getMethodIcon(request.method)"
                    :color="getMethodColor(request.method)"
                  />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ request.method }} {{ request.url }}</q-item-label>
                  <q-item-label caption>
                    {{ formatTimestamp(request.timestamp) }}
                  </q-item-label>
                </q-item-section>
              </q-item>

              <q-item v-if="pendingRequests.length > 5">
                <q-item-section>
                  <q-item-label caption>
                    ... and {{ pendingRequests.length - 5 }} more
                  </q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, onMounted, onUnmounted } from 'vue'
import { useNetworkStatus } from '../composables/useNetworkStatus.js'
import syncService from '../services/syncService.js'
import { Notify, date } from 'quasar'

export default defineComponent({
  name: 'OfflineStatusPage',
  setup() {
    const { isOnline, connectionType, connectionSpeed } = useNetworkStatus()
    
    const pendingRequests = ref([])
    const isSyncing = ref(false)
    const lastSyncTime = ref(null)

    const syncStatusText = ref('Checking...')

    const updateSyncStatus = async () => {
      try {
        const status = await syncService.getSyncStatus()
        pendingRequests.value = await syncService.storage.getPendingRequests()
        isSyncing.value = status.isSyncing
        lastSyncTime.value = status.lastSyncTime

        if (status.isSyncing) {
          syncStatusText.value = 'Syncing...'
        } else if (status.pendingRequests > 0) {
          syncStatusText.value = `${status.pendingRequests} pending requests`
        } else {
          syncStatusText.value = 'All synced'
        }
      } catch (error) {
        console.error('Failed to get sync status:', error)
        syncStatusText.value = 'Error checking status'
      }
    }

    const handleForceSync = async () => {
      if (!isOnline.value) {
        Notify.create({
          type: 'warning',
          message: 'Cannot sync while offline',
          position: 'top-right'
        })
        return
      }

      isSyncing.value = true
      try {
        const success = await syncService.forceSyncAll()
        if (success) {
          await updateSyncStatus()
        }
      } catch (error) {
        console.error('Force sync failed:', error)
        Notify.create({
          type: 'negative',
          message: 'Sync failed. Please try again.',
          position: 'top-right'
        })
      } finally {
        isSyncing.value = false
      }
    }

    const handleClearData = async () => {
      try {
        const success = await syncService.clearAllOfflineData()
        if (success) {
          await updateSyncStatus()
        }
      } catch (error) {
        console.error('Failed to clear offline data:', error)
      }
    }

    const showStorageInfo = async () => {
      try {
        const storageSize = await syncService.storage.getStorageSize()
        if (storageSize) {
          const usedMB = (storageSize.usage / 1024 / 1024).toFixed(2)
          const quotaMB = (storageSize.quota / 1024 / 1024).toFixed(2)

          Notify.create({
            type: 'info',
            message: `Storage: ${usedMB}MB used of ${quotaMB}MB`,
            position: 'top-right',
            timeout: 5000
          })
        } else {
          Notify.create({
            type: 'info',
            message: 'Storage information not available',
            position: 'top-right'
          })
        }
      } catch (error) {
        console.error('Failed to get storage info:', error)
      }
    }

    const getMethodIcon = (method) => {
      const icons = {
        GET: 'download',
        POST: 'add',
        PUT: 'edit',
        DELETE: 'delete',
        PATCH: 'edit'
      }
      return icons[method] || 'send'
    }

    const getMethodColor = (method) => {
      const colors = {
        GET: 'blue',
        POST: 'green',
        PUT: 'orange',
        DELETE: 'red',
        PATCH: 'purple'
      }
      return colors[method] || 'grey'
    }

    const formatTimestamp = (timestamp) => {
      return date.formatDate(timestamp, 'MMM DD, HH:mm')
    }

    let statusInterval = null

    onMounted(() => {
      updateSyncStatus()
      // Update status every 5 seconds
      statusInterval = setInterval(updateSyncStatus, 5000)
    })

    onUnmounted(() => {
      if (statusInterval) {
        clearInterval(statusInterval)
      }
    })

    return {
      isOnline,
      connectionType,
      connectionSpeed,
      pendingRequests,
      isSyncing,
      syncStatusText,
      handleForceSync,
      handleClearData,
      showStorageInfo,
      getMethodIcon,
      getMethodColor,
      formatTimestamp
    }
  }
})
</script>

<style scoped>
.q-card {
  max-width: 600px;
}
</style>
