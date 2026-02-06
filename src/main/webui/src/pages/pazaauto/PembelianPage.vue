<template>
    <q-page padding>
        <div class="q-pa-md">
            <!-- Toolbar with Tambah data button and Search -->
            <q-toolbar class="shadow-1 rounded-borders q-mb-lg">
                <q-btn flat :label="$t('create') + ' Pembelian'" icon="add" color="white" class="bg-primary"
                    @click="openCreateDialog" />
                <q-space />
                <div class="col-2">
                    <q-select v-model="filterJenisPembelian" :options="jenisPembelianOptions" label="Jenis Pembelian"
                        dense options-dense flat outlined clearable />
                </div>
                <div class="col-2">
                    <q-select v-model="filterKategoriOperasional" :options="kategoriOperasionalOptions"
                        label="Kategori Operasional" dense options-dense flat outlined clearable />
                </div>
                <div class="col-6">
                    <q-input dense standout="bg-primary" v-model="searchText" input-class="search-field text-left"
                        class="q-ml-md" placeholder="Search by No Pembelian...">
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

                <template v-slot:body-cell-jenisPembelian="props">
                        <q-badge :color="props.row.jenisPembelian === 'SPAREPART' ? 'blue' : props.row.jenisPembelian === 'BARANG' ? 'green' : 'purple'">
                            {{ props.row.jenisPembelian }}
                        </q-badge>
                </template>

                <template v-slot:body-cell-grandTotal="props">
                        {{ formatCurrency(props.row.grandTotal) }}
                </template>

                <template v-slot:body-cell-statusPembayaran="props">
                        <q-badge :color="getStatusColor(props.row.statusPembayaran)">
                            {{ props.row.statusPembayaran }}
                        </q-badge>
                </template>

                <template v-slot:body-cell-actions="props">
                        <q-btn flat dense round icon="visibility" color="info" @click="viewDetails(props.row)">
                            <q-tooltip>View Details</q-tooltip>
                        </q-btn>
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
            min-width="90vw">
            <q-form @submit="savePembelian" id="pembelian-form">
                <!-- Purchase Type Selector -->
                <div class="row q-mb-md">
                    <q-option-group v-model="formData.jenisPembelian" :options="jenisPembelianRadioOptions"
                        color="primary" inline @update:model-value="onJenisPembelianChange" />
                </div>

                <div class="row q-col-gutter-md">
                    <div class="col-6">
                        <q-input v-model="formData.noPembelian" label="No Pembelian *" outlined dense
                            readonly placeholder="Auto Generated" />
                    </div>
                    <div class="col-6">
                        <q-input v-model="formData.tanggalPembelian" label="Tanggal Pembelian" outlined dense
                            type="datetime-local" stack-label />
                    </div>
                </div>

                <!-- Operational Expense Fields -->
                <div v-if="formData.jenisPembelian === 'OPERASIONAL'" class="row q-col-gutter-md q-mt-sm">
                    <div class="col-6">
                        <q-input v-model="formData.jenisOperasional" label="Jenis Operasional *" outlined dense
                            placeholder="e.g., Electricity, Rent, Maintenance"
                            :rules="[val => !!val || 'Jenis Operasional harus diisi for operational expenses']" />
                    </div>
                    <div class="col-6">
                        <q-select v-model="formData.kategoriOperasional" label="Kategori Operasional *" outlined dense
                            :options="kategoriOperasionalOptions"
                            :rules="[val => !!val || 'Kategori harus diisi for operational expenses']" />
                    </div>
                </div>

                <!-- Supplier Field -->
                <div class="row q-col-gutter-md q-mt-sm">
                    <div v-if="formData.jenisPembelian === 'SPAREPART'" class="col-6">
                        <q-select v-model="formData.supplierId" label="Supplier" outlined dense use-input
                            input-debounce="300" :options="supplierOptions" option-value="id"
                            option-label="namaSupplier" @filter="filterSuppliers" clearable
                            :rules="formData.jenisPembelian === 'SPAREPART' ? [val => !!val || 'Supplier harus diisi for sparepart purchases'] : []">
                            <template v-slot:no-option>
                                <q-item>
                                    <q-item-section class="text-grey">
                                        No results
                                    </q-item-section>
                                </q-item>
                            </template>
                        </q-select>
                    </div>
                    <div class="col-6">
                        <q-select v-model="formData.statusPembayaran" label="Status Pembayaran" outlined dense
                            :options="statusOptions" />
                    </div>
                </div>

                <div class="row q-col-gutter-md q-mt-sm">
                    <div class="col-4">
                        <q-select v-model="formData.jenisPembayaran" label="Jenis Pembayaran" outlined dense
                            :options="['CASH', 'CREDIT', 'TRANSFER']" />
                    </div>
                    <div class="col-4">
                        <q-input v-model.number="formData.diskon" label="Diskon" outlined dense type="number"
                            prefix="Rp" />
                    </div>
                    <div class="col-4">
                        <q-input v-model.number="formData.ppn" label="PPN" outlined dense type="number" prefix="Rp" />
                    </div>
                </div>

                <div class="q-my-md">
                    <q-input v-model="formData.keterangan" label="Keterangan" outlined dense type="textarea" rows="2" />
                </div>

                <!-- Detail Items Section -->
                <q-separator class="q-my-md" />
                <div class="text-subtitle1 q-mb-md">Detail Items</div>

                <div v-for="(detail, index) in formData.details" :key="index" class="row q-col-gutter-md q-mb-md">
                    <!-- Sparepart Purchase Detail -->
                    <template v-if="formData.jenisPembelian === 'SPAREPART'">
                        <div class="col-4">
                            <q-select v-model="detail.sparepartId" label="Sparepart *" outlined dense use-input
                                input-debounce="300" :options="sparepartOptions" option-value="kodeBarang"
                                option-label="namaSparepart" @filter="filterSpareparts"
                                @update:model-value="onSparepartSelected(detail)"
                                :rules="[val => !!val || 'Sparepart harus diisi']">
                                <template v-slot:no-option>
                                    <q-item>
                                        <q-item-section class="text-grey">
                                            No results
                                        </q-item-section>
                                    </q-item>
                                </template>
                            </q-select>
                        </div>
                        <div class="col-2">
                            <q-input v-model.number="detail.harga" label="Harga *" outlined dense type="number"
                                prefix="Rp" :rules="[val => !!val || 'Harga harus diisi']" readonly />
                        </div>
                        <div class="col-2">
                            <q-input v-model.number="detail.kuantiti" label="Qty *" outlined dense type="number"
                                :rules="[val => !!val || 'Quantity harus diisi']"
                                @update:model-value="calculateDetailTotal(detail)" />
                        </div>
                        <div class="col-2">
                            <q-input v-model.number="detail.total" label="Total" outlined dense type="number"
                                prefix="Rp" readonly />
                        </div>
                    </template>

                    <!-- Barang Purchase Detail -->
                    <template v-else-if="formData.jenisPembelian === 'BARANG'">
                        <div class="col-4">
                            <q-select v-model="detail.barangId" label="Barang *" outlined dense use-input
                                input-debounce="300" :options="barangOptions"
                                :option-label="opt => opt.kodeBarang + ' - ' + opt.namaBarang" @filter="filterBarang"
                                @update:model-value="onBarangSelected(detail)"
                                :rules="[val => !!val || 'Barang harus diisi']">
                                <template v-slot:no-option>
                                    <q-item>
                                        <q-item-section class="text-grey">
                                            No results
                                        </q-item-section>
                                    </q-item>
                                </template>
                            </q-select>
                        </div>
                        <div class="col-2">
                            <q-input v-model.number="detail.harga" label="Harga *" outlined dense type="number"
                                prefix="Rp" :rules="[val => !!val || 'Harga harus diisi']"
                                @update:model-value="calculateDetailTotal(detail)" />
                        </div>
                        <div class="col-2">
                            <q-input v-model.number="detail.kuantiti" label="Qty *" outlined dense type="number"
                                :rules="[val => !!val || 'Quantity harus diisi']"
                                @update:model-value="calculateDetailTotal(detail)" />
                        </div>
                        <div class="col-2">
                            <q-input v-model.number="detail.total" label="Total" outlined dense type="number"
                                prefix="Rp" readonly />
                        </div>
                    </template>

                    <!-- Operational Expense Detail -->
                    <template v-else>
                        <div class="col-4">
                            <q-input v-model="detail.namaItem" label="Item Name *" outlined dense
                                :rules="[val => !!val || 'Item name harus diisi']" />
                        </div>
                        <div class="col-2">
                            <q-input v-model.number="detail.harga" label="Harga *" outlined dense type="number"
                                prefix="Rp" :rules="[val => !!val || 'Harga harus diisi']"
                                @update:model-value="calculateDetailTotal(detail)" />
                        </div>
                        <div class="col-2">
                            <q-input v-model.number="detail.kuantiti" label="Qty *" outlined dense type="number"
                                :rules="[val => !!val || 'Quantity harus diisi']"
                                @update:model-value="calculateDetailTotal(detail)" />
                        </div>
                        <div class="col-2">
                            <q-input v-model.number="detail.total" label="Total" outlined dense type="number"
                                prefix="Rp" readonly />
                        </div>
                    </template>

                    <div class="col-2 flex items-center">
                        <q-btn flat dense round icon="delete" color="negative" @click="removeDetail(index)" />
                    </div>
                </div>

                <q-btn flat label="Add Item" icon="add" color="primary" @click="addDetail" class="q-mb-md" />

                <q-separator class="q-my-md" />

                <!-- Grand Total -->
                <div class="row justify-end">
                    <div class="col-4">
                        <q-input v-model.number="formData.grandTotal" label="Grand Total" outlined dense type="number"
                            prefix="Rp" readonly />
                    </div>
                </div>
            </q-form>
            <template #actions>
                <q-btn flat label="Batalkan" color="primary" @click="showDialog = false" />
                <q-btn v-if="isEditMode" label="Hapus" color="negative" flat @click="confirmDelete(formData)" :loading="deleting" />
                <q-btn label="Simpan" type="submit" form="pembelian-form" color="primary" :loading="saving" />
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

        <!-- View Details Dialog -->
        <GenericDialog v-model="showDetailsDialog" :title="`Pembelian Details - ${selectedItem?.noPembelian}`"
            min-width="600px">
            <q-list bordered separator>
                <q-item v-for="detail in itemDetails" :key="detail.id">
                    <q-item-section>
                        <q-item-label>{{ detail.namaItem }}</q-item-label>
                        <q-item-label caption>{{ detail.keterangan }}</q-item-label>
                    </q-item-section>
                    <q-item-section side>
                        <q-item-label>{{ detail.kuantiti }} x {{ formatCurrency(detail.harga) }}</q-item-label>
                        <q-item-label caption>{{ formatCurrency(detail.total) }}</q-item-label>
                    </q-item-section>
                </q-item>
            </q-list>

            <template #actions>
                <q-btn flat label="Close" color="primary" v-close-popup />
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

