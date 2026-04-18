<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                      :on-create="openCreateDialog" :on-edit="openEditDialog" create-label="Tambah Pembelian"
                      row-key="noPembelian" ref="tableRef"
                      search-placeholder="Search by No Pembelian..."
                      dense
                      footerButtonLabel="Print"
                      :footerButtonAction="printTable"
                      >

          <template v-slot:toolbar-filters>
            <div class="row items-center q-gutter-sm">
              <q-select v-model="filterJenisPembelian" :options="jenisPembelianOptions" label="Jenis Pembelian"
                        dense options-dense flat outlined clearable style="min-width: 150px"/>
              <q-select v-model="filterKategoriOperasional" :options="kategoriOperasionalOptions"
                        label="Kategori Operasional" dense options-dense flat outlined clearable
                        style="min-width: 150px"/>
              <q-select v-model="filterStatus" multiple :options="statusOptions" label="Status Pembayaran"
                        dense options-dense flat outlined style="min-width: 150px"/>
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
          </template>

          <template v-slot:body-cell-grandTotal="props">
            {{ formatCurrency(props.row.grandTotal) }}
          </template>

          <template v-slot:body-cell-tanggalPembelian="props">
            {{ formatDateTime(props.row.tanggalPembelian) }}
          </template>

          <template v-slot:body-cell-statusPembayaran="props">
            <q-badge :color="getStatusColor(props.row.statusPembayaran)" style="width: 100px">
              {{ props.row.statusPembayaran }}
            </q-badge>
          </template>

          <template v-slot:body-cell-actions="props" v-if="isEditable">
              <q-btn flat dense round icon="delete" color="negative" @click.stop="confirmDelete(props.row)">
                <q-tooltip>Delete</q-tooltip>
              </q-btn>
          </template>
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-lg">
            <div class="text-h6">Detail Pengeluaran</div>
          </div>

          <q-form class="q-gutter-md" @submit="handleSave">
            <!-- Purchase Information Section -->
            <div class="q-mb-lg">
              <div class="text-subtitle text-weight-bold text-grey-8 q-mb-sm">Informasi Pembelian</div>
              <div class="row q-col-gutter-sm">
                <div class="col-12">
                  <q-input v-model="formData.noPembelian" label="No Pembelian" outlined dense
                           readonly/>
                </div>
                <div class="col-12">
                  <q-input v-model="formData.tanggalPembelian" label="Tanggal Pembelian" outlined
                           dense type="datetime-local" stack-label :readonly="!isEditable"
                           :rules="[val => !!val || 'Tanggal pembelian harus diisi']"
                           hide-bottom-space 
                  />
                </div>
                <div class="col-12">
                  <q-input v-model="formData.jenisPembelian" :options="jenisPembelianRadioOptions"
                            label="Jenis Pembelian" outlined dense :disable="!isEditable"
                            option-label="label" option-value="value" emit-value map-options
                            :rules="[val => !!val || 'Jenis pembelian harus diisi']"
                            hide-bottom-space readonly/>
                </div>
              </div>
            </div>

            <!-- Operational Expense Fields -->
            <div v-if="formData.jenisPembelian === 'OPERASIONAL'" class="q-mb-lg">
              <div class="text-subtitle text-weight-bold text-grey-8 q-mb-sm">Informasi Operasional</div>
              <div class="row q-col-gutter-sm">
                <div class="col-6">
                  <q-input v-model="formData.jenisOperasional" label="Jenis Operasional" outlined dense
                           :readonly="!isEditable"
                           :rules="[val => formData.jenisPembelian === 'OPERASIONAL' ? !!val || 'Jenis operasional harus diisi' : true]"/>
                </div>
                <div class="col-6">
                  <q-input v-model="formData.kategoriOperasional" :options="kategoriOperasionalOptions"
                            label="Kategori Operasional" outlined dense :readonly="!isEditable"
                            :rules="[val => formData.jenisPembelian === 'OPERASIONAL' ? !!val || 'Kategori operasional harus diisi' : true]"/>
                </div>
              </div>
            </div>

            <!-- Supplier Field -->
            <div v-if="formData.jenisPembelian === 'SPAREPART'" class="q-mb-lg">
              <div class="text-subtitle text-weight-bold text-grey-8 q-mb-sm">Informasi Supplier</div>
              <div class="row q-col-gutter-sm">
                <div class="col-12">
                  <q-select
                    v-model="formData.supplierId"
                    :options="filteredSupplierOptions"
                    label="Supplier"
                    outlined
                    dense
                    :disable="!isEditable"
                    option-label="namaSupplier"
                    option-value="id"
                    emit-value
                    map-options
                    use-input
                    input-debounce="300"
                    @filter="filterSuppliers"
                    @input-value="onSupplierInput"
                    :rules="[val => formData.jenisPembelian === 'SPAREPART' ? !!val || 'Supplier harus diisi' : true]"
                    hide-bottom-space
                    readonly
                  >
                    <template v-slot:no-option>
                      <q-item>
                        <q-item-section class="text-grey-6">
                          No results
                        </q-item-section>
                      </q-item>
                      <q-item clickable @click="openSupplierDialog" v-if="supplierSearchText">
                        <q-item-section>
                          <div class="text-primary">
                            <q-icon name="add" class="q-mr-sm"/>
                            Tambah supplier baru: "{{ supplierSearchText }}"
                          </div>
                        </q-item-section>
                      </q-item>
                    </template>
                  </q-select>
                </div>
              </div>
            </div>
            <!-- Pembelian Details Table -->
            <div class="q-mb-lg">
              <div class="text-subtitle1 text-weight-bold text-grey-8 q-mb-sm">
                <div class="row items-center justify-between">
                  <div>Detail pembelian</div>
                  <q-btn
                    v-if="isEditable"
                    color="primary"
                    icon="add"
                    dense
                    @click="openDetailDialog"
                    :disable="!isEditable"
                  >
                    <q-tooltip>Tambah item pembelian</q-tooltip>
                  </q-btn>
                </div>
              </div>

              <q-table
                :rows="formData.details"
                :columns="detailColumns"
                row-key="id"
                flat
                bordered
                dense
                :rows-per-page-options="[0]"
                hide-pagination
              >
                <template v-slot:body-cell-harga="props">
                  <q-td :props="props">
                    {{ formatCurrency(props.row.harga) }}
                  </q-td>
                </template>

                <template v-slot:body-cell-total="props">
                  <q-td :props="props">
                    {{ formatCurrency(props.row.total) }}
                  </q-td>
                </template>

                <template v-slot:body-cell-actions="props" v-if="isEditable">
                  <q-td :props="props" class="text-center">
                    <q-btn
                      flat
                      dense
                      round
                      icon="edit"
                      color="primary"
                      @click="editDetail(props.row)"
                      :disable="!isEditable"
                    >
                      <q-tooltip>Edit</q-tooltip>
                    </q-btn>
                    <q-btn
                      flat
                      dense
                      round
                      icon="delete"
                      color="negative"
                      @click="deleteDetail(props.row)"
                      :disable="!isEditable"
                    >
                      <q-tooltip>Delete</q-tooltip>
                    </q-btn>
                  </q-td>
                </template>

                <template v-slot:no-data>
                  <div class="full-width row flex-center text-grey-6 q-pa-md">
                    <div>Belum ada item pembelian</div>
                  </div>
                </template>
              </q-table>
            </div>

            <!-- Payment Details Section -->
            <div class="bg-grey-2 q-pa-md rounded-borders q-mb-lg">
              <div class="text-subtitle1 text-weight-bold text-grey-8 q-mb-sm">Payment Details</div>
              <div class="row q-col-gutter-sm">
                <div class="col-12">
                  <q-field label="Total" outlined dense stack-label>
                    <template v-slot:control>
                      <div class="self-center full-width no-outline" tabindex="0">{{ formatCurrency(grandTotal) }}</div>
                    </template>
                  </q-field>
                </div>
                <div class="col-12">
                  <q-select v-model="formData.statusPembayaran" :options="statusOptions"
                            label="Status Pembayaran" outlined dense :disable="!isEditable"
                            :rules="[val => !!val || 'Status pembayaran harus diisi']"/>
                </div>
                <div class="col-12">
                  <q-select v-model="formData.jenisPembayaran" :options="['CASH', 'TRANSFER', 'DEBIT', 'KREDIT']"
                            label="Metode Pembayaran" outlined dense :disable="!isEditable"
                            :rules="[val => !!val || 'Metode pembayaran harus diisi']"/>
                </div>
              </div>
            </div>

            <!-- Additional Notes -->
            <div>
              <div class="text-subtitle1 text-weight-bold text-grey-8 q-mb-sm">Informasi Tambahan</div>
              <q-input v-model="formData.keterangan" label="Keterangan" outlined dense type="textarea"
                       rows="3" :readonly="!isEditable"/>
            </div>

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode && isEditable" label="Hapus" color="negative" flat
                     @click="confirmDelete(formData)" :loading="deleting"/>
              <q-btn label="Simpan" type="submit" color="primary" :loading="saving" v-if="isEditable">
                <q-tooltip v-if="hasUnsavedChanges">
                  Ada perubahan yang belum disimpan
                </q-tooltip>
              </q-btn>
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" title="Konfirmasi hapus data" min-width="400px" position="standard">
      Apakah Anda yakin ingin menghapus data Pembelian <strong>{{ itemToDelete?.noPembelian }} ?</strong>?
      <template #actions>
        <q-btn flat label="Batalkan" color="primary" @click="showDeleteDialog = false"/>
        <q-btn flat label="Hapus saja" color="negative" @click="deletePembelian" :loading="deleting"/>
      </template>
    </GenericDialog>

    <!-- Pembelian Detail Dialog -->
    <q-dialog v-model="showDetailDialog" persistent>
      <q-card style="min-width: 500px">
        <q-card-section>
          <div class="text-h6">{{ isEditingDetail ? 'Edit Item' : 'Tambah Item' }}</div>
        </q-card-section>

        <q-separator/>

        <q-card-section class="q-pt-none">
          <q-form @submit="saveDetail" class="q-gutter-md">
            <div class="row q-col-gutter-sm">
              <div class="col-12">
                <q-input
                  v-model="detailForm.namaItem"
                  label="Nama Item*"
                  outlined
                  dense
                  :rules="[val => !!val || 'Nama item harus diisi']"
                  hide-bottom-space
                />
              </div>
              <div class="col-12">
                <q-select
                  v-model="detailForm.kategoriItem"
                  :options="kategoriItemOptions"
                  label="Kategori Item"
                  outlined
                  dense
                  option-label="label"
                  option-value="value"
                  emit-value
                  map-options
                  disabled
                  readonly
                />
              </div>
              <div class="col-6">
                <q-input
                  v-model="detailForm.harga"
                  label="Harga*"
                  outlined
                  dense
                  type="number"
                  step="0.01"
                  min="0"
                  :rules="[val => !!val && val > 0 || 'Harga harus diisi dan lebih dari 0']"
                  hide-bottom-space
                  @update:model-value="calculateDetailTotal"
                />
              </div>
              <div class="col-6">
                <q-input
                  v-model="detailForm.kuantiti"
                  label="Kuantiti*"
                  outlined
                  dense
                  type="number"
                  min="1"
                  :rules="[val => !!val && val > 0 || 'Kuantiti harus diisi dan lebih dari 0']"
                  hide-bottom-space
                  @update:model-value="calculateDetailTotal"
                />
              </div>
              <div class="col-12">
                <q-field label="Total" outlined dense stack-label>
                  <template v-slot:control>
                    <div class="self-center full-width no-outline" tabindex="0">
                      {{ formatCurrency(detailForm.total) }}
                    </div>
                  </template>
                </q-field>
              </div>
              <div class="col-12">
                <q-input
                  v-model="detailForm.keterangan"
                  label="Keterangan"
                  outlined
                  dense
                  type="textarea"
                  rows="2"
                />
              </div>
            </div>
          </q-form>
        </q-card-section>

        <q-separator/>

        <q-card-actions align="right">
          <q-btn flat label="Batal" color="primary" @click="closeDetailDialog"/>
          <q-btn flat label="Simpan" color="primary" @click="saveDetail"/>
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- New Supplier Dialog -->
    <q-dialog v-model="showSupplierDialog" persistent>
      <q-card style="min-width: 500px">
        <q-card-section>
          <div class="text-h6">Add New Supplier</div>
        </q-card-section>

        <q-separator/>

        <q-card-section class="q-pt-none">
          <q-form @submit="createNewSupplier" class="q-gutter-md">
            <div class="row q-col-gutter-sm">
              <div class="col-12">
                <q-input
                  v-model="newSupplierForm.namaSupplier"
                  label="Supplier Name*"
                  outlined
                  dense
                  :rules="[val => !!val || 'Supplier name is required']"
                  hide-bottom-space
                />
              </div>
              <div class="col-12">
                <q-input
                  v-model="newSupplierForm.alamat"
                  label="Address"
                  outlined
                  dense
                  type="textarea"
                  rows="2"
                />
              </div>
              <div class="col-6">
                <q-input
                  v-model="newSupplierForm.telepon"
                  label="Phone"
                  outlined
                  dense
                />
              </div>
              <div class="col-6">
                <q-input
                  v-model="newSupplierForm.email"
                  label="Email"
                  outlined
                  dense
                  type="email"
                />
              </div>
              <div class="col-6">
                <q-input
                  v-model="newSupplierForm.kontakPerson"
                  label="Contact Person"
                  outlined
                  dense
                />
              </div>
              <div class="col-6">
                <q-input
                  v-model="newSupplierForm.noHpKontak"
                  label="Contact Phone"
                  outlined
                  dense
                />
              </div>
              <div class="col-6">
                <q-input
                  v-model="newSupplierForm.kota"
                  label="City"
                  outlined
                  dense
                />
              </div>
              <div class="col-6">
                <q-input
                  v-model="newSupplierForm.kodePos"
                  label="Postal Code"
                  outlined
                  dense
                />
              </div>
              <div class="col-12">
                <q-input
                  v-model="newSupplierForm.keterangan"
                  label="Notes"
                  outlined
                  dense
                  type="textarea"
                  rows="2"
                />
              </div>
            </div>
          </q-form>
        </q-card-section>

        <q-separator/>

        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="primary" @click="closeSupplierDialog"/>
          <q-btn flat label="Save" color="primary" @click="createNewSupplier" :loading="savingSupplier"/>
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Print Preview Dialog -->
    <GenericDialog v-model="showPrintDialog" title="Print Preview" min-width="800px" max-width="90vw">
      <div class="q-pa-sm" style="height: 70vh; width: 100%;">
        <iframe :srcdoc="printPreviewContent" style="width: 100%; height: 100%; border: 1px solid #ccc;"></iframe>
      </div>
      <template #actions>
        <q-btn flat label="Tutup" color="primary" @click="showPrintDialog = false" />
        <q-btn label="Print" icon="print" color="secondary" @click="confirmPrint" />
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import {ref, onMounted, watch, nextTick, computed} from 'vue'
import {api} from 'boot/axios'
import {useQuasar, date} from 'quasar'
import { useDateFilter } from 'src/composables/useDateFilter'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import fakturTemplate from 'assets/template/rekap-pembelian.template?raw'

