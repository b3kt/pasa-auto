<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">{{ $t('app.menu.admin.clearCache.title') }}</div>

    <div v-if="loading" class="row justify-center q-pa-xl">
      <q-spinner-dots color="primary" size="50px" />
    </div>

    <template v-else>
      <!-- Stats cards -->
      <div class="row q-col-gutter-md q-mb-md">
        <div class="col-12 col-sm-4">
          <q-card flat bordered>
            <q-card-section class="row items-center no-wrap">
              <q-icon name="storage" size="36px" color="primary" class="q-mr-md" />
              <div>
                <div class="text-caption text-grey">{{ $t('pages.clearCache.totalEntries') }}</div>
                <div class="text-h5 text-weight-bold">{{ stats.total }}</div>
              </div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-12 col-sm-4">
          <q-card flat bordered>
            <q-card-section class="row items-center no-wrap">
              <q-icon name="check_circle" size="36px" color="green" class="q-mr-md" />
              <div>
                <div class="text-caption text-grey">{{ $t('pages.clearCache.active') }}</div>
                <div class="text-h5 text-weight-bold text-green">{{ stats.active }}</div>
              </div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-12 col-sm-4">
          <q-card flat bordered>
            <q-card-section class="row items-center no-wrap">
              <q-icon name="timer_off" size="36px" color="orange" class="q-mr-md" />
              <div>
                <div class="text-caption text-grey">{{ $t('pages.clearCache.expired') }}</div>
                <div class="text-h5 text-weight-bold text-orange">{{ stats.expired }}</div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Per-endpoint breakdown -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-subtitle1 q-mb-sm">{{ $t('pages.clearCache.cachePerEndpoint') }}</div>
          <q-table
            :rows="prefixRows"
            :columns="prefixColumns"
            row-key="prefix"
            flat dense
            hide-bottom
            :pagination="{ rowsPerPage: 0 }"
          >
            <template v-slot:body-cell-actions="props">
              <q-td :props="props">
                <q-btn flat dense round icon="delete_sweep" color="negative" size="sm"
                  @click="invalidatePrefix(props.row.prefix)">
                  <q-tooltip>{{ $t('pages.clearCache.clearEndpointTooltip') }}</q-tooltip>
                </q-btn>
              </q-td>
            </template>
          </q-table>
          <div v-if="prefixRows.length === 0" class="text-center text-grey q-pa-md">
            {{ $t('pages.clearCache.noCachedData') }}
          </div>
        </q-card-section>
      </q-card>

      <!-- Clear all -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section class="row items-center">
          <div>
            <div class="text-subtitle1">{{ $t('pages.clearCache.resetAllTitle') }}</div>
            <div class="text-caption text-grey">{{ $t('pages.clearCache.resetAllDescription') }}</div>
          </div>
          <q-space />
          <q-btn :label="$t('pages.clearCache.resetCacheButton')" icon="delete_forever" color="negative" :loading="clearing" @click="confirmClearAll" />
        </q-card-section>
      </q-card>

      <!-- Clear server-side caffeine cache (Admin/Owner only; Karyawan may only clear the browser cache above) -->
      <q-card v-if="canClearCaffeine" flat bordered>
        <q-card-section class="row items-center">
          <div>
            <div class="text-subtitle1">{{ $t('pages.clearCache.resetCaffeineTitle') }}</div>
            <div class="text-caption text-grey">{{ $t('pages.clearCache.resetCaffeineDescription') }}</div>
          </div>
          <q-space />
          <q-btn :label="$t('pages.clearCache.clearCaffeineButton')" icon="delete_forever" color="negative" :loading="clearingCaffeine" @click="confirmClearCaffeine" />
        </q-card-section>
      </q-card>
    </template>

    <!-- Confirm dialog -->
    <q-dialog v-model="confirmDialog">
      <q-card style="min-width: 320px">
        <q-card-section class="row items-center">
          <q-avatar icon="warning" color="negative" text-color="white" />
          <span class="q-ml-sm">{{ $t('pages.clearCache.confirmClearAllMessage') }}</span>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat :label="$t('cancel')" v-close-popup />
          <q-btn flat :label="$t('delete')" color="negative" @click="clearAll" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Confirm dialog: caffeine cache -->
    <q-dialog v-model="confirmCaffeineDialog">
      <q-card style="min-width: 320px">
        <q-card-section class="row items-center">
          <q-avatar icon="warning" color="negative" text-color="white" />
          <span class="q-ml-sm">{{ $t('pages.clearCache.confirmClearCaffeineMessage') }}</span>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat :label="$t('cancel')" v-close-popup />
          <q-btn flat :label="$t('delete')" color="negative" @click="clearCaffeine" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { api } from 'boot/axios'
import { useAuthStore } from 'stores/auth-store'
import masterDataCache from 'src/utils/masterDataCache'

const $q = useQuasar()
const { t } = useI18n()
const authStore = useAuthStore()
const loading = ref(false)
const clearing = ref(false)
const clearingCaffeine = ref(false)
const confirmDialog = ref(false)
const confirmCaffeineDialog = ref(false)
const stats = ref({ total: 0, active: 0, expired: 0, byPrefix: {} })

// Clearing the server-side Caffeine cache is Admin/Owner territory; Karyawan only gets the
// client-side browser cache reset above.
const canClearCaffeine = computed(() => {
  const roles = authStore.user?.roles || []
  return roles.includes('Admin') || roles.includes('Owner')
})

const prefixColumns = computed(() => [
  { name: 'prefix', label: t('pages.clearCache.endpointColumn'), field: 'prefix', align: 'left' },
  { name: 'total', label: t('pages.clearCache.entriesColumn'), field: 'total', align: 'center' },
  { name: 'active', label: t('pages.clearCache.active'), field: 'active', align: 'center' },
  { name: 'actions', label: '', field: 'actions', align: 'center' }
])

const prefixRows = computed(() =>
  Object.entries(stats.value.byPrefix).map(([prefix, counts]) => ({
    prefix,
    total: counts.total,
    active: counts.active
  }))
)

async function loadStats() {
  loading.value = true
  try {
    stats.value = await masterDataCache.getStats()
  } finally {
    loading.value = false
  }
}

function confirmClearAll() {
  confirmDialog.value = true
}

async function clearAll() {
  clearing.value = true
  try {
    await masterDataCache.clearAll()
    $q.notify({ type: 'positive', message: t('pages.clearCache.clearAllSuccess') })
    await loadStats()
  } finally {
    clearing.value = false
  }
}

async function invalidatePrefix(prefix) {
  const count = await masterDataCache.invalidatePrefix(prefix)
  $q.notify({ type: 'positive', message: t('pages.clearCache.invalidatePrefixSuccess', { count, prefix }) })
  await loadStats()
}

function confirmClearCaffeine() {
  confirmCaffeineDialog.value = true
}

async function clearCaffeine() {
  clearingCaffeine.value = true
  try {
    await api.post('/api/admin/caffeine-cache/clear')
    $q.notify({ type: 'positive', message: t('pages.clearCache.clearCaffeineSuccess') })
  } finally {
    clearingCaffeine.value = false
  }
}

onMounted(loadStats)
</script>
