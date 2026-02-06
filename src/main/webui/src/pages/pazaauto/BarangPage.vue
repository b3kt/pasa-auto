<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                      :on-create="openCreateDialog"
                      :on-edit="openEditDialog" v-model:search-value="searchText" ref="tableRef">
          <template v-slot:search-append>
            <q-btn round dense flat icon="qr_code_scanner" @click="openScanDialog('search')"/>
          </template>
          <template v-slot:toolbar-filters>
            <div class="col">
              <q-select v-model="filterStatus" multiple :options="statusOptions" :label="$t('availability')" dense
                        options-dense flat outlined/>
            </div>
          </template>

          <template v-slot:body-cell-active="props">
              <q-badge :color="props.row.active ? 'green' : 'red'">
                {{ props.row.active ? 'Active' : 'Inactive' }}
              </q-badge>
          </template>

          <template v-slot:body-cell-hargaJual="props">
              {{ formatCurrency(props.row.hargaJual) }}
          </template>

          <template v-slot:body-cell-hargaBeli="props">
              {{ formatCurrency(props.row.hargaBeli) }}
          </template>
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6 q-mb-md">{{ isEditMode ? 'Edit Barang' : 'Tambah data Barang' }}</div>
            <q-space/>
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>New</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="barang-form" class="q-gutter-md">
            <q-input v-model="formData.namaBarang" label="Nama Barang *" outlined dense
                     :rules="[val => !!val || 'Nama Barang harus diisi']" hide-bottom-space/>

            <q-input v-model.number="formData.hargaJual" label="Harga Jual" outlined dense type="number" step="0.01"
                     prefix="Rp"/>
            <q-input v-model.number="formData.hargaBeli" label="Harga Beli" outlined dense type="number" step="0.01"
                     prefix="Rp"/>

            <q-input v-model.number="formData.stok" label="Stok" outlined dense type="number"/>
            <q-input v-model.number="formData.stokMinimal" label="Stok Minimal" outlined dense type="number"/>

            <q-input v-model="formData.satuan" label="Satuan" outlined dense placeholder="e.g., pcs, kg, liter"/>

            <q-select v-model="formData.supplierId" label="Supplier" outlined dense :options="filteredSupplierOptions"
                      option-label="namaSupplier" option-value="id" emit-value map-options use-input
                      input-debounce="300"
                      @filter="filterSupplier" :loading="loadingSupplier" clearable>
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    No results
                  </q-item-section>
                </q-item>
              </template>
            </q-select>

            <q-input v-model="formData.keterangan" label="Keterangan" outlined dense type="textarea" rows="3"/>

            <q-checkbox v-model="formData.active" label="Active"/>

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode" label="Hapus" color="negative" flat @click="confirmDelete(formData)"
                     :loading="deleting"/>
              <q-btn label="Simpan" type="submit" color="primary" :loading="saving"
                     :disable="isEditMode && !isDirty(formData)"/>
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Scanner Dialog -->
    <q-dialog v-model="showScanDialog" @hide="onScanDialogHide">
      <q-card style="width: 500px; max-width: 80vw;">
        <q-card-section class="row items-center">
          <div class="text-h6">Scan Barcode</div>
          <q-space/>
          <q-btn icon="close" flat round dense v-close-popup/>
        </q-card-section>

        <q-card-section class="q-pa-none" style="height: 400px; position: relative;">
          <qrcode-stream @detect="onDetect" @camera-on="onCameraReady" @error="onError" :formats="['ean_13', 'ean_8']">
            <div
              style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 2px solid red; opacity: 0.5;">
            </div>
            <div v-if="error" class="absolute-full flex flex-center bg-negative text-white text-center q-pa-md">
              <div>
                <div class="q-mb-md">{{ error }}</div>
                <q-btn v-if="showPermissionButton" label="Request Permission" @click="requestPermission" outline
                       color="white"/>
              </div>
            </div>
          </qrcode-stream>
        </q-card-section>
      </q-card>
    </q-dialog>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" title="Konfirmasi hapus data" min-width="400px" position="standard">
      Are you sure you want to delete <strong>{{ itemToDelete?.namaBarang }}</strong>?
      <template #actions>
        <q-btn flat label="Batalkan" color="primary" @click="showDeleteDialog = false"/>
        <q-btn flat label="Hapus saja" color="negative" @click="deleteItem" :loading="deleting"/>
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import {ref, onMounted, watch, nextTick} from 'vue'
import {api} from 'boot/axios'
// import { useQuasar } from 'quasar'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import {useCrud} from 'src/composables/useCrud'
import {useKeyboardShortcuts} from 'src/composables/useKeyboardShortcuts'
import {QrcodeStream} from 'vue-qrcode-reader'