const $q = useQuasar()

// LocalStorage key for filter persistence
const FILTER_STORAGE_KEY = 'pembelian_status_filter'

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
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const searchText = ref('')
const filterJenisPembelian = ref(null)
const filterKategoriOperasional = ref(null)
const filterStatus = ref(loadFilterFromStorage())
const { dateRange, dateRangeText, clearDateRange } = useDateFilter('rekap-pembelian')

// Print dialog state
const showPrintDialog = ref(false)
const printPreviewContent = ref('')

const rows = ref([])
const showDeleteDialog = ref(false)
const isEditMode = ref(false)
const itemToDelete = ref(null)
const splitterModel = ref(70)
const tableRef = ref(null)
const supplierName = ref('')

// Options
const jenisPembelianOptions = ['SPAREPART', 'OPERASIONAL', 'BARANG']
const jenisPembelianRadioOptions = [
  {label: 'Pembelian Sparepart', value: 'SPAREPART'},
  {label: 'Pembelian Barang (Non-Supplier)', value: 'BARANG'},
  {label: 'Pengeluaran Operasional', value: 'OPERASIONAL'}
]
const kategoriOperasionalOptions = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', 'ON_DEMAND']
const statusOptions = ref(['LUNAS', 'BELUM_LUNAS', 'DP'])

