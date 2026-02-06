<template>
    <q-page padding>
        <div class="q-pa-md">
            <!-- Toolbar with Tambah data button and Search -->
            <q-toolbar class="shadow-1 rounded-borders q-mb-lg">
                <q-btn flat :label="$t('create') + ' Pembelian'" icon="add" color="white" class="bg-primary"
                    @click="openCreateDialog" />
                <q-space />
                <div class="col-2">
                    <q-select v-model="filterStatus" multiple :options="statusOptions" label="Status Pembayaran" dense
                        options-dense flat outlined />
                </div>
                <div class="col-6">
                    <q-input dense standout="bg-primary" v-model="searchText" input-class="search-field text-left"
                        class="q-ml-md" placeholder="Search by No Pembelian or Supplier...">
                        <template v-slot:append>
                            <q-icon v-if="searchText === ''" name="search" />
                            <q-icon v-else name="clear" class="cursor-pointer" @click="searchText = ''" />
                        </template>
                    </q-input>
                </div>
            </q-toolbar>

            <!-- Data Table -->
            <q-table class="my-sticky-header-table" flat bordered :rows="rows" :columns="columns" row-key="id"
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
                        <q-btn flat dense round icon="edit" color="primary" @click="openEditDialog(props.row)">
                            <q-tooltip>Edit</q-tooltip>
                        </q-btn>
                        <q-btn flat dense round icon="delete" color="negative" @click="confirmDelete(props.row)">
                            <q-tooltip>Delete</q-tooltip>
                        </q-btn>
                </template>
            </q-table>
        </div>

        <!-- Create/Edit Dialog -->
        <GenericDialog v-model="showDialog" :title="isEditMode ? 'Edit Pembelian' : 'Tambah data Pembelian'"
            min-width="80vw">
            <q-form @submit="savePembelian" id="pembelian-barang-form" class="q-gutter-md">
                <div class="row q-col-gutter-md">
                    <div class="col-6">
                        <q-input v-model="formData.noPembelian" label="No Pembelian *" outlined dense
                            :rules="[val => !!val || 'No Pembelian harus diisi']" />
                    </div>
                    <div class="col-3">
                        <q-input v-model="formData.tanggalPembelian" label="Tanggal Pembelian" outlined dense
                            type="date" stack-label />
                    </div>
                </div>

                <div class="row q-col-gutter-md">
                    <div class="col-6">
                        <q-input v-model="formData.namaSupplier" label="Nama Supplier *" outlined dense
                            :rules="[val => !!val || 'Nama Supplier harus diisi']" />

                    </div>

                </div>


                <div class="row q-col-gutter-md">
                    <div class="col-6">
                        <q-input v-model.number="formData.grandTotal" label="Grand Total" outlined dense type="number"
                            prefix="Rp" />

                    </div>
                    <div class="col-3">
                        <q-select v-model="formData.jenisPembayaran" label="Jenis Pembayaran" outlined dense
                            :options="['CASH', 'CREDIT', 'TRANSFER']" />

                    </div>
                    <div class="col-3">
                        <q-select v-model="formData.statusPembayaran" label="Status Pembayaran" outlined dense
                            :options="statusOptions" />

                    </div>
                </div>

                <div class="q-my-md">
                    <q-input v-model="formData.keterangan" label="Keterangan" outlined dense type="textarea" rows="3" />
                </div>
            </q-form>
            <template #actions>
                <q-btn flat label="Batalkan" color="primary" @click="closeDialog" />
                <q-btn v-if="isEditMode" label="Hapus" color="negative" flat @click="confirmDelete(formData)" :loading="deleting" />
                <q-btn label="Simpan" type="submit" form="pembelian-barang-form" color="primary" :loading="saving" />
            </template>
        </GenericDialog>

        <!-- Delete Confirmation Dialog -->
        <GenericDialog v-model="showDeleteDialog" title="Konfirmasi hapus data" min-width="400px" position="standard">
            Are you sure you want to delete Pembelian <strong>{{ itemToDelete?.noPembelian }}</strong>?
            <template #actions>
                <q-btn flat label="Batalkan" color="primary" @click="showDeleteDialog = false" />
                <q-btn flat label="Hapus saja" color="negative" @click="deletePembelian" :loading="deleting" />
            </template>
        </GenericDialog>
    </q-page>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { api } from 'boot/axios'
