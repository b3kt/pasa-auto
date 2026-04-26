<template>
    <q-page padding>
        <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
            <template v-slot:before>
                <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                    @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                    :on-edit="openEditDialog" row-key="noPenjualan" ref="tableRef"
                    search-placeholder="Search by No Penjualan or SPK..."
                    dense>

                    <template v-slot:toolbar-filters>
                        <div class="row items-center q-gutter-sm">
                           <q-input :model-value="dateRangeText" label="Date Range" outlined dense readonly >
                               <template v-slot:append>
                                   <q-icon name="event" class="cursor-pointer">
                                       <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                                           <q-date v-model="dateRange" range>
                                               <div class="row items-center justify-end q-gutter-sm">
                                                   <q-btn label="Clear" color="primary" flat @click="clearDateRange" />
                                                   <q-btn label="OK" color="primary" flat v-close-popup />
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

                    <template v-slot:body-cell-tanggalJamPenjualan="props">
                            {{ formatDateTime(props.row.tanggalJamPenjualan) }}
                    </template>

                    <template v-slot:body-cell-statusPembayaran="props">
                            <q-badge :color="getStatusColor(props.row.statusPembayaran)" style="width: 100px">
                                {{ props.row.statusPembayaran }}
                            </q-badge>
                    </template>

                    <template v-slot:body-cell-actions="props">
                        <q-td :props="props" class="text-center">
                            <q-btn flat dense round icon="print" color="secondary" @click.stop="printPenjualan(props.row)">
                                <q-tooltip>Print</q-tooltip>
                            </q-btn>
                            <q-btn flat dense round icon="edit" color="primary" @click.stop="openEditDialog(props.row)">
                                <q-tooltip>Edit</q-tooltip>
                            </q-btn>
                        </q-td>
                    </template>
                </GenericTable>
            </template>

            <template v-slot:after>
                <div class="q-pa-md scroll" style="height: 100%">
                    <div class="row items-center q-mb-lg">
                        <div class="text-h6">Detail Penjualan</div>
                        <q-space />
                    </div>

                    <q-form class="q-gutter-md" @submit="handleSave">
                        <!-- SPK Selection -->
                        <!-- <div v-if="!isEditMode">
                            <q-select v-model="selectedSpk" label="Pilih SPK *" outlined dense
                                :options="filteredSpkOptions" :option-label="opt => `${opt.noSpk} - ${opt.namaPelanggan}`"
                                option-value="id" use-input input-debounce="300" @filter="filterSpk"
                                @update:model-value="onSpkChange" :loading="loadingSpk"
                                :rules="[val => !!val || 'SPK harus dipilih']" clearable>
                                <template v-slot:no-option>
                                    <q-item>
                                        <q-item-section class="text-grey">
                                            No unprocessed SPK found
                                        </q-item-section>
                                    </q-item>
                                </template>
                            </q-select>
                        </div> -->

                        <!-- Invoice Information Section -->
                        <div class="q-mb-lg">
                            <div class="text-subtitle text-weight-bold text-grey-8 q-mb-sm">Invoice Information</div>
                            <div class="row q-col-gutter-sm">
                                <div class="col-12">
                                    <q-input v-model="formData.noPenjualan" label="No Penjualan" outlined dense
                                        readonly />
                                </div>
                                <div class="col-12">
                                    <q-input v-model="formData.tanggalJamPenjualan" label="Tanggal Penjualan" outlined
                                        dense type="datetime-local" stack-label :readonly="!isEditable" />
                                </div>
                                <div class="col-12">
                                    <q-input v-model="formData.noSpk" label="No SPK" outlined dense readonly />
                                </div>
                            </div>
                        </div>


                        <!-- Customer Info -->
                        <SPKCustomerInfo
                            v-model:namaPelanggan="formData.namaPelanggan"
                            v-model:alamat="formData.alamatPelanggan"
                            v-model:merk="formData.merkKendaraan"
                            v-model:jenis="formData.jenisKendaraan"
                            v-model:nopol="formData.noPolisi"
                            :isNewCustomer="false" />


                        <!-- Details Editor -->
                        <SPKDetailsEditor v-model:details="formData.details"
                            :allJasaOptions="allJasaOptions"
                            :allBarangOptions="allBarangOptions"
                            :canEdit="formData.statusSpk === 'PROSES' && formData.statusPembayaran !== 'LUNAS'"
                            :noSpk="formData.noSpk" />

                        <!-- Payment Details Section -->
                        <div class="bg-grey-2 q-pa-md rounded-borders q-mb-lg">
                            <div class="text-subtitle1 text-weight-bold text-grey-8 q-mb-sm">Payment Details</div>
                            <div class="row q-col-gutter-sm">
                              <div class="col-12">
                                <q-input v-model="formData.statusPembayaran" label="Total" outlined
                                         dense readonly :model-value="formatCurrency(grandTotal)" />
                              </div>
                              <div class="col-12">
                                    <q-input v-model="formData.statusPembayaran" label="Status Pembayaran" outlined
                                        dense readonly :model-value="determinePaymentStatus()"
                                        :class="determinePaymentStatusStyle()"
                                    />
                                </div>
                                <div class="col-12">
                                    <!-- <q-select v-model="formData.metodePembayaran"
                                        :options="['CASH', 'TRANSFER', 'DEBIT', 'KREDIT']" label="Metode Pembayaran"
                                        outlined dense :readonly="!isEditable" /> -->

                                    <q-input v-model="formData.metodePembayaran" label="Metode Pembayaran" outlined
                                        dense readonly />
                                </div>
                                <div class="col-12">
                                    <q-input v-model.number="formData.uangDibayar" label="Uang Dibayar" outlined dense
                                        type="number" prefix="Rp" readonly
                                        @update:model-value="calculateKembalian" />
                                </div>
                                <div class="col-12">
                                    <q-input v-model.number="formData.kembalian" label="Kembalian" outlined dense
                                        type="number" prefix="Rp" readonly />
                                </div>
                            </div>
                        </div>

                        <!-- Additional Notes -->
                        <div>
                            <div class="text-subtitle1 text-weight-bold text-grey-8 q-mb-sm">Additional Information</div>
                            <q-input v-model="formData.keterangan" label="Keterangan" outlined dense type="textarea"
                                rows="3" :readonly="!isEditable" />
                        </div>

                        <div class="row justify-end q-mt-md q-gutter-sm">
                            <q-btn label="Print" icon="print" color="secondary" @click="printPenjualan(formData)"
                                v-if="formData.noPenjualan" />
                            <q-btn label="Simpan" type="submit" color="primary" :loading="saving" v-if="isEditable" />
                        </div>
                    </q-form>
                </div>
            </template>
        </q-splitter>

        <!-- Keep Delete and Print Dialogs as they are global or large -->
        <!-- Delete Confirmation Dialog -->
        <GenericDialog v-model="showDeleteDialog" title="Konfirmasi hapus data" min-width="400px" position="standard">
            Are you sure you want to delete Penjualan <strong>{{ itemToDelete?.noPenjualan }}</strong>?
            <template #actions>
                <q-btn flat label="Batalkan" color="primary" @click="showDeleteDialog = false" />
                <q-btn flat label="Hapus saja" color="negative" @click="deletePenjualan" :loading="deleting" />
            </template>
        </GenericDialog>

        <!-- Print Preview Dialog -->
        <GenericDialog v-model="showPrintDialog" title="Print Preview" min-width="800px" max-width="90vw">
            <div class="q-pa-sm" style="height: 70vh; width: 100%;">
                <iframe :srcdoc="printPreviewContent"
                    style="width: 100%; height: 100%; border: 1px solid #ccc;"></iframe>
            </div>
            <template #actions>
                <q-btn flat label="Batalkan" color="primary" @click="showPrintDialog = false" />
                <q-btn label="Print" icon="print" color="secondary" @click="confirmPrint" />
            </template>
        </GenericDialog>
    </q-page>