const supplierOptions = ref([])
const filteredSupplierOptions = ref([])
const supplierSearchText = ref('')
const showSupplierDialog = ref(false)
const showDetailDialog = ref(false)
const isEditingDetail = ref(false)
const editingDetailIndex = ref(-1)
const newSupplierForm = ref({
  namaSupplier: '',
  alamat: '',
  telepon: '',
  email: '',
  kontakPerson: '',
  noHpKontak: '',
  kota: '',
  kodePos: '',
  keterangan: ''
})
const savingSupplier = ref(false)

// Detail form
const detailForm = ref({
  id: null,
  pembelianId: null,
  namaItem: '',
  kategoriItem: 'SPAREPART',
  harga: 0,
  kuantiti: 1,
  total: 0,
  keterangan: ''
})

const kategoriItemOptions = [
  {label: 'Sparepart', value: 'SPAREPART'},
  {label: 'Barang', value: 'BARANG'},
  {label: 'Operasional', value: 'OPERASIONAL'}
]

// Detail table columns
const detailColumns = [
  {
    name: 'namaItem',
    label: 'Nama Item',
    align: 'left',
    field: 'namaItem',
    sortable: false
  },
  {
    name: 'kategoriItem',
    label: 'Kategori',
    align: 'left',
    field: 'kategoriItem',
    sortable: false
  },
  {
    name: 'harga',
    label: 'Harga',
    align: 'right',
    field: 'harga',
    sortable: false
  },
  {
    name: 'kuantiti',
    label: 'Qty',
    align: 'center',
    field: 'kuantiti',
    sortable: false
  },
  {
    name: 'total',
    label: 'Total',
    align: 'right',
    field: 'total',
    sortable: false
  },
  {
    name: 'keterangan',
    label: 'Keterangan',
    align: 'left',
    field: 'keterangan',
    sortable: false
  },
  {
    name: 'actions',
    label: 'Actions',
    align: 'center',
    field: 'actions',
    sortable: false
  }
]