//const $q = useQuasar()

// LocalStorage key for filter persistence
const FILTER_STORAGE_KEY = 'barang_status_filter'

// Load filter from localStorage
const loadFilterFromStorage = () => {
  try {
    const stored = localStorage.getItem(FILTER_STORAGE_KEY)
    return stored ? JSON.parse(stored) : []
  } catch (error) {
    console.error('Failed to load filter from storage:', error)
    return []
  }
}

// Save filter to localStorage
const saveFilterToStorage = (filter) => {
  try {
    localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(filter))
  } catch (error) {
    console.error('Failed to save filter to storage:', error)
  }
}

// Filter State
const searchText = ref('')
const filterStatus = ref(loadFilterFromStorage())
const statusOptions = ref([
  'AVAILABLE',
  'OUT_OF_STOCK'
])

// Use CRUD Composable
const {
  rows,
  loading,
  saving,
  deleting,
  showDeleteDialog,
  isEditMode,
  itemToDelete,
  pagination,
  fetchData,
  onRequest,
  onSearch,
  saveData,
  confirmDelete,
  deleteItem: baseDeleteItem,
  openCreateDialog: baseOpenCreateDialog,
  openEditDialog: baseOpenEditDialog,
  isDirty
} = useCrud({
  baseApiUrl: '/api/pazaauto/barang'
})

const splitterModel = ref(70)
const tableRef = ref(null)

// Override fetchData to include custom filters
const originalFetchData = fetchData
const fetchBarang = async () => {
  const customParams = {}
  if (filterStatus.value && filterStatus.value.length > 0) {
    customParams.statusFilter = filterStatus.value.join(',')
  }
  await originalFetchData(customParams)
}

// Supplier Logic
const loadingSupplier = ref(false)
const supplierOptions = ref([])
const filteredSupplierOptions = ref([])

const fetchSupplier = async () => {
  loadingSupplier.value = true
  try {
    const response = await api.get('/api/pazaauto/supplier')
    if (response.data.success) {
      supplierOptions.value = response.data.data || []
      filteredSupplierOptions.value = supplierOptions.value
    }
  } catch (error) {
    console.error('Failed to fetch suppliers', error)
  } finally {
    loadingSupplier.value = false
  }
}

const filterSupplier = (val, update) => {
  update(() => {
    if (val === '') {
      filteredSupplierOptions.value = supplierOptions.value
    } else {
      const needle = val.toLowerCase()
      filteredSupplierOptions.value = supplierOptions.value.filter(
        v => v.namaSupplier.toLowerCase().includes(needle) > -1
      )
    }
  })
}

// Form Data
const formData = ref({
  id: null,
  kodeBarang: '',
  namaBarang: '',
  hargaJual: null,
  hargaBeli: null,
  stok: null,
  stokMinimal: null,
  satuan: '',
  supplierId: null,
  keterangan: '',
  active: true
})

const resetForm = () => {
  formData.value = {
    id: null,
    kodeBarang: '',
    namaBarang: '',
    hargaJual: null,
    hargaBeli: null,
    stok: null,
    stokMinimal: null,
    satuan: '',
    supplierId: null,
    keterangan: '',
    active: true
  }
}

const deleteItem = async () => {
  const success = await baseDeleteItem()
  if (success) {
    resetForm()
  }
}

const openCreateDialog = () => {
  baseOpenCreateDialog(resetForm)
}

const openEditDialog = (row) => {
  baseOpenEditDialog(row, (r) => {
    formData.value = {...r}
  })
}