// State
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const searchText = ref('')
const filterJenisPembelian = ref(null)
const filterKategoriOperasional = ref(null)
const rows = ref([])
const showDialog = ref(false)
const showDeleteDialog = ref(false)
const showDetailsDialog = ref(false)
const isEditMode = ref(false)
const itemToDelete = ref(null)
const selectedItem = ref(null)
const itemDetails = ref([])

// Options
const jenisPembelianOptions = ['SPAREPART', 'OPERASIONAL', 'BARANG']
const jenisPembelianRadioOptions = [
    { label: 'Sparepart Purchase', value: 'SPAREPART' },
    { label: 'Barang Purchase (Non-Supplier)', value: 'BARANG' },
    { label: 'Operational Expense', value: 'OPERASIONAL' }
]
const kategoriOperasionalOptions = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', 'ON_DEMAND']
const statusOptions = ['LUNAS', 'BELUM_LUNAS', 'DP']

const supplierOptions = ref([])
const sparepartOptions = ref([])
const barangOptions = ref([])

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
        sortable: true,
        format: val => val ? new Date(val).toLocaleString() : '-'
    },
    {
        name: 'jenisPembelian',
        label: 'Jenis',
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

        if (filterJenisPembelian.value) {
            params.jenisPembelian = filterJenisPembelian.value
        }

        if (filterKategoriOperasional.value) {
            params.kategoriOperasional = filterKategoriOperasional.value
        }

        const response = await api.get('/api/pazaauto/pembelian/paginated', { params })
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
    generateNoPembelian() // Generate number on create
}