// Computed values
const grandTotal = computed(() => {
  if (!formData.value.details) return 0
  return formData.value.details.reduce((sum, detail) => sum + (detail.total || 0), 0)
})

const isEditable = computed(() => {
  return false // Allow editing in both create and edit modes
})

const pagination = ref({
  sortBy: 'tanggalPembelian',
  descending: true,
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0
})

// Form data
const formData = ref({
  id: null,
  noPembelian: '',
  tanggalPembelian: '',
  jenisPembelian: 'SPAREPART',
  jenisOperasional: '',
  kategoriOperasional: null,
  supplierId: null,
  grandTotal: 0,
  statusPembayaran: 'BELUM_LUNAS',
  jenisPembayaran: 'CASH',
  diskon: 0,
  ppn: 0,
  keterangan: '',
  details: []
})

// Original form data for change tracking
const originalFormData = ref({})

// Computed property to check for unsaved changes
const hasUnsavedChanges = computed(() => {
  if (!isEditMode.value) return false

  // Compare current form data with original
  const current = JSON.stringify(formData.value)
  const original = JSON.stringify(originalFormData.value)
  return current !== original
})

// Table columns
const columns = [

  {
    name: 'tanggalPembelian',
    label: 'Tanggal',
    align: 'left',
    field: 'tanggalPembelian',
    sortable: true
  },
  {
    name: 'noPembelian',
    required: true,
    label: 'No Pembelian',
    align: 'left',
    field: 'noPembelian',
    sortable: true
  },
  {
    name: 'jenisPembelian',
    label: 'Jenis Pengeluaran',
    align: 'center',
    field: 'jenisPembelian',
    sortable: true
  },
  {
    name: 'jenisOperasional',
    label: 'Jenis Operasional',
    align: 'left',
    field: 'jenisOperasional',
    sortable: true
  },
  {
    name: 'kategoriOperasional',
    label: 'Kategori Operasional',
    align: 'left',
    field: 'kategoriOperasional',
    sortable: true
  },

  {
    name: 'namaSupplier',
    label: 'Supplier',
    align: 'left',
    field: 'namaSupplier',
    sortable: true
  },
  {
    name: 'grandTotal',
    label: 'Jumlah Biaya',
    align: 'right',
    field: 'grandTotal',
    sortable: true
  },
  {
    name: 'jenisPembayaran',
    label: 'Jenis Pembayaran',
    align: 'center',
    field: 'jenisPembayaran',
    sortable: true
  },

  {
    name: 'keterangan',
    label: 'Keterangan',
    align: 'center',
    field: 'keterangan',
    sortable: true
  },

  {
    name: 'statusPembayaran',
    label: 'Status',
    align: 'center',
    field: 'statusPembayaran',
    sortable: true
  }
]

