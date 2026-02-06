<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
          @update:pagination="pagination = $event" @request="onRequest" @search="onSearch" :on-create="openCreateDialog"
          :on-edit="openEditDialog" create-label="Tambah data Jasa" ref="tableRef"
          search-placeholder="Search by name...">
          <template v-slot:body-cell-hargaJasa="props">
              {{ formatCurrency(props.row.hargaJasa) }}
          </template>
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6 q-mb-md">{{ isEditMode ? 'Edit Jasa' : 'Tambah data Jasa' }}</div>
            <q-space />
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>New</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="jasa-form" class="q-gutter-md">
            <q-input v-model="formData.namaJasa" label="Nama Jasa *" outlined dense
              :rules="[val => !!val || 'Nama Jasa tidak boleh kosong']" hide-bottom-space/>

            <q-input v-model.number="formData.hargaJasa" label="Harga Jasa" outlined dense type="number" prefix="Rp"
              :rules="[val => val >= 0 || 'Harga jasa tidak valid']" hide-bottom-space/>

            <q-input v-model.number="formData.estimasiWaktu" label="Estimasi Waktu (menit)" outlined dense type="number"
              :rules="[val => val >= 0 || 'Waktu estimasi tidak valid']" hide-bottom-space/>

            <q-input v-model="formData.deskripsi" label="Deskripsi" outlined dense type="textarea" rows="3" />

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode" label="Hapus" color="negative" flat @click="confirmDelete(formData)" :loading="deleting" />
              <q-btn label="Simpan" type="submit" color="primary" :loading="saving" :disable="isEditMode && !isDirty(formData)" />
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" title="Konfirmasi hapus data" min-width="400px" position="standard">
      Are you sure you want to delete <strong>{{ itemToDelete?.namaJasa }}</strong>?
      <template #actions>
        <q-btn flat label="Batalkan" color="primary" @click="showDeleteDialog = false" />
        <q-btn flat label="Hapus saja" color="negative" @click="deleteItem" :loading="deleting" />
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import { useCrud } from 'src/composables/useCrud'
import { useKeyboardShortcuts } from 'src/composables/useKeyboardShortcuts'

// CRUD Composable configuration
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
  baseApiUrl: '/api/pazaauto/jasa'
})

const splitterModel = ref(70)
const tableRef = ref(null)

// Form Data
const formData = ref({
  id: null,
  namaJasa: '',
  hargaJasa: null,
  estimasiWaktu: null,
  deskripsi: ''
})

const resetForm = () => {
  formData.value = {
    id: null,
    namaJasa: '',
    hargaJasa: null,
    estimasiWaktu: null,
    deskripsi: ''
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
    formData.value = { ...r }
  })
}

const handleSave = async () => {
  const result = await saveData(formData.value)
  if (result) {
    formData.value = { ...result }
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

// Table Columns
const columns = [
  {
    name: 'namaJasa',
    required: true,
    label: 'Nama Jasa',
    align: 'left',
    field: 'namaJasa',
    sortable: true
  },
  {
    name: 'hargaJasa',
    label: 'Harga Jasa',
    align: 'right',
    field: 'hargaJasa',
    sortable: true
  },
  {
    name: 'estimasiWaktu',
    label: 'Estimasi Waktu (menit)',
    align: 'center',
    field: 'estimasiWaktu',
    sortable: true
  },
  {
    name: 'deskripsi',
    label: 'Deskripsi',
    align: 'left',
    field: 'deskripsi'
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