import { useQuasar } from 'quasar'
import GenericDialog from 'components/GenericDialog.vue'
import { useKeyboardShortcuts } from 'src/composables/useKeyboardShortcuts'

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
const filterStatus = ref(loadFilterFromStorage())
const rows = ref([])
const showDialog = ref(false)
const showDeleteDialog = ref(false)
const isEditMode = ref(false)
const itemToDelete = ref(null)

// Status options
const statusOptions = ref([
    'LUNAS',
    'BELUM_LUNAS',
    'DP'
])

const pagination = ref({
    sortBy: 'createdAt',
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
    namaSupplier: '',
    grandTotal: 0,
    statusPembayaran: 'BELUM_LUNAS',
    jenisPembayaran: 'CASH',
    keterangan: ''
})

// Table columns
const columns = [
    {
        name: 'noPembelian',
        required: true,
        label: 'No Pembelian',
        align: 'left',
        field: 'noPembelian',
        sortable: true
    },
    {
        name: 'tanggalPembelian',
        label: 'Tanggal',
        align: 'left',
        field: 'tanggalPembelian',
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

        if (filterStatus.value && filterStatus.value.length > 0) {
            params.statusFilter = filterStatus.value.join(',')
        }

        const response = await api.get('/api/pazaauto/pembelian-barang/paginated', { params })
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
    const { page, rowsPerPage, sortBy, descending } = props.pagination
    pagination.value.page = page
    pagination.value.rowsPerPage = rowsPerPage
    pagination.value.sortBy = sortBy
    pagination.value.descending = descending
    fetchPembelian(pagination.value)
}

const openCreateDialog = () => {
    isEditMode.value = false
    resetForm()
    showDialog.value = true
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
        id: null,
        noPembelian: '',
        tanggalPembelian: new Date().toISOString().split('T')[0],
        namaSupplier: '',
        grandTotal: 0,
        statusPembayaran: 'BELUM_LUNAS',
        jenisPembayaran: 'CASH',
        keterangan: ''
    }
}

const savePembelian = async () => {
    saving.value = true
    try {
        let response
        if (isEditMode.value) {
            response = await api.put(`/api/pazaauto/pembelian-barang/${formData.value.id}`, formData.value)
        } else {
            response = await api.post('/api/pazaauto/pembelian-barang', formData.value)
        }

        if (response.data.success) {
            $q.notify({
                type: 'positive',
                message: isEditMode.value ? 'Pembelian updated successfully' : 'Pembelian created successfully'
            })
            closeDialog()
            await fetchPembelian()
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

// Keyboard Shortcuts
useKeyboardShortcuts({
    onSave: () => {
        if (showDialog.value && !saving.value) {
            savePembelian()
        }
    },
    onDelete: () => {
        if (showDialog.value && isEditMode.value && !deleting.value) {
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

const deletePembelian = async () => {
    deleting.value = true
    try {
        const response = await api.delete(`/api/pazaauto/pembelian-barang/${itemToDelete.value.id}`)
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
        case 'LUNAS': return 'green'
        case 'BELUM_LUNAS': return 'red'
        case 'DP': return 'orange'
        default: return 'grey'
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

watch(filterStatus, (newVal) => {
    saveFilterToStorage(newVal)
    pagination.value.page = 1
    fetchPembelian()
}, { deep: true })

// Lifecycle
onMounted(() => {
    fetchPembelian()
})
</script>

<style lang="sass" scoped>
.dialog-pembelian
  min-width: calc(80vw - 48px)

.my-sticky-header-table
  max-height: 70vh

  thead tr th
    position: sticky
    z-index: 1
    background-color: #ffffff

  thead tr:first-child th
    top: 0
</style>