// Methods
const fetchPembelian = async (paginationData = pagination.value) => {
  loading.value = true
  try {
    const params = {
      page: paginationData.page,
      rowsPerPage: paginationData.rowsPerPage
    }

    if (paginationData.sortBy) {
      params.sortBy = paginationData.sortBy
      params.descending = paginationData.descending
    }

    if (searchText.value) {
      params.search = searchText.value
    }

    if (filterJenisPembelian.value) {
      params.jenisPembelian = filterJenisPembelian.value
    }

    if (filterKategoriOperasional.value) {
      params.kategoriOperasional = filterKategoriOperasional.value
    }

    if (filterStatus.value && filterStatus.value.length > 0) {
      params.statusFilter = filterStatus.value.join(',')
    }

    // Add date range filter
    if (dateRange.value?.from) {
      // Quasar returns YYYY/MM/DD format by default. Backend needs YYYY-MM-DD.
      params.startDate = dateRange.value.from.replace(/\//g, '-')
    }
    if (dateRange.value?.to) {
      params.endDate = dateRange.value.to.replace(/\//g, '-')
    }

    const response = await api.get('/api/pazaauto/pembelian/paginated', {params})
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
      message: 'Failed to fetch pembelian data',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    loading.value = false
  }
}

const onRequest = (props) => {
  const {page, rowsPerPage, sortBy, descending} = props.pagination
  pagination.value.page = page
  pagination.value.rowsPerPage = rowsPerPage
  pagination.value.sortBy = sortBy
  pagination.value.descending = descending
  fetchPembelian(pagination.value)
}

const onSearch = (val) => {
  searchText.value = val
  pagination.value.page = 1
  fetchPembelian()
}



const formatDateTime = (value) => {
  if (!value) return ''
  return date.formatDate(value, 'YYYY-MM-DD HH:mm')
}

const openCreateDialog = async () => {
  isEditMode.value = false
  resetForm()
  await fetchNextPembelianNumber()
  await fetchSuppliers()
  filteredSupplierOptions.value = supplierOptions.value
}

const openEditDialog = async (row) => {
  isEditMode.value = true

  // Fetch full pembelian details
  try {
    const response = await api.get(`/api/pazaauto/pembelian/${row.noPembelian}`)
    if (response.data.success) {
      formData.value = {...response.data.data}
      if (!formData.value.details) formData.value.details = []
      // Format datetime to YYYY-MM-DDThh:mm for datetime-local input
      if (formData.value.tanggalPembelian) {
        formData.value.tanggalPembelian = formData.value.tanggalPembelian.substring(0, 16)
      }

      // Set supplier name for display
      if (formData.value.supplierId && typeof formData.value.supplierId === 'object') {
        supplierName.value = formData.value.supplierId.namaSupplier
      }
    } else {
      formData.value = {...row, details: row.details || []}
      if (formData.value.tanggalPembelian) {
        formData.value.tanggalPembelian = formData.value.tanggalPembelian.substring(0, 16)
      }
    }
  } catch (error) {
    console.error('Failed to fetch pembelian details:', error)
    formData.value = {...row, details: row.details || []}
    if (formData.value.tanggalPembelian) {
      formData.value.tanggalPembelian = formData.value.tanggalPembelian.substring(0, 16)
    }
  }

  nextTick(() => {
    // Store original form data for change tracking
    originalFormData.value = JSON.parse(JSON.stringify(formData.value))
    tableRef.value?.selectRowByItem(row)
  })
}

const confirmDelete = (row) => {
  itemToDelete.value = row
  showDeleteDialog.value = true
}

const deletePembelian = async () => {
  deleting.value = true
  try {
    const response = await api.delete(`/api/pazaauto/pembelian/${itemToDelete.value.id}`)
    if (response.data.success) {
      $q.notify({
        type: 'positive',
        message: 'Pembelian deleted successfully'
      })
      showDeleteDialog.value = false
      itemToDelete.value = null
      await fetchPembelian()
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to delete pembelian',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    deleting.value = false
  }
}

const formatCurrency = (value) => {
  if (!value) return 'Rp 0'
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    minimumFractionDigits: 0
  }).format(value)
}

const getStatusColor = (status) => {
  switch (status) {
    case 'LUNAS':
      return 'green'
    case 'BELUM_LUNAS':
      return 'red'
    case 'DP':
      return 'orange'
    default:
      return 'grey'
  }
}

const resetForm = () => {
  formData.value = {
    id: null,
    noPembelian: '',
    tanggalPembelian: new Date().toISOString().slice(0, 16),
    jenisPembelian: 'SPAREPART',
    jenisOperasional: '',
    kategoriOperasional: null,
    supplierId: null,
    grandTotal: 0,
    statusPembayaran: 'BELUM_LUNAS',
    jenisPembayaran: 'CASH',
    diskon: 0,
    ppn: 0,
    keterangan: '',
    details: []
  }
}

const fetchNextPembelianNumber = async () => {
  try {
    const response = await api.get('/api/pazaauto/pembelian/get-next-number')
    if (response.data.success) {
      formData.value.noPembelian = response.data.data
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to fetch pembelian number',
      caption: error.response?.data?.message || error.message
    })
  }
}

