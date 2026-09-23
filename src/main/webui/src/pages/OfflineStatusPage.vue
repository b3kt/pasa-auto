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
              {{ $t('pages.offlineStatusPage.networkStatusTitle') }}
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
                  <q-item-label>{{ $t('pages.offlineStatusPage.connectionStatusLabel') }}</q-item-label>
                  <q-item-label caption>
                    {{ isOnline ? $t('pages.offlineStatusPage.online') : $t('pages.offlineStatusPage.offline') }}
                  </q-item-label>
                </q-item-section>
              </q-item>

              <q-item>
                <q-item-section avatar>
                  <q-icon name="router" color="primary" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ $t('pages.offlineStatusPage.connectionTypeLabel') }}</q-item-label>
                  <q-item-label caption>
                    {{ connectionType || $t('pages.offlineStatusPage.unknown') }}
                  </q-item-label>
                </q-item-section>
              </q-item>

              <q-item>
                <q-item-section avatar>
                  <q-icon name="speed" color="primary" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ $t('pages.offlineStatusPage.connectionSpeedLabel') }}</q-item-label>
                  <q-item-label caption>
                    {{ connectionSpeed || $t('pages.offlineStatusPage.unknown') }}
                  </q-item-label>
                </q-item-section>
              </q-item>

              <q-item>
                <q-item-section avatar>
                  <q-icon name="sync" color="primary" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ $t('pages.offlineStatusPage.syncStatusLabel') }}</q-item-label>
                  <q-item-label caption>
                    {{ syncStatusText }}
                  </q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-card-section>

          <q-separator />

          <q-card-section>
            <div class="text-h6 q-mb-md">{{ $t('pages.offlineStatusPage.offlineActionsTitle') }}</div>

            <div class="q-gutter-md">
              <q-btn
                color="primary"
                icon="sync"
                :label="$t('pages.offlineStatusPage.forceSyncButton')"
                :loading="isSyncing"
                @click="handleForceSync"
                :disable="!isOnline"
              />

              <q-btn
                color="warning"
                icon="clear"
                :label="$t('pages.offlineStatusPage.clearOfflineDataButton')"
                @click="handleClearData"
              />

              <q-btn
                color="info"
                icon="info"
                :label="$t('pages.offlineStatusPage.storageInfoButton')"
                @click="showStorageInfo"
              />
            </div>
          </q-card-section>

          <q-separator />

          <q-card-section>
            <div class="text-h6 q-mb-md">{{ $t('pages.offlineStatusPage.pendingRequestsTitle') }}</div>

            <div v-if="pendingRequests.length === 0" class="text-grey-6">
              {{ $t('pages.offlineStatusPage.noPendingRequestsLabel') }}
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
                    {{ $t('pages.offlineStatusPage.andMoreLabel', { count: pendingRequests.length - 5 }) }}
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
import { useI18n } from 'vue-i18n'
import { useNetworkStatus } from '../composables/useNetworkStatus.js'
import syncService from '../services/syncService.js'
import { Notify, date } from 'quasar'

export default defineComponent({
  name: 'OfflineStatusPage',
  setup() {
    const { t } = useI18n()
    const { isOnline, connectionType, connectionSpeed } = useNetworkStatus()
    
    const pendingRequests = ref([])
    const isSyncing = ref(false)
    const lastSyncTime = ref(null)

    const syncStatusText = ref(t('pages.offlineStatusPage.syncChecking'))

    const updateSyncStatus = async () => {
      try {
        const status = await syncService.getSyncStatus()
        pendingRequests.value = await syncService.storage.getPendingRequests()
        isSyncing.value = status.isSyncing
        lastSyncTime.value = status.lastSyncTime

        if (status.isSyncing) {
          syncStatusText.value = t('pages.offlineStatusPage.syncSyncing')
        } else if (status.pendingRequests > 0) {
          syncStatusText.value = t('pages.offlineStatusPage.pendingRequestsCount', { count: status.pendingRequests })
        } else {
          syncStatusText.value = t('pages.offlineStatusPage.syncAllSynced')
        }
      } catch (error) {
        console.error('Failed to get sync status:', error)
        syncStatusText.value = t('pages.offlineStatusPage.syncError')
      }
    }

    const handleForceSync = async () => {
      if (!isOnline.value) {
        Notify.create({
          type: 'warning',
          message: t('pages.offlineStatusPage.cannotSyncOffline'),
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
          message: t('pages.offlineStatusPage.syncFailed'),
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
            message: t('pages.offlineStatusPage.storageUsage', { used: usedMB, quota: quotaMB }),
            timeout: 5000
          })
        } else {
          Notify.create({
            type: 'info',
            message: t('pages.offlineStatusPage.storageInfoUnavailable'),
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