</template>

<script setup>
import { ref, onMounted, watch, nextTick, computed } from 'vue'
import { api } from 'boot/axios'
import { useQuasar, date } from 'quasar'
import { useDateFilter } from 'src/composables/useDateFilter'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import SPKDetailsEditor from 'components/SPKDetailsEditor.vue'
import SPKCustomerInfo from 'components/SPKCustomerInfo.vue'
import fakturTemplate from 'assets/template/faktur.template?raw'

const $q = useQuasar()

// LocalStorage key for filter persistence
const FILTER_STORAGE_KEY = 'penjualan_status_filter'

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
const deleting = ref(false)
const searchText = ref('')
const filterStatus = ref(loadFilterFromStorage())
const { dateRange, dateRangeText, clearDateRange } = useDateFilter('backup-penjualan')
const rows = ref([])
const showDeleteDialog = ref(false)
const isEditMode = ref(false)
const itemToDelete = ref(null)
const showPrintDialog = ref(false)
const printPreviewContent = ref('')
const splitterModel = ref(70)
const tableRef = ref(null)

const selectedSpk = ref(null)

// SPK Options for new record
const unprocessedSpkOptions = ref([])
const filteredSpkOptions = ref([])
const loadingSpk = ref(false)

// Options for SPKDetailsEditor
const allJasaOptions = ref([])
const allBarangOptions = ref([])

