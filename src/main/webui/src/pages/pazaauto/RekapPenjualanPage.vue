<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="$q.screen.xs ? [100, 100] : [50, 100]"
                style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                      :on-create="openCreateDialog"
                      :on-edit="openEditDialog" create-label="Tambah data Karyawan" ref="tableRef"
                      search-placeholder="Search by name or email...">
          <template v-slot:toolbar-filters>
            <div class="row items-center q-gutter-sm">
              <q-input :model-value="dateRangeText" label="Date Range" outlined dense readonly>
                <template v-slot:append>
                  <q-icon name="event" class="cursor-pointer">
                    <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                      <q-date v-model="dateRange" range>
                        <div class="row items-center justify-end q-gutter-sm">
                          <q-btn label="Clear" color="primary" flat @click="clearDateRange"/>
                          <q-btn label="OK" color="primary" flat v-close-popup/>
                        </div>
                      </q-date>
                    </q-popup-proxy>
                  </q-icon>
                </template>
              </q-input>
            </div>
            <q-btn :icon="showDetail ? 'chevron_left' : 'chevron_right'" @click="showDetailForm()" class="q-ml-sm">
              <q-tooltip>
                {{ showDetail ? 'Sembunyikan detail' : 'Tampilkan lebih rinci' }}
              </q-tooltip>
            </q-btn>
          </template>
          <template v-slot:body-cell-statusSpk="props">
            <q-badge :color="getStatusColor(props.row.statusSpk)">
              {{ props.row.statusSpk || 'N/A' }}
            </q-badge>
          </template>

          <template v-slot:body-cell-startedAt="props">
            {{ formatDateTime(props.row.startedAt) }}
          </template>
          <template v-slot:body-cell-finishedAt="props">
            {{ formatDateTime(props.row.finishedAt) }}
          </template>

          <template v-slot:body-cell-uangDibayar="props">
            {{ formatCurrency(props.row.uangDibayar) }}
          </template>

          <template v-slot:body-cell-kembalian="props">
            {{ formatCurrency(props.row.kembalian) }}
          </template>

          <template v-slot:body-cell-grandTotal="props">
            {{ formatCurrency(props.row.grandTotal) }}
          </template>

        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6 q-mb-md">Detail SPK dan Penjualan</div>
            <q-space/>
          </div>
          <q-form @submit="handleSave" id="karyawan-form" class="q-gutter-md">
            <q-card flat bordered>
              <q-card-section class="row" horizontal>
                <q-card-section class="col-6 q-col-gutter-md">
                  <div>
                    <span class="text-caption text-bold">Informasi SPK</span>
                  </div>
                  <q-input v-model="formData.tanggalJamSpk" label="Tanggal" outlined dense
                           placeholder="YYYY-MM-DD HH:mm:ss"
                           readonly/>
                  <q-input v-model="formData.noSpk" label="No SPK" outlined dense readonly/>
                  <q-input v-model.number="formData.noAntrian" label="No Antrian" outlined dense type="number"
                           readonly/>

                  <q-input v-model.number="formData.nopol" label="No Polisi *" outlined dense
                           readonly/>

                  <q-select v-model="selectedMekaniks" label="Select Mechanics" outlined dense multiple
                            :options="karyawanOptions" option-label="namaKaryawan" option-value="id" use-chips use-input
                            input-debounce="300" @filter="filterKaryawan" :loading="loadingKaryawan"
                            readonly
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
                                    :options="['Utama', 'Pembantu']"
                                    dense
                                    outlined style="min-width: 120px"/>
                        </q-item-section>
                      </q-item>
                    </template>
                  </q-select>

                  <q-input v-model.number="formData.km" label="KM" outlined dense type="number"
                           :rules="[val => !!val || 'KM harus diisi number']"
                           hide-bottom-space readonly/>
                  <q-input v-model="formData.keterangan" label="Keterangan" outlined dense type="textarea" rows="2"
                           autogrow readonly/>
                </q-card-section>
                <q-card-section>
                  <SPKCustomerInfo
                    v-model:namaPelanggan="formData.namaPelanggan"
                    v-model:alamat="formData.alamat"
                    v-model:merk="formData.merk"
                    v-model:jenis="formData.jenis"
                    :nopol="formData.nopol"
                    :isNewCustomer="isNewCustomer"
                  />
                </q-card-section>
              </q-card-section>
            </q-card>


            <!-- Split Layout for Jasa and Barang -->
            <SPKDetailsEditor
              v-model:details="formData.details"
              :allJasaOptions="allJasaOptions"
              :allBarangOptions="allBarangOptions"
              :canEdit="false"
              :noSpk="formData.noSpk"
              @update-master-jasa="handleUpdateMasterJasa"
              @update-master-barang="handleUpdateMasterBarang"
            />

            <!-- Penjualan Information Section -->
            <q-card flat bordered class="q-mt-md">
              <q-card-section>
                <div class="text-caption text-bold q-mb-md">Informasi Penjualan</div>
                <div class="row q-col-gutter-md">
                  <div class="col-12 col-md-6">
                    <q-input v-model="formData.noPenjualan" label="No Faktur Penjualan" outlined dense readonly/>
                  </div>
                  <div class="col-12 col-md-6">
                    <q-field label="Tanggal Penjualan" outlined dense stack-label>
                      <template v-slot:control>
                        <div class="self-center full-width no-outline" tabindex="0">
                          {{ formatDateTime(formData.tanggalJamPenjualan) }}
                        </div>
                      </template>
                    </q-field>
                  </div>
                  <div class="col-12 col-md-6">
                    <q-field label="Total Tagihan" outlined dense stack-label>
                      <template v-slot:control>
                        <div class="self-center full-width no-outline" tabindex="0">{{ formatCurrency(formData.grandTotal) }}</div>
                      </template>
                    </q-field>
                  </div>
                  <div class="col-12 col-md-6">
                    <q-field label="Uang Dibayar" outlined dense stack-label>
                      <template v-slot:control>
                        <div class="self-center full-width no-outline" tabindex="0">{{ formatCurrency(formData.uangDibayar) }}</div>
                      </template>
                    </q-field>
                  </div>
                  <div class="col-12 col-md-6">
                    <q-field label="Kembalian" outlined dense stack-label>
                      <template v-slot:control>
                        <div class="self-center full-width no-outline" tabindex="0">{{ formatCurrency(formData.kembalian) }}</div>
                      </template>
                    </q-field>
                  </div>
                  <div class="col-12 col-md-6">
                    <q-input v-model="formData.metodePembayaran" label="Metode Pembayaran" outlined dense readonly/>
                  </div>
                  <div class="col-12 col-md-6">
                    <q-input v-model="formData.statusPembayaran" label="Status Pembayaran" outlined dense readonly>
                      <template v-slot:append>
                        <q-badge :color="getPaymentStatusColor(formData.statusPembayaran)">
                          {{ formData.statusPembayaran || 'N/A' }}
                        </q-badge>
                      </template>
                    </q-input>
                  </div>
                </div>
              </q-card-section>
            </q-card>

            <div class="row justify-end q-gutter-sm">
              <div v-if="formData.statusSpk === 'SELESAI'">
                <q-btn label="Print" type="button" @click="printSpk" style="width: 100px;"
                       :loading="saving" class="q-mr-sm"/>
              </div>
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Print Preview Dialog -->
    <GenericDialog v-model="showPrintDialog" title="Print Preview" min-width="800px" max-width="90vw">
      <div class="q-pa-sm" style="height: 70vh; width: 100%;">
        <iframe :srcdoc="printPreviewContent" style="width: 100%; height: 100%; border: 1px solid #ccc;"></iframe>
      </div>
      <template #actions>
        <q-btn flat label="Tutup" color="primary" @click="showPrintDialog = false"/>
        <q-btn label="Print" icon="print" color="secondary" @click="confirmPrint"/>
      </template>
    </GenericDialog>

    <!-- Payment Dialog -->
    <GenericDialog v-model="showPaymentDialog" title="Konfirmasi Pembayaran" min-width="500px">
      <div class="q-pa-md">
        <div class="row q-col-gutter-md">
          <div class="col-12">
            <q-input v-model="paymentData.noPenjualan" label="No Penjualan" outlined dense readonly/>
          </div>
          <div class="col-12">
            <q-field label="Total Tagihan" outlined dense stack-label>
              <template v-slot:control>
                <div class="self-center full-width no-outline" tabindex="0">{{ formatCurrency(grandTotal) }}</div>
              </template>
            </q-field>
          </div>
          <div class="col-12">
            <q-select v-model="paymentData.metodePembayaran" :options="['CASH', 'TRANSFER', 'DEBIT', 'KREDIT']"
                      label="Metode Pembayaran" outlined dense/>
          </div>
          <div class="col-12">
            <q-input v-model.number="paymentData.uangDibayar" label="Uang Dibayar" outlined dense type="number"
                     prefix="Rp" @update:model-value="calculateKembalian" autofocus
                     :rules="[val => val >= grandTotal || 'Uang dibayar kurang dari total tagihan']"/>
          </div>
          <div class="col-12">
            <q-input v-model="paymentData.kembalian" label="Kembalian" outlined dense readonly
                     :model-value="formatCurrency(paymentData.kembalian)" stack-label/>
          </div>
        </div>
      </div>
      <template #actions>
        <q-btn flat label="Batal" color="primary" @click="showPaymentDialog = false"/>
        <q-btn label="Konfirmasi & Selesai" color="green" @click="confirmPayment" :loading="saving"/>
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import {ref, onMounted, watch, computed, nextTick} from 'vue'
import {api} from 'boot/axios'
import {useQuasar, date} from 'quasar'
import GenericDialog from 'components/GenericDialog.vue'
import {useKeyboardShortcuts} from 'src/composables/useKeyboardShortcuts'
import GenericTable from "components/GenericTable.vue";
import SPKDetailsEditor from 'components/SPKDetailsEditor.vue'
import SPKCustomerInfo from 'components/SPKCustomerInfo.vue'
import fakturTemplate from 'assets/template/faktur.template?raw'