const handleSave = async () => {
  const result = await saveData(formData.value)
  if (result) {
    formData.value = {...result}
    if (!isEditMode.value) {
      openEditDialog(result)
      tableRef.value?.selectRowByItem(result)
    }
  }
}

// Keyboard Shortcuts
useKeyboardShortcuts({
  onSave: () => {
    if (!saving.value && !(isEditMode.value && !isDirty(formData.value))) {
      handleSave()
    }
  },
  onDelete: () => {
    if (isEditMode.value && !deleting.value) {
      confirmDelete(formData.value)
    }
  },
  onNew: () => {
    openCreateDialog()
  }
})

// Scanner Logic
const showScanDialog = ref(false)
const scanMode = ref('edit') // 'edit' or 'search'
// const selectedDevice = ref(null)
// const devices = ref([])

const openScanDialog = (mode = 'edit') => {
  scanMode.value = mode
  showScanDialog.value = true
}

const onDetect = (detectedCodes) => {
  if (detectedCodes && detectedCodes.length > 0) {
    const code = detectedCodes[0].rawValue
    console.log(`Code matched = ${code}`)

    if (scanMode.value === 'edit') {
      formData.value.kodeBarang = code
    } else if (scanMode.value === 'search') {
      searchText.value = code
      // Trigger search immediately if needed, but GenericTable watches searchText
    }

    showScanDialog.value = false
  }
}

const error = ref('')
const showPermissionButton = ref(false)

const onCameraReady = async (capabilities) => {
  error.value = ''
  showPermissionButton.value = false
  console.log('Camera ready:', capabilities)
}

const onError = (err) => {
  console.error('QR Code Stream Error:', err)
  if (err.name === 'NotAllowedError') {
    error.value = 'Camera access denied. Please grant permission.'
    showPermissionButton.value = true
  } else if (err.name === 'NotFoundError') {
    error.value = 'No camera found on this device.'
  } else if (err.name === 'NotSupportedError') {
    error.value = 'Secure context required (HTTPS, localhost).'
  } else if (err.name === 'NotReadableError') {
    error.value = 'Camera is already in use.'
  } else if (err.name === 'OverconstrainedError') {
    error.value = 'Installed cameras are not suitable.'
  } else if (err.name === 'StreamApiNotSupportedError') {
    error.value = 'Stream API is not supported in this browser.'
  } else if (err.name === 'InsecureContextError') {
    error.value = 'Camera access is only permitted in secure context. Use HTTPS or localhost.'
  } else {
    error.value = `Camera error: ${err.name}`
  }
}

const requestPermission = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({video: true})
    if (stream) {
      stream.getTracks().forEach(track => track.stop())
      // Reload the component to retry
      showScanDialog.value = false
      nextTick(() => {
        showScanDialog.value = true
      })
    }
  } catch (err) {
    console.error('Permission request failed', err)
    onError(err)
  }
}

const onScanDialogHide = () => {
  // QrcodeStream handles stopping the camera automatically when destroyed/unmounted
  error.value = ''
  showPermissionButton.value = false
}

// Table Columns
const columns = [
  {name: 'namaBarang', required: true, label: 'Nama Barang', align: 'left', field: 'namaBarang', sortable: true},
  {name: 'hargaJual', label: 'Harga Jual', align: 'right', field: 'hargaJual', sortable: true},
  {name: 'hargaBeli', label: 'Harga Beli', align: 'right', field: 'hargaBeli', sortable: true},
  {name: 'stok', label: 'Stok', align: 'center', field: 'stok', sortable: true},
  {name: 'active', label: 'Status', align: 'center', field: 'active', sortable: true}
]

const formatCurrency = (value) => {
  if (!value) return 'Rp 0'
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    minimumFractionDigits: 0
  }).format(value)
}

// Watchers
watch(filterStatus, (newVal) => {
  saveFilterToStorage(newVal)
  pagination.value.page = 1
  fetchBarang()
}, {deep: true})

// Lifecycle
onMounted(() => {
  fetchBarang()
  fetchSupplier()
})
</script>

<style lang="sass" scoped>
</style>