// Computed values for SPKDetailsEditor
const jasaRows = computed(() => {
    if (!formData.value.details) return []
    return formData.value.details
        .filter(d => d.jasaId)
        .map(d => {
            const jasa = allJasaOptions.value.find(j => j.id === d.jasaId)
            const harga = d.harga !== undefined && d.harga !== null ? d.harga : (jasa ? jasa.hargaJasa : 0)
            const namaItem = jasa ? jasa.namaJasa : d.namaItem || 'Unknown Service'
            return { ...d, harga, namaItem }
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
            return { ...d, harga, namaItem }
        })
})

const subtotalJasa = computed(() => {
    return jasaRows.value.reduce((sum, row) => sum + (row.harga * row.jumlah), 0)
})

const subtotalBarang = computed(() => {
    return barangRows.value.reduce((sum, row) => sum + (row.harga * row.jumlah), 0)
})

const grandTotal = computed(() => {
  return subtotalBarang.value + subtotalJasa.value;
})

const isEditable = computed(() => {
    if (!isEditMode.value) return true
    return formData.value.statusSpk !== 'SELESAI'
})

const pagination = ref({
    sortBy: 'tanggalJamPenjualan',
    descending: true,
    page: 1,
    rowsPerPage: 10,
    rowsNumber: 0
})

// Form data
const formData = ref({
    noPenjualan: '',
    tanggalJamPenjualan: '',
    noSpk: '',
    grandTotal: 0,
    statusPembayaran: 'BELUM_LUNAS',
    metodePembayaran: 'CASH',
    keterangan: '',
    // SPK related fields
    details: [],
    statusSpk: '',
    namaPelanggan: '',
    alamat: '',
    merk: '',
    jenis: '',
    nopol: '',
    uangDibayar: 0,
    kembalian: 0,
    diskon: 0,
    alamatPelanggan: '',
    merkKendaraan: '',
    jenisKendaraan: '',
})

// Table columns
const columns = [


  {
        name: 'noPenjualan',
        required: true,
        label: 'No Penjualan',
        align: 'left',
        field: 'noPenjualan',
        sortable: true
    },
    {
        name: 'tanggalJamPenjualan',
        label: 'Tanggal',
        align: 'left',
        field: 'tanggalJamPenjualan',
        sortable: true
    },
    {
        name: 'noSpk',
        label: 'No SPK',
        align: 'left',
        field: 'noSpk',
        sortable: true
    },
  {
    name: 'statusPembayaran',
    label: 'Status',
    align: 'center',
    field: 'statusPembayaran',
    sortable: true
  },
    {
        name: 'grandTotal',
        label: 'Grand Total',
        align: 'right',
        field: 'grandTotal',
        sortable: true
    }
]

// Methods
const fetchPenjualan = async (paginationData = pagination.value) => {
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

        // Add status filter - always filter for SPK with PROSES status
        params.spkStatusFilter = 'PROSES'

        // Add date range filter
        if (dateRange.value?.from) {
            // Quasar returns YYYY/MM/DD format by default. Backend needs YYYY-MM-DD.
            params.startDate = dateRange.value.from.replace(/\//g, '-')
        }
        if (dateRange.value?.to) {
            params.endDate = dateRange.value.to.replace(/\//g, '-')
        }

        const response = await api.get('/api/pazaauto/penjualan/paginated', { params })
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
            message: 'Failed to fetch penjualan data',
            caption: error.response?.data?.message || error.message
        })
    } finally {
        loading.value = false
    }
}

const onRequest = (props) => {
    const { page, rowsPerPage, sortBy, descending } = props.pagination
    pagination.value.page = page
    pagination.value.rowsPerPage = rowsPerPage
    pagination.value.sortBy = sortBy
    pagination.value.descending = descending
    fetchPenjualan(pagination.value)
}

const onSearch = (val) => {
    searchText.value = val
    pagination.value.page = 1
    fetchPenjualan()
}