const $q = useQuasar()

// LocalStorage key for filter persistence
const FILTER_STORAGE_KEY = 'spk_status_filter'

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

// State
const splitterModel = $q.screen.xs ? ref(100) : ref(100)
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const loadingPelanggan = ref(false)
const loadingKaryawan = ref(false)
const isNewCustomer = ref(false)
const searchText = ref('')
const filterStatus = ref(loadFilterFromStorage())
const filterToday = ref(false)
const todayVal = new Date()
const yesterdayVal = date.subtractFromDate(todayVal, {days: 1})
const dateRange = ref({
  from: date.formatDate(yesterdayVal, 'YYYY/MM/DD'),
  to: date.formatDate(todayVal, 'YYYY/MM/DD')
})
const showDetail = ref(false)

// Computed property for date range display text
const dateRangeText = computed(() => {
  if (!dateRange.value || (!dateRange.value.from && !dateRange.value.to)) {
    return ''
  }
  if (dateRange.value.from && dateRange.value.to) {
    return `${dateRange.value.from} - ${dateRange.value.to}`
  }
  if (dateRange.value.from) {
    return `From: ${dateRange.value.from}`
  }
  if (dateRange.value.to) {
    return `To: ${dateRange.value.to}`
  }
  return ''
})
const rows = ref([])
const pelangganOptions = ref([])
const filteredPelangganOptions = ref([])
const karyawanOptions = ref([])
const selectedMekaniks = ref([])  // Array of { id, namaKaryawan, tugas }
const showDialog = ref(false)
const showDeleteDialog = ref(false)
const showPaymentDialog = ref(false)
const isEditMode = ref(false)
const itemToDelete = ref(null)
const isEditable = ref(false)
const initialData = ref(null)
const tableRef = ref(null)
const showPrintDialog = ref(false)
const printPreviewContent = ref('')
const isDirty = (current) => {
  if (!initialData.value) return true
  return JSON.stringify(current) !== JSON.stringify(initialData.value)
}

