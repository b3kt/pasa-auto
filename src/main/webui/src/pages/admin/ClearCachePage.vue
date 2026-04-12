<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">Browser Cache</div>

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
                <div class="text-caption text-grey">Total Entries</div>
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
                <div class="text-caption text-grey">Active</div>
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
                <div class="text-caption text-grey">Expired</div>
                <div class="text-h5 text-weight-bold text-orange">{{ stats.expired }}</div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Per-endpoint breakdown -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-subtitle1 q-mb-sm">Cache per Endpoint</div>
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
                  <q-tooltip>Clear cache for this endpoint</q-tooltip>
                </q-btn>
              </q-td>
            </template>
          </q-table>
          <div v-if="prefixRows.length === 0" class="text-center text-grey q-pa-md">
            No cached data
          </div>
        </q-card-section>
      </q-card>

      <!-- Clear all -->
      <q-card flat bordered>
        <q-card-section class="row items-center">
          <div>
            <div class="text-subtitle1">Reset Semua Cache</div>
            <div class="text-caption text-grey">Menghapus seluruh data cache master. Data akan di-fetch ulang dari server saat halaman dibuka.</div>
          </div>
          <q-space />
          <q-btn label="Reset Cache" icon="delete_forever" color="negative" :loading="clearing" @click="confirmClearAll" />
        </q-card-section>
      </q-card>
    </template>

    <!-- Confirm dialog -->
    <q-dialog v-model="confirmDialog">
      <q-card style="min-width: 320px">
        <q-card-section class="row items-center">
          <q-avatar icon="warning" color="negative" text-color="white" />
          <span class="q-ml-sm">Hapus semua browser cache?</span>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Batal" v-close-popup />
          <q-btn flat label="Hapus" color="negative" @click="clearAll" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import masterDataCache from 'src/utils/masterDataCache'

const $q = useQuasar()
const loading = ref(false)
const clearing = ref(false)
const confirmDialog = ref(false)
const stats = ref({ total: 0, active: 0, expired: 0, byPrefix: {} })

const prefixColumns = [
  { name: 'prefix', label: 'Endpoint', field: 'prefix', align: 'left' },
  { name: 'total', label: 'Entries', field: 'total', align: 'center' },
  { name: 'active', label: 'Active', field: 'active', align: 'center' },
  { name: 'actions', label: '', field: 'actions', align: 'center' }
]

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
    $q.notify({ type: 'positive', message: 'Cache berhasil dihapus' })
    await loadStats()
  } finally {
    clearing.value = false
  }
}

async function invalidatePrefix(prefix) {
  const count = await masterDataCache.invalidatePrefix(prefix)
  $q.notify({ type: 'positive', message: `${count} entri cache dihapus untuk ${prefix}` })
  await loadStats()
}

onMounted(loadStats)
</script>