const openEditDialog = async (row) => {
    isEditMode.value = true
    try {
        // Fetch full penjualan details
        const response = await api.get(`/api/pazaauto/penjualan/${row.noPenjualan}`)
        if (response.data.success) {
            formData.value = { ...response.data.data }
            if (!formData.value.details) formData.value.details = [] // Fix undefined prop details
            // Format datetime to YYYY-MM-DDThh:mm for datetime-local input
            if (formData.value.tanggalJamPenjualan) {
                formData.value.tanggalJamPenjualan = formData.value.tanggalJamPenjualan.substring(0, 16)
            }
            // If linked to SPK, fetch SPK details to populate customer info and items
            // if (formData.value.noSpk) {
            //     await fetchSpkDetails(formData.value.noSpk)
            // }
        } else {
            formData.value = { ...row, details: row.details || [] }
            if (formData.value.tanggalJamPenjualan) {
                formData.value.tanggalJamPenjualan = formData.value.tanggalJamPenjualan.substring(0, 16)
            }
        }
        formData.value.grandTotal = grandTotal
    } catch (error) {
        console.error('Failed to fetch penjualan details:', error)
        formData.value = { ...row, details: row.details || [] }
        if (formData.value.tanggalJamPenjualan) {
            formData.value.tanggalJamPenjualan = formData.value.tanggalJamPenjualan.substring(0, 16)
        }
    }

    nextTick(() => {
        tableRef.value?.selectRowByItem(row)
    })
}

const openCreateDialog = async () => {
    isEditMode.value = false
    resetForm()
    await fetchUnprocessedSpk()
}

const fetchUnprocessedSpk = async () => {
    loadingSpk.value = true
    try {
        const response = await api.get('/api/pazaauto/spk/unprocessed')
        if (response.data.success) {
            unprocessedSpkOptions.value = response.data.data
            filteredSpkOptions.value = [...unprocessedSpkOptions.value]
        }
    } catch (error) {
        console.error('Failed to fetch unprocessed SPKs:', error)
    } finally {
        loadingSpk.value = false
    }
}

const fetchJasa = async () => {
    try {
        const response = await api.get('/api/pazaauto/jasa')
        if (response.data.success) {
            allJasaOptions.value = response.data.data || []
        }
    } catch (error) {
        console.error('Failed to fetch jasa:', error)
    }
}

const fetchBarang = async () => {
    try {
        const response = await api.get('/api/pazaauto/barang')
        if (response.data.success) {
            allBarangOptions.value = response.data.data || []
        }
    } catch (error) {
        console.error('Failed to fetch barang:', error)
    }
}

const resetForm = () => {
    selectedSpk.value = null
    formData.value = {
        noPenjualan: '',
        tanggalJamPenjualan: new Date().toISOString().slice(0, 16),
        noSpk: '',
        grandTotal: 0,
        statusPembayaran: 'BELUM_LUNAS',
        metodePembayaran: 'CASH',
        keterangan: '',
        details: [],
        statusSpk: '',
        namaPelanggan: '',
        alamat: '',
        merk: '',
        jenis: '',
        nopol: '',
        uangDibayar: 0,
        kembalian: 0,
        diskon: 0
    }
}

const saving = ref(false)

const handleSave = async () => {
    saving.value = true
    try {
        // Prepare Penjualan data
        const penjualanData = {
            noPenjualan: formData.value.noPenjualan,
            tanggalJamPenjualan: formData.value.tanggalJamPenjualan,
            noSpk: formData.value.noSpk,
            grandTotal: formData.value.grandTotal,
            uangDibayar: formData.value.uangDibayar,
            kembalian: formData.value.kembalian,
            metodePembayaran: formData.value.metodePembayaran,
            statusPembayaran: determinePaymentStatus(),
            diskon: formData.value.diskon,
            keterangan: formData.value.keterangan || `Penjualan for SPK ${formData.value.noSpk}`
        }

        let response
        if (isEditMode.value) {
            response = await api.put(`/api/pazaauto/penjualan/${formData.value.noPenjualan}`, penjualanData)
        } else {
            response = await api.post('/api/pazaauto/penjualan', penjualanData)
        }

        if (response.data.success) {
            $q.notify({
                type: 'positive',
                message: isEditMode.value ? 'Penjualan updated successfully' : 'Penjualan created successfully'
            })
            await fetchPenjualan()
            if (!isEditMode.value) {
                isEditMode.value = true
            }
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: error.response?.data?.message || error.message || 'Failed to save penjualan',
            caption: error.response?.data?.details || ''
        })
    } finally {
        saving.value = false
    }
}