// Payment dialog state
const paymentData = ref({
  uangDibayar: 0,
  kembalian: 0,
  metodePembayaran: 'CASH'
})

// Detail SPK State
const allJasaOptions = ref([])
const allBarangOptions = ref([])

// Computed
const jasaRows = computed(() => {
  if (!formData.value.details) return []
  return formData.value.details
    .filter(d => d.jasaId)
    .map(d => {
      const jasa = allJasaOptions.value.find(j => j.id === d.jasaId)
      const harga = d.harga !== undefined && d.harga !== null ? d.harga : (jasa ? jasa.hargaJasa : 0)
      const namaItem = jasa ? jasa.namaJasa : d.namaItem || 'Unknown Service'
      return {...d, harga, namaItem}
    })
})

const barangRows = computed(() => {
  if (!formData.value.details) return []
  return formData.value.details
    .filter(d => d.sparepartId)
    .map(d => {
      const barang = allBarangOptions.value.find(b => b.id === d.sparepartId)
      const harga = d.harga !== undefined && d.harga !== null ? d.harga : (barang ? barang.hargaJual : 0)
      const namaItem = barang ? barang.namaBarang : d.namaItem || 'Unknown Part'
      return {...d, harga, namaItem}
    })
})

const subtotalJasa = computed(() => {
  return jasaRows.value.reduce((sum, row) => sum + (row.harga * row.jumlah), 0)
})

const subtotalBarang = computed(() => {
  return barangRows.value.reduce((sum, row) => sum + (row.harga * row.jumlah), 0)
})

const grandTotal = computed(() => subtotalJasa.value + subtotalBarang.value)

const pagination = ref({
  sortBy: "noSpk",
  descending: true,
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0
})

// Form data
const formData = ref({
  id: null,
  noSpk: '',
  noAntrian: null,
  tanggalJamSpk: '',
  nopol: '',
  namaKaryawan: '',
  km: null,
  statusSpk: 'SELESAI',
  diskon: null,
  keluhan: '',
  keterangan: '',
  kmSaatIni: null,
  ppn: null,
  status: '',
  csId: null,
  mekanikId: null,
  mekanikList: null,
  // Customer info fields
  namaPelanggan: '',
  alamat: '',
  merk: '',
  jenis: '',
  // Details
  details: [],
  startProcess: false
})

