<template>
  <q-page padding>
    <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                  @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                  row-key="id" :search-placeholder="$t('pages.customerBookingsPage.searchPlaceholder')">
      <template v-slot:title>
        <div class="text-h6 q-mb-md">{{ $t('pages.customerBookingsPage.title') }}</div>
      </template>

      <template v-slot:toolbar-filters>
        <q-select v-model="filterStatus" :label="$t('pages.customerBookingsPage.statusColumn')" dense outlined
                  bg-color="white" :options="statusOptions" emit-value map-options clearable style="min-width: 160px"/>
      </template>

      <template v-slot:body-cell-status="props">
        <q-badge :color="statusColor(props.value)">{{ statusLabel(props.value) }}</q-badge>
      </template>

      <template v-slot:body-cell-actions="props">
        <template v-if="props.row.status === 'REQUESTED'">
          <q-btn dense flat color="positive" :label="$t('pages.customerBookingsPage.actionConfirm')"
                 @click.stop="confirm(props.row)"/>
          <q-btn dense flat color="negative" :label="$t('pages.customerBookingsPage.actionReject')"
                 @click.stop="reject(props.row)"/>
        </template>
        <q-btn v-else-if="props.row.status === 'CONFIRMED'" dense flat color="primary"
               :label="$t('pages.customerBookingsPage.actionConvert')" @click.stop="openConvert(props.row)"/>
      </template>
    </GenericTable>

    <GenericDialog v-model="convertDialog" :title="$t('pages.customerBookingsPage.convertTitle')"
                   position="standard" min-width="400px">
      <div class="q-gutter-sm">
        <q-select v-model="selectedMekaniks" :label="$t('pages.customerBookingsPage.selectMechanicLabel')" outlined dense
                  multiple :options="karyawanOptions" option-label="namaKaryawan" option-value="id" use-chips use-input
                  input-debounce="300" @filter="filterKaryawan" :loading="loadingKaryawan"
                  emit-value map-options>
          <template v-slot:option="{ itemProps, opt }">
            <q-item v-bind="itemProps">
              <q-item-section side>
                <q-checkbox :model-value="isSelected(opt.id)" @update:model-value="toggleMechanic(opt)"/>
              </q-item-section>
              <q-item-section @click.stop>
                <q-item-label>{{ opt.namaKaryawan }}</q-item-label>
              </q-item-section>
              <q-item-section @click.stop side v-if="isSelected(opt.id)">
                <q-select :model-value="getMekanikTugas(opt.id)"
                          @update:model-value="setMekanikTugas(opt.id, $event)"
                          :options="['Utama', 'Pembantu']" dense outlined style="min-width: 120px"/>
              </q-item-section>
            </q-item>
          </template>
        </q-select>
        <q-input v-model.number="convertForm.km" :label="$t('pages.customerBookingsPage.kmLabel')" type="number"
                 outlined dense/>
      </div>
      <template #actions>
        <q-btn flat :label="$t('cancel')" color="primary" v-close-popup/>
        <q-btn color="primary" :label="$t('pages.customerBookingsPage.convertButton')" @click="convert"/>
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { api } from 'boot/axios'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'

const $q = useQuasar()
const { t } = useI18n()

const STATUS_VALUES = ['REQUESTED', 'CONFIRMED', 'CONVERTED', 'REJECTED', 'CANCELLED']

const allBookings = ref([])
const rows = ref([])
const loading = ref(false)
const searchText = ref('')
const filterStatus = ref(null)
const convertDialog = ref(false)
const convertForm = reactive({ id: null, km: null })
const karyawanOptions = ref([])
const selectedMekaniks = ref([])  // Array of { id, namaKaryawan, tugas }
const loadingKaryawan = ref(false)

const pagination = ref({
  sortBy: 'requestedDateTime',
  descending: true,
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0
})

const columns = computed(() => [
  { name: 'customerEmail', label: t('pages.customerBookingsPage.customerColumn'), field: 'customerEmail', align: 'left', sortable: true },
  { name: 'namaPelanggan', label: t('pages.customerBookingsPage.pelangganColumn'), field: 'namaPelanggan', align: 'left', sortable: true },
  { name: 'nopol', label: t('pages.customerBookingsPage.plateColumn'), field: 'nopol', align: 'left', sortable: true },
  { name: 'km', label: t('pages.customerBookingsPage.kmColumn'), field: 'km', align: 'right', sortable: true },
  { name: 'requestedDateTime', label: t('pages.customerBookingsPage.requestedColumn'), field: 'requestedDateTime', align: 'left', sortable: true,
    format: (v) => v ? new Date(v).toLocaleString() : '' },
  { name: 'status', label: t('pages.customerBookingsPage.statusColumn'), field: 'status', align: 'center', sortable: true },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
])

const statusOptions = computed(() =>
  STATUS_VALUES.map(value => ({ value, label: t(`pages.customerBookingsPage.statuses.${value}`) }))
)

const statusLabel = (status) => (STATUS_VALUES.includes(status) ? t(`pages.customerBookingsPage.statuses.${status}`) : status)