const determinePaymentStatus = () => {
  if(formData.value.statusPembayaran === null) {
    if (formData.value.uangDibayar >= grandTotal.value) {
      return 'LUNAS'
    } else if (formData.value.uangDibayar > 0) {
      return 'DP'
    } else {
      return 'BELUM_LUNAS'
    }
  } else {
    return formData.value.statusPembayaran
  }
}

const determinePaymentStatusStyle = () => {
  if(formData.value.statusPembayaran === null) {
    if (formData.value.uangDibayar >= grandTotal.value) {
      return 'bg-green'
    } else if (formData.value.uangDibayar > 0) {
      return 'bg-yellow'
    } else {
      return 'bg-red'
    }
  } else {
    if (formData.value.statusPembayaran === 'LUNAS') {
      return 'bg-green'
    } else {
      return 'bg-red'
    }
  }
}

const calculateKembalian = () => {
    formData.value.kembalian = (formData.value.uangDibayar || 0) - (grandTotal.value || 0)
}

const deletePenjualan = async () => {
    deleting.value = true
    try {
        const response = await api.delete(`/api/pazaauto/penjualan/${itemToDelete.value.noPenjualan}`)
        if (response.data.success) {
            $q.notify({
                type: 'positive',
                message: 'Penjualan deleted successfully'
            })
            showDeleteDialog.value = false
            itemToDelete.value = null
            await fetchPenjualan()
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: 'Failed to delete penjualan',
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

const formatDateTime = (value) => {
    if (!value) return ''
    return date.formatDate(value, 'YYYY-MM-DD HH:mm')
}

const formatNumber = (value) => {
    if (!value) return '0'
    return new Intl.NumberFormat('id-ID', {
        minimumFractionDigits: 0
    }).format(value)
}

const getStatusColor = (status) => {
    switch (status) {
        case 'LUNAS': return 'green'
        case 'BELUM_LUNAS': return 'red'
        case 'DP': return 'orange'
        default: return 'grey'
    }
}

const printPenjualan = async (row) => {
    try {
        const response = await api.get(`/api/pazaauto/penjualan/${row.noPenjualan}/print`)
        if (response.data.success) {
            const data = response.data.data

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
            message: 'Failed to print penjualan',
            caption: error.response?.data?.message || error.message
        })
    }
}

const confirmPrint = () => {
    // Tambah data invisible iframe for actual printing
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

// Template rendering helper
const renderTemplate = (template, context) => {
    const keys = Object.keys(context)
    const values = Object.values(context)
    try {
        // Tambah data a function that destructures context and returns the evaluated template literal
        // We wrap the template in backticks to make it a template literal
        // Note: The template file itself should NOT contain backticks wrapping the whole content,
        // but it should contain ${} expressions.
        // However, if the template is just text with ${}, we can wrap it in backticks here.
        // But wait, the imported string will be a regular string.
        // We need to evaluate expressions inside it.

        // Alternative: Use a simple replace for specific variables if it's not too complex,
        // OR use the new Function approach if we trust the template source (which is local).

        // The template file currently has ${data.xxx} syntax which is valid for template literals.
        // So we can do: return new Function(...keys, `return \`${template}\`;`)(...values)

        return new Function(...keys, `return \`${template}\`;`)(...values)
    } catch (e) {
        console.error('Template rendering error:', e)
        return 'Error rendering template'
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
        fetchPenjualan()
    }, 500)
})

watch(filterStatus, (newVal) => {
    saveFilterToStorage(newVal)
    pagination.value.page = 1
    fetchPenjualan()
}, { deep: true })

watch(dateRange, () => {
    pagination.value.page = 1
    fetchPenjualan()
}, { deep: true })

// Lifecycle
onMounted(() => {
    fetchPenjualan()
    fetchJasa()
    fetchBarang()
    openCreateDialog()
})
</script>

<style lang="sass" scoped>
.my-sticky-header-table
  max-height: 100%

  thead tr th
    position: sticky
    z-index: 1
    background-color: #ffffff

  thead tr:first-child th
    top: 0

  tr.selected td:after, tr.selected td:before
    content: none
    background: none
  th:hover, td:hover, tr:hover
    background: none
</style>