const openEditDialog = async (row) => {
    isEditMode.value = true
    formData.value = { ...row }

    // Fetch details
    try {
        const response = await api.get(`/api/pazaauto/pembelian-detail/by-pembelian/${row.id}`)
        if (response.data.success) {
            formData.value.details = (response.data.data || []).map(d => ({
                ...d,
                barangId: d.barang || d.barangId // Use object if available
            }))
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: 'Failed to fetch details',
            caption: error.response?.data?.message || error.message
        })
    }

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
    addDetail()
}

const onJenisPembelianChange = () => {
    formData.value.details = []
    addDetail()
    if (!isEditMode.value) {
        generateNoPembelian() // Regenerate number when type changes
    }
}

const addDetail = () => {
    formData.value.details.push({
        namaItem: '',
        kategoriItem: formData.value.jenisPembelian,
        harga: 0,
        kuantiti: 1,
        total: 0,
        keterangan: '',
        barangId: null,
        sparepartId: null
    })
}

const removeDetail = (index) => {
    formData.value.details.splice(index, 1)
    calculateGrandTotal()
}

const generateNoPembelian = async () => {
    try {
        const response = await api.get('/api/pazaauto/pembelian/generate-no', {
            params: { jenisPembelian: formData.value.jenisPembelian }
        })
        if (response.data.success) {
            formData.value.noPembelian = response.data.data
        }
    } catch (error) {
        console.error('Failed to generate no pembelian', error)
        $q.notify({
            type: 'warning',
            message: 'Failed to auto-generate No Pembelian'
        })
    }
}

