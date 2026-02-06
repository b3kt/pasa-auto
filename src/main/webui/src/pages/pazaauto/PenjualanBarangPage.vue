<template>
    <q-page padding>
        <div class="q-pa-md">
            <!-- Toolbar with Tambah data button and Search -->
            <q-toolbar class="shadow-1 rounded-borders q-mb-lg">
                <!-- <q-btn flat :label="$t('create') + ' Penjualan'" icon="add" color="white" class="bg-primary"
                    @click="openCreateDialog" /> -->
                <q-space />
                <div class="col-auto q-mr-md">
                    <q-checkbox v-model="filterToday" label="Filter Penjualan hari ini" dense />
                </div>
                <div class="col-2">
                    <q-select v-model="filterStatus" multiple :options="statusOptions" label="Status Pembayaran" dense
                        options-dense flat outlined />
                </div>
                <div class="col-6">
                    <q-input dense standout="bg-primary" v-model="searchText" input-class="search-field text-left"
                        class="q-ml-md" placeholder="Search by No Penjualan or SPK...">
                        <template v-slot:append>
                            <q-icon v-if="searchText === ''" name="search" />
                            <q-icon v-else name="clear" class="cursor-pointer" @click="searchText = ''" />
                        </template>
                    </q-input>
                </div>
            </q-toolbar>

            <!-- Data Table -->
            <q-table class="my-sticky-header-table" flat bordered :rows="rows" :columns="columns" row-key="noPenjualan"
                :loading="loading" v-model:pagination="pagination" @request="onRequest" binary-state-sort>

                <template v-slot:body-cell-grandTotal="props">
                        {{ formatCurrency(props.row.grandTotal) }}
                </template>

                <template v-slot:body-cell-statusPembayaran="props">
                        <q-badge :color="getStatusColor(props.row.statusPembayaran)">
                            {{ props.row.statusPembayaran }}
                        </q-badge>
                </template>

                <template v-slot:body-cell-actions="props">
                        <q-btn flat dense round icon="print" color="secondary" @click="printPenjualan(props.row)">
                            <q-tooltip>Print</q-tooltip>
                        </q-btn>
                        <q-btn flat dense round icon="edit" color="primary" @click="openEditDialog(props.row)">
                            <q-tooltip>Edit</q-tooltip>
                        </q-btn>
                </template>
            </q-table>
        </div>

        <!-- Create/Edit Dialog -->
        <GenericDialog v-model="showDialog" min-width="700px" max-width="800px">
            <template #header>
                <q-card-section class="bg-primary text-white">
                    <div class="row items-center">
                        <div class="col">
                            <div class="text-h5">{{ isEditMode ? 'PENJUALAN' : 'PENJUALAN' }}</div>
                            <div class="text-subtitle2">{{ formData.noPenjualan || 'New Invoice' }}</div>
                        </div>
                        <div class="col-auto text-right">
                            <div class="text-subtitle2">Date: {{ new Date().toLocaleDateString('id-ID') }}</div>
                            <q-badge :color="getStatusColor(formData.statusPembayaran)" class="q-mt-xs">
                                {{ formData.statusPembayaran || 'NEW' }}
                            </q-badge>
                        </div>
                    </div>
                </q-card-section>
            </template>

            <q-separator />

            <q-form @submit.prevent="closeDialog" class="q-gutter-md q-mt-md">
                <!-- Invoice Information Section -->
                <div>
                    <div class="text-subtitle1 text-weight-bold text-grey-8 q-mb-md">Invoice Information</div>
                    <div class="row q-col-gutter-md">
                        <div class="col-6">
                            <q-input v-model="formData.noPenjualan" label="No Penjualan" outlined dense readonly />
                        </div>
                        <div class="col-6">
                            <q-input v-model="formData.tanggalJamPenjualan" label="Tanggal Penjualan" outlined dense
                                type="datetime-local" stack-label readonly />
                        </div>
                    </div>
                    <div class="row q-col-gutter-md q-mt-xs">
                        <div class="col-6">
                            <q-input v-model="formData.noSpk" label="No SPK" outlined dense readonly />
                        </div>
                        <div class="col-6">
                            <q-input v-model.number="formData.grandTotal" label="Grand Total" outlined dense
                                type="number" prefix="Rp" readonly />
                        </div>
                    </div>
                </div>

                <q-separator class="q-my-md" />

                <!-- Payment Details Section -->
                <div class="bg-grey-2 q-pa-md rounded-borders">
                    <div class="text-subtitle1 text-weight-bold text-grey-8 q-mb-md">Payment Details</div>
                    <div class="row q-col-gutter-md">
                        <div class="col-4">
                            <q-input v-model="formData.statusPembayaran" label="Status Pembayaran" outlined dense
                                readonly />
                        </div>
                        <div class="col-4">
                            <q-input v-model="formData.metodePembayaran" label="Metode Pembayaran" outlined dense
                                readonly />
                        </div>
                        <div class="col-4">
                            <q-input v-model.number="formData.uangDibayar" label="Uang Dibayar" outlined dense
                                type="number" prefix="Rp" readonly />
                        </div>
                    </div>
                    <div class="row q-col-gutter-md q-mt-xs">
                        <div class="col-6">
                            <q-input v-model.number="formData.kembalian" label="Kembalian" outlined dense type="number"
                                prefix="Rp" readonly />
                        </div>
                        <div class="col-6">
                            <q-input v-model.number="formData.diskon" label="Diskon" outlined dense type="number"
                                prefix="Rp" readonly />
                        </div>
                    </div>
                </div>

                <q-separator class="q-my-md" />

                <!-- Additional Notes -->
                <div>
                    <div class="text-subtitle1 text-weight-bold text-grey-8 q-mb-md">Additional Information
                    </div>
                    <q-input v-model="formData.keterangan" label="Keterangan" outlined dense type="textarea" rows="3"
                        readonly />
                </div>
            </q-form>
            <template #actions>
                <q-btn label="Close" color="primary" @click="closeDialog" />
            </template>
        </GenericDialog>

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
                <iframe :srcdoc="printPreviewContent" style="width: 100%; height: 100%; border: 1px solid #ccc;"></iframe>
            </div>
            <template #actions>
                <q-btn flat label="Batalkan" color="primary" @click="showPrintDialog = false" />
                <q-btn label="Print" icon="print" color="secondary" @click="confirmPrint" />
            </template>
        </GenericDialog>
    </q-page>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { api } from 'boot/axios'