const formatDateTime = (value) => {
  if (!value) return ''
  return date.formatDate(value, 'YYYY-MM-DD HH:mm')
}

// Table columns
const columns = [
  {
    name: 'statusSpk',
    label: 'Status',
    align: 'center',
    field: 'statusSpk',
    sortable: true
  },
  {
    name: 'noSpk',
    required: true,
    label: 'No SPK',
    align: 'left',
    field: 'noSpk',
    sortable: true
  },
  {
    name: 'noAntrian',
    label: 'No Antrian',
    align: 'center',
    field: 'noAntrian',
    sortable: true
  },
  {
    name: 'namaPelanggan',
    label: 'Pelanggan',
    align: 'left',
    field: 'namaPelanggan',
    sortable: true
  },
  {
    name: 'nopol',
    label: 'Nopol',
    align: 'left',
    field: 'nopol',
    sortable: true
  },
  {
    name: 'km',
    label: 'KM',
    align: 'center',
    field: 'km',
    sortable: true
  },
  {
    name: 'tanggalJamSpk',
    label: 'Tanggal/Jam',
    align: 'left',
    field: 'tanggalJamSpk',
    sortable: true
  },
  {
    name: 'startedAt',
    label: 'Mulai',
    align: 'left',
    field: 'startedAt',
    sortable: true
  },
  {
    name: 'finishedAt',
    label: 'Selesai',
    align: 'left',
    field: 'finishedAt',
    sortable: true
  },
  {
    name: 'namaKaryawan',
    label: 'Mekanik',
    align: 'left',
    field: 'namaKaryawan',
    sortable: true
  },
  {
    name: 'noPenjualan',
    required: true,
    label: 'No Faktur Penjualan',
    align: 'left',
    field: 'noPenjualan',
    sortable: true
  },
  {
    name: 'grandTotal',
    required: true,
    label: 'Total',
    align: 'left',
    field: 'grandTotal',
    sortable: true
  },
  {
    name: 'uangDibayar',
    label: 'Dibayar',
    align: 'left',
    field: 'uangDibayar',
    sortable: true
  },
  {
    name: 'kembalian',
    label: 'Kembalian',
    align: 'left',
    field: 'kembalian',
    sortable: true
  },
  {
    name: 'metodePembayaran',
    label: 'Metode pembayaran',
    align: 'left',
    field: 'metodePembayaran',
    sortable: true
  },
  {
    name: 'statusPembayaran',
    label: 'Status pembayaran',
    align: 'left',
    field: 'statusPembayaran',
    sortable: true
  }
]