const calculateDetailTotal = (detail) => {
    detail.total = (detail.harga || 0) * (detail.kuantiti || 0)
    calculateGrandTotal()
}

const calculateGrandTotal = () => {
    const subtotal = formData.value.details.reduce((sum, detail) => sum + (detail.total || 0), 0)
    const diskon = formData.value.diskon || 0
    const ppn = formData.value.ppn || 0
    formData.value.grandTotal = subtotal - diskon + ppn
}

const onSparepartSelected = (detail) => {
    if (detail.sparepartId) {
        detail.namaItem = detail.sparepartId.namaSparepart
        detail.harga = detail.sparepartId.hargaBeli || 0
        calculateDetailTotal(detail)
    }
}

const filterSuppliers = async (val, update) => {
    if (val === '') {
        update(() => {
            supplierOptions.value = []
        })
        return
    }

    try {
        const response = await api.get('/api/pazaauto/supplier', {
            params: { search: val }
        })
        update(() => {
            if (response.data.success) {
                supplierOptions.value = response.data.data || []
            }
        })
    } catch (error) {
        update(() => {
            console.error('Error fetching suppliers:', error)
            supplierOptions.value = []
        })
    }
}

const filterSpareparts = async (val, update) => {
    if (val === '') {
        update(() => {
            sparepartOptions.value = []
        })
        return
    }

    try {
        const params = { search: val }
        const supplierId = formData.value.supplierId?.id || formData.value.supplierId
        if (supplierId) {
            params.supplierId = supplierId
        }

        const response = await api.get('/api/pazaauto/sparepart', { params })
        update(() => {
            if (response.data.success) {
                sparepartOptions.value = response.data.data || []
            }
        })
    } catch (error) {
        update(() => {
            console.error('Error fetching spareparts:', error)
            sparepartOptions.value = []
        })
    }
}



const filterBarang = async (val, update) => {
    if (val === '') {
        update(() => {
            barangOptions.value = []
        })
        return
    }

    try {
        const response = await api.get('/api/pazaauto/barang', {
            params: { search: val }
        })
        update(() => {
            if (response.data.success) {
                barangOptions.value = response.data.data || []
            }
        })
    } catch (error) {
        update(() => {
            console.error('Error fetching barang:', error)
            barangOptions.value = []
        })
    }
}

const onBarangSelected = (detail) => {
    if (detail.barangId) {
        detail.namaItem = detail.barangId.namaBarang
        detail.harga = detail.barangId.hargaBeli || 0
        calculateDetailTotal(detail)
    }
}

const savePembelian = async () => {
    saving.value = true
    try {
        // Prepare data
        const payload = {
            ...formData.value,
            tanggalPembelian: formData.value.tanggalPembelian ? new Date(formData.value.tanggalPembelian).toISOString() : null,
            supplierId: formData.value.supplierId?.id || formData.value.supplierId
        }

        // Prepare details
        const details = formData.value.details.map(detail => ({
            ...detail,
            sparepartId: detail.sparepartId?.kodeBarang || detail.sparepartId,
            barangId: detail.barangId?.id || detail.barangId?.kodeBarang || (typeof detail.barangId === 'object' ? detail.barangId.id : detail.barangId), // Handle ID extraction safely
            kategoriItem: formData.value.jenisPembelian,
            supplierId: formData.value.supplierId?.id || formData.value.supplierId
        }))

        const requestBody = {
            pembelian: payload,
            details: details
        }

        let response
        if (isEditMode.value) {
            response = await api.put(`/api/pazaauto/pembelian/${formData.value.id}/with-details`, requestBody)
        } else {
            response = await api.post('/api/pazaauto/pembelian/with-details', requestBody)
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

const viewDetails = async (row) => {
    selectedItem.value = row
    try {
        const response = await api.get(`/api/pazaauto/pembelian-detail/by-pembelian/${row.id}`)
        if (response.data.success) {
            itemDetails.value = response.data.data || []
            showDetailsDialog.value = true
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: 'Failed to fetch details',
            caption: error.response?.data?.message || error.message
        })
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

watch([filterJenisPembelian, filterKategoriOperasional], () => {
    pagination.value.page = 1
    fetchPembelian()
}, { deep: true })

watch(() => formData.value.details, () => {
    calculateGrandTotal()
}, { deep: true })

watch([() => formData.value.diskon, () => formData.value.ppn], () => {
    calculateGrandTotal()
})

// Lifecycle
onMounted(() => {
    fetchPembelian()
})
</script>

<style lang="sass" scoped>
.dialog-pembelian
  min-width: calc(90vw - 48px)

.my-sticky-header-table
  max-height: 70vh

  thead tr th
    position: sticky
    z-index: 1
    background-color: #ffffff

  thead tr:first-child th
    top: 0
</style>