import { useQuasar } from 'quasar'
import GenericDialog from 'components/GenericDialog.vue'
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
const filterToday = ref(false)
const rows = ref([])
const showDialog = ref(false)
const showDeleteDialog = ref(false)
const isEditMode = ref(false)
const itemToDelete = ref(null)
const showPrintDialog = ref(false)
const printPreviewContent = ref('')

// Status options
const statusOptions = ref([
    'LUNAS',
    'BELUM_LUNAS',
    'DP'
])

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
    keterangan: ''
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
        name: 'grandTotal',
        label: 'Grand Total',
        align: 'right',
        field: 'grandTotal',
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
        name: 'actions',
        label: 'Actions',
        align: 'center',
        field: 'actions'
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

        if (filterStatus.value && filterStatus.value.length > 0) {
            params.statusFilter = filterStatus.value.join(',')
        }

        // Add today filter if checked
        if (filterToday.value) {
            params.filterToday = true
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

const openEditDialog = (row) => {
    isEditMode.value = true
    formData.value = { ...row }
    showDialog.value = true
}

const closeDialog = () => {
    showDialog.value = false
    resetForm()
}

const resetForm = () => {
    formData.value = {
        noPenjualan: '',
        tanggalJamPenjualan: new Date().toISOString().slice(0, 16), // Format YYYY-MM-DDTHH:mm
        noSpk: '',
        grandTotal: 0,
        statusPembayaran: 'BELUM_LUNAS',
        metodePembayaran: 'CASH',
        keterangan: ''
    }
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

watch(filterToday, () => {
    pagination.value.page = 1
    fetchPenjualan()
})

// Lifecycle
onMounted(() => {
    fetchPenjualan()
})
</script>

<style lang="sass" scoped>
.my-sticky-header-table
  max-height: 70vh

  thead tr th
    position: sticky
    z-index: 1
    background-color: #ffffff

  thead tr:first-child th
    top: 0
</style>