const fetchSuppliers = async () => {
  try {
    const response = await api.get('/api/pazaauto/supplier')
    if (response.data.success) {
      supplierOptions.value = response.data.data || []
      filteredSupplierOptions.value = supplierOptions.value
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to fetch suppliers',
      caption: error.response?.data?.message || error.message
    })
  }
}

const filterSuppliers = (val, update) => {
  supplierSearchText.value = val
  update(() => {
    if (!val) {
      filteredSupplierOptions.value = supplierOptions.value
    } else {
      const needle = val.toLowerCase()
      filteredSupplierOptions.value = supplierOptions.value.filter(
        supplier => supplier.namaSupplier.toLowerCase().includes(needle)
      )
    }
  })
}

const onSupplierInput = (val) => {
  supplierSearchText.value = val
}

const openSupplierDialog = () => {
  newSupplierForm.value.namaSupplier = supplierSearchText.value
  showSupplierDialog.value = true
}

const closeSupplierDialog = () => {
  showSupplierDialog.value = false
  resetSupplierForm()
}

const resetSupplierForm = () => {
  newSupplierForm.value = {
    namaSupplier: '',
    alamat: '',
    telepon: '',
    email: '',
    kontakPerson: '',
    noHpKontak: '',
    kota: '',
    kodePos: '',
    keterangan: ''
  }
}

