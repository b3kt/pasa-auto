<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">Log Aktivitas</div>

    <!-- Filters -->
    <q-card flat bordered class="q-mb-md">
      <q-card-section>
        <div class="row q-col-gutter-md items-end">
          <div class="col-12 col-sm-3">
            <q-input v-model="filters.tableName" dense outlined label="Nama Tabel" />
          </div>
          <div class="col-12 col-sm-3">
            <q-input v-model="filters.recordId" dense outlined label="ID Record" type="number" />
          </div>
          <div class="col-12 col-sm-2">
            <q-select v-model="filters.action" dense outlined label="Aksi" :options="actionOptions" clearable />
          </div>
          <div class="col-12 col-sm-2">
            <q-select v-model="filters.username" dense outlined label="User" :options="userOptions" clearable />
          </div>
          <div class="col-12 col-sm-2">
            <q-btn color="primary" label="Cari" icon="search" @click="loadData" :loading="loading" />
          </div>
        </div>
      </q-card-section>
    </q-card>

    <!-- Results table -->
    <q-table
      :rows="rows"
      :columns="columns"
      row-key="id"
      :loading="loading"
      :pagination="pagination"
      @update:pagination="v => pagination = v"
      flat
      :rows-per-page-options="[10, 20, 50, 0]"
    >
      <template v-slot:body-cell-action="props">
        <q-td :props="props">
          <q-chip
            :color="getActionColor(props.value)"
            text-color="white"
            size="sm"
          >
            {{ props.value }}
          </q-chip>
        </q-td>
      </template>

      <template v-slot:body-cell-changes="props">
        <q-td :props="props">
          <q-btn
            flat dense size="sm"
            icon="compare_arrows"
            color="primary"
            @click="showDiff(props.row)"
          >
            <q-tooltip>Lihat perubahan</q-tooltip>
          </q-btn>
        </q-td>
      </template>

      <template v-slot:body-cell-timestamp="props">
        <q-td :props="props">
          {{ formatDate(props.value) }}
        </q-td>
      </template>
    </q-table>

    <!-- Diff viewer dialog -->
    <q-dialog v-model="diffDialog" maximized>
      <q-card>
        <q-card-section class="row items-center">
          <div class="text-h6">
            Perubahan: {{ selectedRow?.tableName }} ({{ selectedRow?.action }})
            <q-chip :color="getActionColor(selectedRow?.action)" text-color="white" size="sm">
              {{ selectedRow?.action }}
            </q-chip>
          </div>
          <q-space />
          <q-btn flat icon="close" v-close-popup />
        </q-card-section>
        
        <q-card-section class="q-pa-none">
          <div class="text-caption q-mb-sm text-grey">
            User: {{ selectedRow?.username || 'system' }} | 
            Record ID: {{ selectedRow?.recordId }} | 
            Waktu: {{ formatDate(selectedRow?.timestamp) }}
          </div>
        </q-card-section>
        
        <q-separator />
        
        <q-card-section style="height: calc(100vh - 200px)">
          <div class="row q-col-gutter-md" style="height: 100%">
            <!-- Before value column -->
            <div class="col-12 col-md-6" style="height: 100%">
              <div class="text-subtitle2 q-mb-sm text-negative">
                <q-icon name="arrow_back" class="q-mr-xs" />
                SEBELUM
              </div>
              <div class="diff-viewer q-pa-sm bg-grey-2 rounded-borders" style="height: calc(100% - 30px); overflow: auto;">
                <template v-if="diffData.before">
                  <table class="diff-table">
                    <template v-for="(value, key) in diffData.before" :key="key">
                      <tr v-if="diffData.after && diffData.after[key] !== value" class="diff-removed">
                        <td class="diff-key">{{ key }}</td>
                        <td class="diff-value">{{ formatValue(value) }}</td>
                      </tr>
                      <tr v-else-if="!diffData.after || diffData.after[key] === value">
                        <td class="diff-key">{{ key }}</td>
                        <td class="diff-value">{{ formatValue(value) }}</td>
                      </tr>
                    </template>
                    <tr v-if="!diffData.before || Object.keys(diffData.before).length === 0">
                      <td class="text-grey">(tidak ada)</td>
                    </tr>
                  </table>
                </template>
                <template v-else>
                  <div class="text-grey">(tidak ada)</div>
                </template>
              </div>
            </div>
            
            <!-- After value column -->
            <div class="col-12 col-md-6" style="height: 100%">
              <div class="text-subtitle2 q-mb-sm text-positive">
                <q-icon name="arrow_forward" class="q-mr-xs" />
                SESUDAH
              </div>
              <div class="diff-viewer q-pa-sm bg-grey-2 rounded-borders" style="height: calc(100% - 30px); overflow: auto;">
                <template v-if="diffData.after">
                  <table class="diff-table">
                    <template v-for="(value, key) in diffData.after" :key="key">
                      <tr v-if="diffData.before && diffData.before[key] !== value" class="diff-added">
                        <td class="diff-key">{{ key }}</td>
                        <td class="diff-value">{{ formatValue(value) }}</td>
                      </tr>
                      <tr v-else-if="!diffData.before || diffData.before[key] === value">
                        <td class="diff-key">{{ key }}</td>
                        <td class="diff-value">{{ formatValue(value) }}</td>
                      </tr>
                    </template>
                    <tr v-if="!diffData.after || Object.keys(diffData.after).length === 0">
                      <td class="text-grey">(tidak ada)</td>
                    </tr>
                  </table>
                </template>
                <template v-else>
                  <div class="text-grey">(tidak ada)</div>
                </template>
              </div>
            </div>
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from 'boot/axios'
import { useQuasar } from 'quasar'