const statusColor = (status) => ({
  REQUESTED: 'grey', CONFIRMED: 'primary', CONVERTED: 'positive', REJECTED: 'negative', CANCELLED: 'negative'
})[status] || 'grey'

function compareValues(a, b) {
  if (a === b) return 0
  if (a === null || a === undefined) return -1
  if (b === null || b === undefined) return 1
  if (typeof a === 'number' && typeof b === 'number') return a - b
  return String(a).localeCompare(String(b), undefined, { numeric: true, sensitivity: 'base' })
}

// The admin endpoint returns the whole queue, so filtering, sorting and paging happen here
function applyFilters() {
  let list = allBookings.value

  if (filterStatus.value) {
    list = list.filter(b => b.status === filterStatus.value)
  }

  const needle = searchText.value.trim().toLowerCase()
  if (needle) {
    list = list.filter(b =>
      (b.customerEmail || '').toLowerCase().includes(needle) ||
      (b.namaPelanggan || '').toLowerCase().includes(needle) ||
      (b.nopol || '').toLowerCase().includes(needle)
    )
  }

  const { sortBy, descending } = pagination.value
  if (sortBy) {
    list = [...list].sort((a, b) => compareValues(a[sortBy], b[sortBy]))
    if (descending) list.reverse()
  }

  pagination.value.rowsNumber = list.length

  const totalPages = Math.max(1, Math.ceil(list.length / pagination.value.rowsPerPage))
  if (pagination.value.page > totalPages) pagination.value.page = totalPages

  const start = (pagination.value.page - 1) * pagination.value.rowsPerPage
  rows.value = list.slice(start, start + pagination.value.rowsPerPage)
}

function onRequest(props) {
  const { page, rowsPerPage, sortBy, descending } = props.pagination
  pagination.value.page = page
  pagination.value.rowsPerPage = rowsPerPage
  pagination.value.sortBy = sortBy
  pagination.value.descending = descending
  applyFilters()
}

function onSearch(val) {
  searchText.value = val || ''
  pagination.value.page = 1
  applyFilters()
}

watch(filterStatus, () => {
  pagination.value.page = 1
  applyFilters()
})

onMounted(load)
onMounted(fetchKaryawan)

async function load() {
  loading.value = true
  try {
    const response = await api.get('/api/pazaauto/customer-bookings')
    allBookings.value = response.data?.data || []
    applyFilters()
  } catch (e) {
    $q.notify({ type: 'negative', message: t('pages.customerBookingsPage.fetchFailed'), caption: e.response?.data?.message || e.message })
  } finally {
    loading.value = false
  }
}

async function confirm(row) {
  try {
    await api.post(`/api/pazaauto/customer-bookings/${row.id}/confirm`, {})
    $q.notify({ type: 'positive', message: t('pages.customerBookingsPage.notifyConfirmed') })
    await load()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || t('pages.customerBookingsPage.confirmFailed') })
  }
}

async function reject(row) {
  try {
    await api.post(`/api/pazaauto/customer-bookings/${row.id}/reject`, {})
    $q.notify({ type: 'info', message: t('pages.customerBookingsPage.notifyRejected') })
    await load()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || t('pages.customerBookingsPage.rejectFailed') })
  }
}

function openConvert(row) {
  convertForm.id = row.id
  selectedMekaniks.value = []
  // Seed with the odometer the customer reported; staff can correct it to the real intake reading
  convertForm.km = row.km ?? null
  convertDialog.value = true
}

async function fetchKaryawan() {
  loadingKaryawan.value = true
  try {
    const response = await api.get('/api/pazaauto/karyawan')
    if (response.data.success) {
      karyawanOptions.value = response.data.data || []
    }
  } catch (e) {
    $q.notify({ type: 'negative', message: t('pages.customerBookingsPage.fetchKaryawanFailed'), caption: e.response?.data?.message || e.message })
  } finally {
    loadingKaryawan.value = false
  }
}

function filterKaryawan(val, update) {
  update(() => {})
}

function isSelected(id) {
  return selectedMekaniks.value.some(m => m.id === id)
}

function toggleMechanic(mechanic) {
  const index = selectedMekaniks.value.findIndex(m => m.id === mechanic.id)
  if (index > -1) {
    selectedMekaniks.value.splice(index, 1)
  } else {
    selectedMekaniks.value.push({ id: mechanic.id, namaKaryawan: mechanic.namaKaryawan, tugas: 'Utama' })
  }
}

function getMekanikTugas(id) {
  return selectedMekaniks.value.find(m => m.id === id)?.tugas || 'Utama'
}

function setMekanikTugas(id, tugas) {
  const mechanic = selectedMekaniks.value.find(m => m.id === id)
  if (mechanic) {
    mechanic.tugas = tugas
  }
}

async function convert() {
  try {
    await api.post(`/api/pazaauto/customer-bookings/${convertForm.id}/convert-to-spk`, {
      mekanikList: selectedMekaniks.value.map(m => ({ id: m.id, tugas: m.tugas || 'Utama' })),
      km: convertForm.km
    })
    $q.notify({ type: 'positive', message: t('pages.customerBookingsPage.notifyConverted') })
    convertDialog.value = false
    await load()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || t('pages.customerBookingsPage.convertFailed') })
  }
}
</script>