const fetchSpk = async (paginationData = pagination.value) => {
  loading.value = true
  try {
    const params = {
      page: paginationData.page,
      rowsPerPage: paginationData.rowsPerPage
    }

    // Add sorting if specified
    if (paginationData.sortBy) {
      params.sortBy = paginationData.sortBy
      params.descending = paginationData.descending
    }

    // Add search if specified
    if (searchText.value) {
      params.search = searchText.value
    }

    // Add status filter - always filter for OPEN status
    params.statusFilter = 'SELESAI'

    // Add today filter if checked
    if (filterToday.value) {
      params.filterToday = true
    }

    // Add date range filter
    if (dateRange.value?.from) {
      // Quasar returns YYYY/MM/DD format by default. Backend needs YYYY-MM-DD.
      params.startDate = dateRange.value.from.replace(/\//g, '-')
    }
    if (dateRange.value?.to) {
      params.endDate = dateRange.value.to.replace(/\//g, '-')
    }

    const response = await api.get('/api/pazaauto/rekap-penjualan/paginated', {params})
    if (response.data.success) {
      const pageData = response.data.data
      rows.value = pageData.rows || []
      pagination.value.rowsNumber = pageData.rowsNumber
      pagination.value.page = pageData.page
      pagination.value.rowsPerPage = pageData.rowsPerPage
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to fetch SPK data',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    loading.value = false
  }
}

const fetchNextSpkNumber = async () => {
  try {
    const response = await api.get('/api/pazaauto/rekap-penjualan/get-next-spk-number')
    if (response.data.success) {
      formData.value.noSpk = response.data.data
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to fetch SPK data',
      caption: error.response?.data?.message || error.message
    })
  }
}

const fetchPelanggan = async () => {
  loadingPelanggan.value = true
  try {
    const response = await api.get('/api/pazaauto/pelanggan')
    if (response.data.success) {
      pelangganOptions.value = response.data.data || []
      filteredPelangganOptions.value = pelangganOptions.value
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to fetch pelanggan data',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    loadingPelanggan.value = false
  }
}

const onNopolChange = async (nopol) => {
  if (!nopol) {
    // Clear customer info fields
    formData.value.namaPelanggan = ''
    formData.value.alamat = ''
    formData.value.merk = ''
    formData.value.jenis = ''
    isNewCustomer.value = false
    return
  }

  // Check if nopol exists in pelangganOptions
  const existingPelanggan = pelangganOptions.value.find(p => p.nopol === nopol)

  if (existingPelanggan) {
    // Existing customer - fetch details and make fields readonly
    isNewCustomer.value = false
    try {
      const response = await api.get(`/api/pazaauto/pelanggan/by-nopol/${nopol}`)
      if (response.data.success && response.data.data) {
        const pelanggan = response.data.data
        formData.value.namaPelanggan = pelanggan.namaPelanggan || ''
        formData.value.alamat = pelanggan.alamat || ''
        formData.value.merk = pelanggan.merk || ''
        formData.value.jenis = pelanggan.jenis || ''
      }
    } catch (error) {
      $q.notify({
        type: 'negative',
        message: 'Failed to fetch pelanggan details',
        caption: error.response?.data?.message || error.message
      })
    }
  } else {
    // New customer - clear fields and make them editable
    isNewCustomer.value = true
    formData.value.namaPelanggan = ''
    formData.value.alamat = ''
    formData.value.merk = ''
    formData.value.jenis = ''
    $q.notify({
      type: 'info',
      message: 'New customer - Please fill in customer details',
      timeout: 2000
    })
  }
}

const fetchKaryawan = async () => {
  loadingKaryawan.value = true
  try {
    const response = await api.get('/api/pazaauto/karyawan')
    if (response.data.success) {
      karyawanOptions.value = response.data.data || []
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to fetch karyawan data',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    loadingKaryawan.value = false
  }
}

const filterKaryawan = (val, update) => {
  update(() => {
    if (!val) {
      // Don't filter, let q-select handle it
      return
    }
  })
}

const isSelected = (id) => {
  return selectedMekaniks.value.some(m => m.id === id)
}

const toggleMechanic = (mechanic) => {
  const index = selectedMekaniks.value.findIndex(m => m.id === mechanic.id)
  if (index > -1) {
    // Remove
    selectedMekaniks.value.splice(index, 1)
  } else {
    // Add with default tugas
    selectedMekaniks.value.push({
      id: mechanic.id,
      namaKaryawan: mechanic.namaKaryawan,
      tugas: 'Utama'
    })
  }
}

const getMekanikTugas = (id) => {
  const mechanic = selectedMekaniks.value.find(m => m.id === id)
  return mechanic?.tugas || 'Utama'
}

const setMekanikTugas = (id, tugas) => {
  const mechanic = selectedMekaniks.value.find(m => m.id === id)
  if (mechanic) {
    mechanic.tugas = tugas
  }
}

const showDetailForm = () => {
  showDetail.value = !showDetail.value
  splitterModel.value = showDetail.value ? 50 : 100
}

const onRequest = (props) => {
  const {page, rowsPerPage, sortBy, descending} = props.pagination
  pagination.value.page = page
  pagination.value.rowsPerPage = rowsPerPage
  pagination.value.sortBy = sortBy
  pagination.value.descending = descending
  fetchSpk(pagination.value)
}

const onSearch = (val) => {
  searchText.value = val
  pagination.value.page = 1
  fetchSpk()
}

const clearDateRange = () => {
  dateRange.value = {from: '', to: ''}
}

const openCreateDialog = async () => {
  isEditMode.value = false
  isEditable.value = false
  resetForm()
  initNoSpk()
  initialData.value = null
  showDialog.value = true
  // Fetch options for inline add
  await Promise.all([fetchJasa(), fetchBarang()])
}

const openEditDialog = async (row) => {
  isEditMode.value = true

  // Fetch options first so we can map prices
  await Promise.all([fetchJasa(), fetchBarang()])

  // Fetch full details including details list
  try {
    const response = await api.get(`/api/pazaauto/rekap-penjualan/${row.id}`)
    if (response.data.success) {
      formData.value = response.data.data
      // Ensure details is an array
      if (!formData.value.details) {
        formData.value.details = []
      }
    } else {
      formData.value = {...row, details: []}
    }
  } catch (error) {
    console.error('Failed to fetch SPK details', error)
    formData.value = {...row, details: []}
  }

  onNopolChange(formData.value.nopol)

  // Parse mekanikList (now it's an array from backend)
  if (formData.value.mekanikList && Array.isArray(formData.value.mekanikList)) {
    selectedMekaniks.value = formData.value.mekanikList.map(m => {
      const karyawan = karyawanOptions.value.find(k => k.id === m.id)
      return {
        id: m.id,
        namaKaryawan: karyawan?.namaKaryawan || `ID: ${m.id}`,
        tugas: m.tugas
      }
    })
  } else {
    selectedMekaniks.value = []
  }

  isEditable.value = false
  initialData.value = JSON.parse(JSON.stringify(formData.value))
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
  isNewCustomer.value = false
  selectedMekaniks.value = []
  resetForm()
}

const resetForm = () => {
  formData.value = {
    id: null,
    noSpk: '',
    noAntrian: null,
    tanggalJamSpk: '',
    nopol: '',
    namaKaryawan: '',
    km: null,
    statusSpk: 'PROSES',
    diskon: null,
    keluhan: '',
    keterangan: '',
    kmSaatIni: null,
    ppn: null,
    status: '',
    csId: null,
    mekanikId: null,
    // Customer info fields
    namaPelanggan: '',
    alamat: '',
    merk: '',
    jenis: '',
    details: []
  }
}

const initNoSpk = () => {
  const offsetMs = 7 * 60 * 60 * 1000;
  const gmt7 = new Date(new Date().getTime() + offsetMs);
  const gmt7Iso = gmt7.toISOString().replace("T", " ").replace("Z", "").substring(0, 16);
  formData.value.tanggalJamSpk = gmt7Iso;
  fetchNextSpkNumber()
}

const printSpk = async () => {
  // Use currently open SPK data to construct the print DTO
  // Or fetch from backend if preferred, but since we have it here:
  const data = {
    noPenjualan: formData.value.noSpk, // For SPK we use noSpk as noPenjualan in template
    tanggal: formData.value.tanggalJamSpk,
    noSpk: formData.value.noSpk,
    namaPelanggan: formData.value.namaPelanggan,
    alamatPelanggan: formData.value.alamat,
    nopol: formData.value.nopol,
    merk: formData.value.merk,
    model: formData.value.jenis,
    km: formData.value.km,
    namaMekanik: selectedMekaniks.value.map(m => m.namaKaryawan).join(', '),
    subTotal: grandTotal.value,
    diskon: formData.value.diskon || 0,
    ppn: formData.value.ppn || 0,
    grandTotal: grandTotal.value,
    uangDibayar: 0,
    kembalian: 0,
    keterangan: formData.value.keterangan,
    items: [
      ...jasaRows.value.map(j => ({
        nama: j.namaItem,
        qty: j.jumlah,
        harga: j.harga,
        subTotal: j.harga * j.jumlah,
        type: 'JASA'
      })),
      ...barangRows.value.map(b => ({
        nama: b.namaItem,
        qty: b.jumlah,
        harga: b.harga,
        subTotal: b.harga * b.jumlah,
        type: 'BARANG'
      }))
    ]
  }

  // Render template
  const renderedContent = renderTemplate(fakturTemplate, {
    data,
    formatCurrency,
    formatNumber
  })

  printPreviewContent.value = renderedContent
  showPrintDialog.value = true
}

const confirmPrint = () => {
  let iframe = document.getElementById('print-iframe')
  if (iframe) {
    document.body.removeChild(iframe)
  }

  iframe = document.createElement('iframe')
  iframe.id = 'print-iframe'
  iframe.style.position = 'absolute'
  iframe.style.width = '0px'
  iframe.style.height = '0px'
  iframe.style.border = 'none'
  document.body.appendChild(iframe)

  const doc = iframe.contentWindow.document
  doc.open()
  doc.write(printPreviewContent.value)
  doc.close()

  // Give some time for the content to be parsed and images to load if any
  setTimeout(() => {
    iframe.contentWindow.focus()
    iframe.contentWindow.print()
  }, 250)
}

const renderTemplate = (template, context) => {
  const keys = Object.keys(context)
  const values = Object.values(context)
  try {
    return new Function(...keys, `return \`${template}\`;`)(...values)
  } catch (e) {
    console.error('Template rendering error:', e)
    return 'Error rendering template'
  }
}

const handleSave = async () => {
  await saveSpk()
  // Re-fetch the full SPK (with details) and update the selected row in the table
  if (formData.value.id) {
    await openEditDialog(formData.value)
    nextTick(() => {
      tableRef.value?.selectRowByItem(formData.value)
    })
  }
}

const saveSpk = async () => {
  saving.value = true
  try {
    // If new customer, create pelanggan first
    if (isNewCustomer.value && !isEditMode.value) {
      // Validate required customer fields
      if (!formData.value.namaPelanggan || !formData.value.merk) {
        $q.notify({
          type: 'warning',
          message: 'Please fill in required customer fields (Nama, Merk)'
        })
        //saving.value = false
        return
      }

      // Tambah data new pelanggan
      const pelangganData = {
        nopol: formData.value.nopol,
        namaPelanggan: formData.value.namaPelanggan,
        alamat: formData.value.alamat,
        merk: formData.value.merk,
        jenis: formData.value.jenis
      }

      try {
        const pelangganResponse = await api.post('/api/pazaauto/pelanggan', pelangganData)
        if (!pelangganResponse.data.success) {
          throw new Error('Failed to create pelanggan')
        }
        $q.notify({
          type: 'positive',
          message: 'New customer created successfully'
        })
        // Refresh pelanggan list
        await fetchPelanggan()
        isNewCustomer.value = false
      } catch (error) {
        $q.notify({
          type: 'negative',
          message: 'Failed to create new customer',
          caption: error.response?.data?.message || error.message
        })
        saving.value = false
        return
      }
    }

    // Convert selectedMekaniks to List (Array)
    if (selectedMekaniks.value.length > 0) {
      formData.value.mekanikList = selectedMekaniks.value.map(m => ({id: m.id, tugas: m.tugas || 'Utama'}))
    } else {
      formData.value.mekanikList = []
    }

    // Proceed with SPK creation/update
    let response
    if (isEditMode.value) {
      response = await api.put(`/api/pazaauto/rekap-penjualan/${formData.value.id}`, formData.value)
    } else {
      response = await api.post('/api/pazaauto/rekap-penjualan', formData.value)
    }

    if (response.data.success) {
      const result = response.data.data
      $q.notify({
        type: 'positive',
        message: isEditMode.value ? 'SPK updated successfully' : 'SPK created successfully'
      })
      await fetchSpk()
      if (result) {
        formData.value = {
          ...result,
          details: result.details || []
        }
        initialData.value = JSON.parse(JSON.stringify(formData.value))
        if (!isEditMode.value) {
          isEditMode.value = true
        }
      }
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to save SPK',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    saving.value = false
  }
}

const calculateKembalian = () => {
  paymentData.value.kembalian = paymentData.value.uangDibayar - grandTotal.value
}

const determinePaymentStatus = () => {
  if (paymentData.value.uangDibayar >= grandTotal.value) {
    return 'LUNAS'
  } else if (paymentData.value.uangDibayar > 0) {
    return 'DP'
  } else {
    return 'BELUM_LUNAS'
  }
}

const confirmPayment = async () => {
  saving.value = true
  try {
    // Validate payment amount
    if (paymentData.value.uangDibayar <= 0) {
      $q.notify({
        type: 'warning',
        message: 'Please enter a valid payment amount'
      })
      saving.value = false
      return
    }

    // Calculate kembalian
    calculateKembalian()

    // Prepare penjualan data
    const penjualanData = {
      noPenjualan: paymentData.value.noPenjualan,
      tanggalJamPenjualan: new Date().toISOString(),
      noSpk: formData.value.noSpk,
      grandTotal: grandTotal.value,
      uangDibayar: paymentData.value.uangDibayar,
      kembalian: paymentData.value.kembalian,
      metodePembayaran: paymentData.value.metodePembayaran,
      statusPembayaran: determinePaymentStatus(),
      keterangan: `Payment for SPK ${formData.value.noSpk}`,
      kendaraanId: formData.value.idKendaraan,
      pelangganId: formData.value.idPelanggan,
      karyawanId: formData.value.idKaryawan
    }

    // Tambah data penjualan
    const penjualanResponse = await api.post('/api/pazaauto/penjualan', penjualanData)

    if (!penjualanResponse.data.success) {
      throw new Error('Failed to create penjualan')
    }

    // Tambah data penjualan details
    const detailPromises = []

    // Add jasa details
    jasaRows.value.forEach((row) => {
      detailPromises.push(api.post('/api/pazaauto/penjualan-detail', {
        noPenjualan: paymentData.value.noPenjualan,
        namaJasaBarang: row.namaItem,
        kategori: 'JASA',
        jasaId: row.jasaId,
        hargaJual: row.harga,
        kuantiti: row.jumlah,
        total: row.harga * row.jumlah,
        keterangan: row.namaItem
      }))
    })

    // Add barang details
    barangRows.value.forEach((row) => {
      detailPromises.push(api.post('/api/pazaauto/penjualan-detail', {
        noPenjualan: paymentData.value.noPenjualan,
        namaJasaBarang: row.namaItem,
        kategori: 'SPAREPART',
        sparepartId: row.sparepartId,
        hargaJual: row.harga,
        kuantiti: row.jumlah,
        total: row.harga * row.jumlah,
        keterangan: row.namaItem
      }))
    })

    // Save all details in parallel
    await Promise.all(detailPromises)

    // Update SPK status to SELESAI
    formData.value.statusSpk = 'SELESAI'
    formData.value.finishedAt = new Date().toISOString()

    // Save SPK
    const spkResponse = await api.put(`/api/pazaauto/rekap-penjualan/${formData.value.id}`, formData.value)

    if (spkResponse.data.success) {
      $q.notify({
        type: 'positive',
        message: 'Payment processed successfully',
        caption: `Invoice ${paymentData.value.noPenjualan} created`
      })

      // Close dialogs
      showPaymentDialog.value = false
      closeDialog()

      // Refresh SPK list
      await fetchSpk()
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to process payment',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    saving.value = false
  }
}

// Keyboard Shortcuts
useKeyboardShortcuts({
  onSave: () => {
    if (!saving.value && formData.value.statusSpk !== 'SELESAI' && formData.value.statusSpk !== 'BATAL') {
      if (!isEditMode.value || isDirty(formData.value)) {
        saveSpk()
      }
    } else if (showPaymentDialog.value && !saving.value) {
      confirmPayment()
    }
  },
  onDelete: () => {
    if (isEditMode.value && !deleting.value && formData.value.statusSpk !== 'SELESAI' && formData.value.statusSpk !== 'BATAL') {
      confirmDelete(formData.value)
    }
  },
  onNew: () => {
    openCreateDialog()
  }
})

const confirmDelete = (row) => {
  itemToDelete.value = row
  showDeleteDialog.value = true
}

const formatCurrency = (value) => {
  if (!value) return 'Rp 0'
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    minimumFractionDigits: 0
  }).format(value)
}

const formatNumber = (value) => {
  if (!value) return '0'
  return new Intl.NumberFormat('id-ID', {
    minimumFractionDigits: 0
  }).format(value)
}

const getStatusColor = (status) => {
  if (!status) return 'grey'
  const statusLower = status.toLowerCase()
  if (statusLower.includes('selesai') || statusLower.includes('complete')) return 'green'
  if (statusLower.includes('proses') || statusLower.includes('progress')) return 'orange'
  if (statusLower.includes('batal') || statusLower.includes('cancel')) return 'red'
  return 'blue'
}

const getPaymentStatusColor = (status) => {
  if (!status) return 'grey'
  const statusLower = status.toLowerCase()
  if (statusLower.includes('lunas')) return 'green'
  if (statusLower.includes('dp')) return 'orange'
  if (statusLower.includes('belum') || statusLower.includes('pending')) return 'red'
  return 'blue'
}

// Detail SPK Methods
const fetchJasa = async () => {
  try {
    const response = await api.get('/api/pazaauto/jasa')
    if (response.data.success) {
      allJasaOptions.value = response.data.data || []
    }
  } catch (error) {
    console.error('Failed to fetch jasa', error)
  }
}

const fetchBarang = async () => {
  try {
    const response = await api.get('/api/pazaauto/barang')
    if (response.data.success) {
      allBarangOptions.value = response.data.data || []
    }
  } catch (error) {
    console.error('Failed to fetch barang', error)
  }
}

const handleUpdateMasterJasa = async (payload) => {
  try {
    const response = await api.put(`/api/pazaauto/jasa/${payload.id}`, payload)
    if (response.data.success) {
      $q.notify({
        type: 'positive',
        message: 'Master data Jasa updated successfully'
      })
      await fetchJasa()
    }
  } catch (error) {
    console.error('Failed to update master jasa', error)
    $q.notify({
      type: 'negative',
      message: 'Failed to update master data Jasa',
      caption: error.response?.data?.message || error.message
    })
  }
}

const handleUpdateMasterBarang = async (payload) => {
  try {
    const response = await api.put(`/api/pazaauto/barang/${payload.id}`, payload)
    if (response.data.success) {
      $q.notify({
        type: 'positive',
        message: 'Master data Barang updated successfully'
      })
      await fetchBarang()
    }
  } catch (error) {
    console.error('Failed to update master barang', error)
    $q.notify({
      type: 'negative',
      message: 'Failed to update master data Barang',
      caption: error.response?.data?.message || error.message
    })
  }
}

// Watchers
let searchTimeout = null
watch(searchText, (newVal) => {
  console.log('searchText changed to:', newVal)
  // Debounce search to avoid too many API calls
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    // Reset to page 1 when searching
    pagination.value.page = 1
    fetchSpk()
  }, 500)
})

// Watch filter status changes
watch(filterStatus, (newVal) => {
  console.log('filterStatus changed to:', newVal)
  // Save to localStorage
  saveFilterToStorage(newVal)
  // Reset to page 1 when filtering
  pagination.value.page = 1
  fetchSpk()
})

// Watch today filter changes
// Watch date range changes
watch(dateRange, () => {
  pagination.value.page = 1
  fetchSpk()
}, {deep: true})

watch(filterToday, (newVal) => {
  console.log('filterToday changed to:', newVal)
  // Reset to page 1 when filtering
  pagination.value.page = 1
  fetchSpk()
})

// Lifecycle
onMounted(() => {
  fetchSpk()
  fetchPelanggan()
  fetchKaryawan()
  openCreateDialog()
})
</script>

<style lang="sass" scoped>
.dialog-spk
  min-width: calc(80vw - 48px)

.my-sticky-header-table
  max-height: 70vh

  thead tr th
    position: sticky
    z-index: 1
    background-color: #ffffff

  thead tr:first-child th
    top: 0

.cursor-pointer-rows
  :deep(tbody tr)
    cursor: pointer
</style>
