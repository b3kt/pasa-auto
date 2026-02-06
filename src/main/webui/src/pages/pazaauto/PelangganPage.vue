<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                      :on-create="openCreateDialog" ref="tableRef"
                      :on-edit="openEditDialog" create-label="Tambah data Pelanggan"
                      search-placeholder="Search by name, nopol, or email...">
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6 q-mb-md">{{ isEditMode ? 'Edit Pelanggan' : 'Tambah data Pelanggan' }}</div>
            <q-space/>
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>New</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="pelanggan-form" class="q-gutter-md">

            <q-input v-model="formData.nopol" label="Nopol *" outlined dense
                     :rules="[val => !!val || 'Nopol harus diisi']"
                     hide-bottom-space/>
            <q-input v-model="formData.namaPelanggan" label="Nama Pelanggan *" outlined dense
                     :rules="[val => !!val || 'Nama Pelanggan harus diisi']"
                     hide-bottom-space/>
            <q-input v-model="formData.email" label="Email" outlined dense type="email"/>
            <q-input v-model="formData.noHp" label="No HP" outlined dense/>
            <q-input v-model="formData.alamat" label="Alamat" outlined dense type="textarea" rows="2"/>
            <q-input v-model="formData.kota" label="Kota" outlined dense/>
            <q-input v-model="formData.tanggalJoin" label="Tanggal Join" outlined dense type="date"/>

            <div class="text-subtitle2 q-mt-md">Informasi Kendaraan</div>
            <q-select v-model="formData.merk" label="Merk *" outlined dense use-input input-debounce="300"
                      new-value-mode="add-unique" :options="filteredMerkOptions" @filter="filterMerk"
                      :rules="[val => !!val || 'Merk harus diisi']"
                      hide-bottom-space/>
            <q-select v-model="formData.jenis" label="Jenis" outlined dense use-input input-debounce="300"
                      new-value-mode="add-unique" :options="filteredJenisOptions" @filter="filterJenis"/>

            <q-input v-model="formData.keterangan" label="Keterangan" outlined dense type="textarea" rows="2"/>

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode" label="Hapus" color="negative" flat @click="confirmDelete(formData)" :loading="deleting" />
              <q-btn label="Simpan" type="submit" color="primary" :loading="saving" :disable="isEditMode && !isDirty(formData)"/>
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" title="Konfirmasi hapus data" min-width="400px" position="standard">
      Are you sure you want to delete <strong>{{ itemToDelete?.namaPelanggan }}</strong>?
      <template #actions>
        <q-btn flat label="Batalkan" color="primary" @click="showDeleteDialog = false"/>
        <q-btn flat label="Hapus saja" color="negative" @click="deleteItem" :loading="deleting"/>
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import {ref, onMounted, watch} from 'vue'
import {api} from 'boot/axios'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import {useCrud} from 'src/composables/useCrud'
import { useKeyboardShortcuts } from 'src/composables/useKeyboardShortcuts'

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
  baseApiUrl: '/api/pazaauto/pelanggan',
  defaultPagination: {
    sortBy: null,
    descending: false,
    page: 1,
    rowsPerPage: 10,
    rowsNumber: 0
  }
})

const splitterModel = ref(70)
const tableRef = ref(null)

// Form Data
const formData = ref({
  id: null,
  nopol: '',
  namaPelanggan: '',
  email: '',
  noHp: '',
  alamat: '',
  kota: '',
  kodePos: '',
  jenisKelamin: null,
  tanggalJoin: null,
  merk: '',
  jenis: '',
  keterangan: ''
})

const resetForm = () => {
  formData.value = {
    id: null,
    nopol: '',
    namaPelanggan: '',
    email: '',
    noHp: '',
    alamat: '',
    kota: '',
    kodePos: '',
    jenisKelamin: null,
    tanggalJoin: null,
    merk: '',
    jenis: '',
    keterangan: ''
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

// Autocomplete Logic
const merkOptions = ref([])
const filteredMerkOptions = ref([])
const jenisOptions = ref([])
const filteredJenisOptions = ref([])

const fetchAutocompleteData = async () => {
  try {
    const [merkRes, jenisRes] = await Promise.all([
      api.get('/api/pazaauto/kendaraan/merk/distinct'),
      api.get('/api/pazaauto/kendaraan/jenis/distinct')
    ])

    if (merkRes.data.success) {
      merkOptions.value = merkRes.data.data
      filteredMerkOptions.value = merkOptions.value
    }

    if (jenisRes.data.success) {
      jenisOptions.value = jenisRes.data.data
      filteredJenisOptions.value = jenisOptions.value
    }
  } catch (error) {
    console.error('Failed to fetch autocomplete data', error)
  }
}

const filterMerk = (val, update) => {
  update(() => {
    if (val === '') {
      filteredMerkOptions.value = merkOptions.value
    } else {
      const needle = val.toLowerCase()
      filteredMerkOptions.value = merkOptions.value.filter(
        v => v.toLowerCase().indexOf(needle) > -1
      )
    }
  })
}

const filterJenis = (val, update) => {
  update(() => {
    if (val === '') {
      filteredJenisOptions.value = jenisOptions.value
    } else {
      const needle = val.toLowerCase()
      filteredJenisOptions.value = jenisOptions.value.filter(
        v => v.toLowerCase().indexOf(needle) > -1
      )
    }
  })
}

const fetchFilteredJenis = async (merk) => {
  try {
    const response = await api.get('/api/pazaauto/kendaraan/jenis/by-merk', {
      params: {merk}
    })
    if (response.data.success) {
      jenisOptions.value = response.data.data
      filteredJenisOptions.value = jenisOptions.value
    }
  } catch (error) {
    console.error('Failed to fetch filtered jenis', error)
  }
}

watch(() => formData.value.merk, (newMerk) => {
  if (newMerk) {
    fetchFilteredJenis(newMerk)
  } else {
    // If merk cleared, reset to all distinct jenis (re-fetch or use initial if plausible, here re-fetch is safest)
    fetchAutocompleteData()
  }
})

// Table Columns
const columns = [
  {
    name: 'nopol',
    required: true,
    label: 'Nopol',
    align: 'left',
    field: 'nopol',
    sortable: true
  },
  {
    name: 'namaPelanggan',
    required: true,
    label: 'Nama Pelanggan',
    align: 'left',
    field: 'namaPelanggan',
    sortable: true
  },
  {
    name: 'email',
    label: 'Email',
    align: 'left',
    field: 'email'
  },
  {
    name: 'noHp',
    label: 'No HP',
    align: 'left',
    field: 'noHp'
  },
  {
    name: 'merk',
    label: 'Merk',
    align: 'left',
    field: 'merk',
    sortable: true
  },
  {
    name: 'jenis',
    label: 'Jenis',
    align: 'left',
    field: 'jenis'
  }
]

// Lifecycle
onMounted(() => {
  fetchData()
  fetchAutocompleteData()
})
</script>

<style lang="sass" scoped>
</style>