const $q = useQuasar()

const loading = ref(false)
const rows = ref([])
const pagination = ref({ page: 1, rowsPerPage: 10, rowsNumber: 0 })

const filters = ref({
  tableName: '',
  recordId: null,
  action: null,
  username: ''
})

const actionOptions = ['INSERT', 'UPDATE', 'DELETE']
const userOptions = ref([])

const diffDialog = ref(false)
const selectedRow = ref(null)
const diffData = ref({ before: null, after: null })

const columns = [
  { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
  { name: 'table_name', label: 'Tabel', field: 'tableName', align: 'left', sortable: true },
  { name: 'action', label: 'Aksi', field: 'action', align: 'center', sortable: true },
  { name: 'username', label: 'User', field: 'username', align: 'left', sortable: true },
  { name: 'timestamp', label: 'Waktu', field: 'timestamp', align: 'left', sortable: true },
  { name: 'record_id', label: 'Record ID', field: 'recordId', align: 'right', sortable: true },
  { name: 'changes', label: 'Perubahan', field: 'changes', align: 'center' }
]

async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      rowsPerPage: pagination.value.rowsPerPage,
      search: filters.value.tableName || undefined
    }

    const response = await api.get('/api/audit-trail/paginated', { params })
    if (response.data?.data) {
      rows.value = response.data.data.rows || []
      pagination.value.rowsNumber = response.data.data.rowsNumber || 0
    }
  } catch (e) {
    console.error(e);
    $q.notify({ type: 'negative', message: 'Gagal memuat data audit trail' })
  } finally {
    loading.value = false
  }
}

async function loadUserOptions() {
  try {
    const response = await api.get('/api/audit-trail')
    if (response.data?.data) {
      const users = new Set()
      response.data.data.forEach(r => {
        if (r.username) users.add(r.username)
      })
      userOptions.value = Array.from(users)
    }
  } catch (e) {
    console.error('Failed to load users:', e)
  }
}

function getActionColor(action) {
  switch (action) {
    case 'INSERT': return 'positive'
    case 'UPDATE': return 'warning'
    case 'DELETE': return 'negative'
    default: return 'grey'
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('id-ID', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatValue(value) {
  if (value === null) return 'null'
  if (value === undefined) return 'undefined'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function showDiff(row) {
  selectedRow.value = row
  diffData.value = {
    before: row.beforeValue || row.before_value || null,
    after: row.afterValue || row.after_value || null
  }
  diffDialog.value = true
}

onMounted(() => {
  loadData()
  loadUserOptions()
})
</script>

<style scoped>
.diff-viewer {
  font-family: monospace;
  font-size: 12px;
}

.diff-table {
  width: 100%;
  border-collapse: collapse;
}

.diff-table tr {
  border-bottom: 1px solid #eee;
}

.diff-table tr.diff-added {
  background-color: #e8f5e9;
}

.diff-table tr.diff-removed {
  background-color: #ffebee;
}

.diff-key {
  padding: 2px 8px 2px 0;
  color: #666;
  vertical-align: top;
  white-space: nowrap;
  width: 40%;
}

.diff-value {
  padding: 2px 0 2px 8px;
  word-break: break-all;
}
</style>