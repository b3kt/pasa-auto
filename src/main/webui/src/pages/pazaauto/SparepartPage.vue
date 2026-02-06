<template>
  <q-page padding>
    <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch" :on-create="openCreateDialog"
      :on-edit="openEditDialog" :on-delete="confirmDelete" create-label="Tambah data Sparepart"
      search-placeholder="Search by code or name...">
      <template v-slot:body-cell-active="props">
        <q-td :props="props">
          <q-badge :color="props.row.active ? 'green' : 'red'">
            {{ props.row.active ? 'Active' : 'Inactive' }}
          </q-badge>
        </q-td>
      </template>

      <template v-slot:body-cell-hargaJual="props">
        <q-td :props="props">
          {{ formatCurrency(props.row.hargaJual) }}
        </q-td>
      </template>

      <template v-slot:body-cell-hargaBeli="props">
        <q-td :props="props">
          {{ formatCurrency(props.row.hargaBeli) }}
        </q-td>
      </template>
    </GenericTable>

    <!-- Create/Edit Dialog -->
    <GenericDialog v-model="showDialog" :title="isEditMode ? 'Edit Sparepart' : 'Tambah data Sparepart'" min-width="600px">
      <q-form @submit="handleSave" id="sparepart-form" class="q-gutter-md">
        <q-input v-model="formData.kodeSparepart" label="Kode Sparepart *" outlined dense
          :rules="[val => !!val || 'Kode Sparepart harus diisi']" />

        <q-input v-model="formData.namaSparepart" label="Nama Sparepart *" outlined dense
          :rules="[val => !!val || 'Nama Sparepart harus diisi']" />

        <div class="row q-col-gutter">
          <div class="col-6">
            <q-input v-model.number="formData.hargaJual" label="Harga Jual" outlined dense type="number" step="0.01"
              prefix="Rp" class="q-mr-md" />
          </div>
          <div class="col-6">
            <q-input v-model.number="formData.hargaBeli" label="Harga Beli" outlined dense type="number" step="0.01"
              prefix="Rp" />
          </div>
        </div>

        <div class="row q-col-gutter">
          <div class="col-4">
            <q-input v-model.number="formData.stok" label="Stok" outlined dense type="number" class="q-mr-md" />
          </div>
          <div class="col-4">
            <q-input v-model.number="formData.stokMinimal" label="Stok Minimal" outlined dense type="number"
              class="q-mr-md" />
          </div>
          <div class="col-4">
            <q-input v-model="formData.satuan" label="Satuan" outlined dense />
          </div>
        </div>

        <div class="row q-col-gutter">
          <div class="col-6">
            <q-input v-model="formData.merek" label="Merek" outlined dense class="q-mr-md" />
          </div>
          <div class="col-6">
            <q-input v-model="formData.tipeKendaraan" label="Tipe Kendaraan" outlined dense />
          </div>
        </div>

        <q-select v-model="formData.supplierId" label="Supplier" outlined dense use-input input-debounce="300"
          :options="supplierOptions" option-value="id" option-label="namaSupplier" @filter="filterSuppliers" emit-value
          map-options clearable>
          <template v-slot:no-option>
            <q-item>
              <q-item-section class="text-grey">
                No results
              </q-item-section>
            </q-item>
          </template>
        </q-select>

        <q-input v-model="formData.keterangan" label="Keterangan" outlined dense type="textarea" rows="2" />

        <q-checkbox v-model="formData.active" label="Active" />
      </q-form>
      <template #actions>
        <q-btn flat label="Batalkan" color="primary" @click="showDialog = false" />
        <q-btn label="Simpan" type="submit" form="sparepart-form" color="primary" :loading="saving" />
      </template>
    </GenericDialog>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" title="Konfirmasi hapus data" min-width="400px" position="standard">
      Are you sure you want to delete <strong>{{ itemToDelete?.namaSparepart }}</strong>?
      <template #actions>
        <q-btn flat label="Batalkan" color="primary" @click="showDeleteDialog = false" />
        <q-btn flat label="Hapus saja" color="negative" @click="deleteItem" :loading="deleting" />
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from 'boot/axios'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import { useCrud } from 'src/composables/useCrud'

// Use CRUD Composable
const {
  rows,
  loading,
  saving,
  deleting,
  showDialog,
  showDeleteDialog,
  isEditMode,
  itemToDelete,
  pagination,
  fetchData,
  onRequest,
  onSearch,
  saveData,
  confirmDelete,
  deleteItem,
  openCreateDialog: baseOpenCreateDialog,
  openEditDialog: baseOpenEditDialog
} = useCrud({
  baseApiUrl: '/api/pazaauto/sparepart',
  idField: 'kodeBarang',
  defaultPagination: {
    sortBy: null,
    descending: false,
    page: 1,
    rowsPerPage: 10,
    rowsNumber: 0
  }
})

// Supplier Logic
const supplierOptions = ref([])

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

// Form Data
const formData = ref({
  kodeBarang: '',
  kodeSparepart: '',
  namaSparepart: '',
  hargaJual: null,
  hargaBeli: null,
  stok: null,
  stokMinimal: null,
  satuan: '',
  merek: '',
  tipeKendaraan: '',
  supplierId: null,
  keterangan: '',
  active: true
})

const resetForm = () => {
  formData.value = {
    kodeBarang: '',
    kodeSparepart: '',
    namaSparepart: '',
    hargaJual: null,
    hargaBeli: null,
    stok: null,
    stokMinimal: null,
    satuan: '',
    merek: '',
    tipeKendaraan: '',
    supplierId: null,
    keterangan: '',
    active: true
  }
  supplierOptions.value = []
}

const openCreateDialog = () => {
  baseOpenCreateDialog(resetForm)
}

const openEditDialog = async (row) => {
  baseOpenEditDialog(row, (r) => {
    formData.value = { ...r }
  })

  // Pre-load supplier if exists
  if (row.supplierId) {
    try {
      const response = await api.get(`/api/pazaauto/supplier/${row.supplierId}`)
      if (response.data.success) {
        supplierOptions.value = [response.data.data]
      }
    } catch (error) {
      console.error('Error fetching supplier details:', error)
    }
  }
}

const handleSave = async () => {
  await saveData(formData.value)
}

// Table Columns
const columns = [
  {
    name: 'kodeSparepart',
    required: true,
    label: 'Kode Sparepart',
    align: 'left',
    field: 'kodeSparepart',
    sortable: true
  },
  {
    name: 'namaSparepart',
    required: true,
    label: 'Nama Sparepart',
    align: 'left',
    field: 'namaSparepart',
    sortable: true
  },
  {
    name: 'hargaJual',
    label: 'Harga Jual',
    align: 'right',
    field: 'hargaJual',
    sortable: true
  },
  {
    name: 'hargaBeli',
    label: 'Harga Beli',
    align: 'right',
    field: 'hargaBeli',
    sortable: true
  },
  {
    name: 'stok',
    label: 'Stok',
    align: 'center',
    field: 'stok',
    sortable: true
  },
  {
    name: 'merek',
    label: 'Merek',
    align: 'left',
    field: 'merek'
  },
  {
    name: 'active',
    label: 'Status',
    align: 'center',
    field: 'active',
    sortable: true
  },
  {
    name: 'actions',
    label: 'Actions',
    align: 'center',
    field: 'actions'
  }
]

const formatCurrency = (value) => {
  if (!value) return 'Rp 0'
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    minimumFractionDigits: 0
  }).format(value)
}

// Lifecycle
onMounted(() => {
  fetchData()
})
</script>

<style lang="sass" scoped>
</style>