const createNewSupplier = async () => {
  savingSupplier.value = true
  try {
    const response = await api.post('/api/pazaauto/supplier', newSupplierForm.value)
    if (response.data.success) {
      $q.notify({
        type: 'positive',
        message: 'Supplier created successfully'
      })

      // Add new supplier to options
      const newSupplier = response.data.data
      supplierOptions.value.push(newSupplier)

      // Select the new supplier
      formData.value.supplierId = newSupplier.id

      closeSupplierDialog()
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to create supplier',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    savingSupplier.value = false
  }
}

// Detail management methods
const openDetailDialog = () => {
  isEditingDetail.value = false
  editingDetailIndex.value = -1
  resetDetailForm()
  showDetailDialog.value = true
}

const editDetail = (detail) => {
  isEditingDetail.value = true
  editingDetailIndex.value = formData.value.details.findIndex(d => d.id === detail.id)
  detailForm.value = {...detail}
  showDetailDialog.value = true
}

const closeDetailDialog = () => {
  showDetailDialog.value = false
  resetDetailForm()
}

const resetDetailForm = () => {
  detailForm.value = {
    id: null,
    pembelianId:null,
    namaItem: '',
    kategoriItem: formData.value.jenisPembelian,
    harga: 0,
    kuantiti: 1,
    total: 0,
    keterangan: ''
  }
}

const calculateDetailTotal = () => {
  const harga = parseFloat(detailForm.value.harga) || 0
  const kuantiti = parseInt(detailForm.value.kuantiti) || 0
  detailForm.value.total = harga * kuantiti
}

const saveDetail = () => {
  // Validate form
  if (!detailForm.value.namaItem || !detailForm.value.harga || !detailForm.value.kuantiti) {
    $q.notify({
      type: 'negative',
      message: 'Mohon lengkapi field yang wajib diisi'
    })
    return
  }

  if (detailForm.value.harga <= 0 || detailForm.value.kuantiti <= 0) {
    $q.notify({
      type: 'negative',
      message: 'Harga dan kuantiti harus lebih dari 0'
    })
    return
  }

  const detailData = {
    ...detailForm.value,
    id: detailForm.value.id,
    pembelianId: formData.value.id,
    harga: parseFloat(detailForm.value.harga),
    kuantiti: parseInt(detailForm.value.kuantiti),
    total: parseFloat(detailForm.value.total)
  }

  if (isEditingDetail.value) {
    // Update existing detail
    formData.value.details[editingDetailIndex.value] = detailData
  } else {
    // Add new detail
    formData.value.details.push(detailData)
  }

  closeDetailDialog()

  $q.notify({
    type: 'positive',
    message: isEditingDetail.value ? 'Item berhasil diupdate' : 'Item berhasil ditambahkan'
  })
}

const deleteDetail = async (detail) => {
  $q.dialog({
    title: 'Konfirmasi Hapus',
    message: `Apakah Anda yakin ingin menghapus item "${detail.namaItem}"?`,
    cancel: true,
    persistent: true
  }).onOk(async () => {
    try {
      // If detail has an ID, delete it from database
      if (detail.id) {
        await api.delete(`/api/pazaauto/pembelian-detail/${detail.id}`)
      }

      // Remove from local array
      const index = formData.value.details.findIndex(d => d.id === detail.id)
      if (index > -1) {
        formData.value.details.splice(index, 1)
        $q.notify({
          type: 'positive',
          message: 'Item berhasil dihapus'
        })
      }
    } catch (error) {
      $q.notify({
        type: 'negative',
        message: 'Failed to delete item',
        caption: error.response?.data?.message || error.message
      })
    }
  })
}

const printTable = async () => {
  try {
    // Fetch all pembelian data without pagination
    const response = await api.get('/api/pazaauto/pembelian/paginated', {
      params: {
        page: 1,
        rowsPerPage: 10000, // Get all records
        search: searchText.value,
        jenisPembelian: filterJenisPembelian.value,
        kategoriOperasional: filterKategoriOperasional.value,
        statusFilter: filterStatus.value ? filterStatus.value.join(',') : '',
        startDate: dateRange.value?.from ? dateRange.value.from.replace(/\//g, '-') : '',
        endDate: dateRange.value?.to ? dateRange.value.to.replace(/\//g, '-') : ''
      }
    })
    
    if (response.data.success) {
      // Extract rows from paginated response
      const records = response.data.data.rows || response.data.data || []
      
      const data = {
        noPenjualan: 'LAPORAN PEMBELIAN',
        namaPelanggan: 'LAPORAN PEMBELIAN', 
        tanggal: new Date().toLocaleDateString('id-ID'),
        grandTotal: records.reduce((sum, item) => sum + (item.grandTotal || 0), 0),
        filters: {
          search: searchText.value || '-',
          dateRange: dateRangeText.value || '-',
          jenisPembelian: filterJenisPembelian.value || '-',
          kategoriOperasional: filterKategoriOperasional.value || '-',
          status: filterStatus.value?.join(', ') || '-'
        },
        items: records.map(item => ({
          nama: item.noPembelian,
          tanggal: formatDateTime(item.tanggalPembelian),
          type: item.jenisPembelian,
          kategori: item.kategoriOperasional,
          supplier: item.supplierId?.namaSupplier || item.namaSupplier || '-',
          harga: item.grandTotal,
          status: item.statusPembayaran,
          metode: item.jenisPembayaran
        }))
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
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to fetch pembelian data for printing',
      caption: error.response?.data?.message || error.message
    })
  }
}

const formatNumber = (value) => {
  if (!value) return '0'
  return new Intl.NumberFormat('id-ID', {
    minimumFractionDigits: 0
  }).format(value)
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

  setTimeout(() => {
    iframe.contentWindow.focus()
    iframe.contentWindow.print()
  }, 250)
}

// Template rendering helper
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
  saving.value = true
  try {
    let response
    if (isEditMode.value) {
      response = await api.put(`/api/pazaauto/pembelian/${formData.value.id}`, formData.value)
    } else {
      response = await api.post('/api/pazaauto/pembelian', formData.value)
    }

    if (response.data.success) {
      $q.notify({
        type: 'positive',
        message: isEditMode.value ? 'Pembelian updated successfully' : 'Pembelian created successfully'
      })
      await fetchPembelian()
      if (!isEditMode.value) {
        await openEditDialog(response.data.data)
      } else {
        // Refresh original data after successful update
        originalFormData.value = JSON.parse(JSON.stringify(formData.value))
      }
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to save pembelian',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    saving.value = false
  }
}

// Watchers
let searchTimeout = null
watch(searchText, () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    pagination.value.page = 1
    fetchPembelian()
  }, 500)
})

watch([filterJenisPembelian, filterKategoriOperasional], () => {
  pagination.value.page = 1
  fetchPembelian()
}, {deep: true})

watch(filterStatus, (newVal) => {
  saveFilterToStorage(newVal)
  pagination.value.page = 1
  fetchPembelian()
}, {deep: true})

watch(dateRange, () => {
  pagination.value.page = 1
  fetchPembelian()
}, {deep: true})

// Lifecycle
onMounted(() => {
  fetchPembelian()
  fetchSuppliers()
})
</script>
