<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                      :on-create="openCreateDialog"
                      :on-edit="openEditDialog" create-label="Tambah data Karyawan" ref="tableRef"
                      search-placeholder="Search by name or email...">
          <template v-slot:body-cell-jenisKelamin="props">
              <q-badge :color="props.row.jenisKelamin === 'L' ? 'blue' : 'pink'">
                {{ props.row.jenisKelamin === 'L' ? 'Laki-laki' : 'Perempuan' }}
              </q-badge>
          </template>
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6 q-mb-md">{{ isEditMode ? 'Edit Karyawan' : 'Tambah data Karyawan' }}</div>
            <q-space/>
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>New</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="karyawan-form" class="q-gutter-md">
            <q-input v-model="formData.namaKaryawan" label="Nama Karyawan *" outlined dense
                     :rules="[val => !!val || 'Nama Karyawan harus diisi']"
                     hide-bottom-space/>

            <q-select v-model="formData.idPosisi" label="Posisi *" outlined dense :options="filteredPosisiOptions"
                      option-label="posisi" option-value="id" emit-value map-options use-input input-debounce="300"
                      @filter="filterPosisi" :rules="[val => !!val || 'Posisi harus diisi']" :loading="loadingPosisi"
                      hide-bottom-space>
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    No results
                  </q-item-section>
                </q-item>
              </template>
            </q-select>

            <q-input v-model="formData.alamat" label="Alamat" outlined dense type="textarea" rows="2"/>

            <q-input v-model="formData.tanggalBergabung" label="Tanggal Bergabung" outlined dense type="date"/>

            <q-input v-model="formData.email" label="Email" outlined dense type="email"/>
            <q-input v-model="formData.noTelepon" label="No Telepon" outlined dense/>

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode" label="Hapus" color="negative" flat @click="confirmDelete(formData)"
                     :loading="deleting"/>
              <q-btn label="Simpan" type="submit" color="primary" :loading="saving" :disable="isEditMode && !isDirty(formData)"/>
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" title="Konfirmasi hapus data" min-width="400px" position="standard">
      Are you sure you want to delete <strong>{{ itemToDelete?.namaKaryawan }}</strong>?
      <template #actions>
        <q-btn flat label="Batalkan" color="primary" @click="showDeleteDialog = false"/>
        <q-btn flat label="Hapus saja" color="negative" @click="deleteItem" :loading="deleting"/>
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import {ref, onMounted} from 'vue'
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
  baseApiUrl: '/api/pazaauto/karyawan'
})

const splitterModel = ref(70)
const tableRef = ref(null)

// Posisi Logic
const loadingPosisi = ref(false)
const posisiOptions = ref([])
const filteredPosisiOptions = ref([])

const fetchPosisi = async () => {
  loadingPosisi.value = true
  try {
    const response = await api.get('/api/pazaauto/karyawan-posisi')
    if (response.data.success) {
      posisiOptions.value = response.data.data || []
      filteredPosisiOptions.value = posisiOptions.value
    }
  } catch (error) {
    console.error('Failed to fetch posisi data', error)
  } finally {
    loadingPosisi.value = false
  }
}

const filterPosisi = (val, update) => {
  update(() => {
    if (val === '') {
      filteredPosisiOptions.value = posisiOptions.value
    } else {
      const needle = val.toLowerCase()
      filteredPosisiOptions.value = posisiOptions.value.filter(
        v => v.posisi.toLowerCase().indexOf(needle) > -1
      )
    }
  })
}

// Roles Logic
const loadingRoles = ref(false)
const roleOptions = ref([])

const fetchRoles = async () => {
  loadingRoles.value = true
  try {
    const response = await api.get('/api/roles')
    if (response.data.success) {
      roleOptions.value = response.data.data || []
    }
  } catch (error) {
    console.error('Failed to fetch roles', error)
  } finally {
    loadingRoles.value = false
  }
}

// Form Data
const formData = ref({
  id: null,
  namaKaryawan: '',
  email: '',
  noTelepon: '',
  alamat: '',
  jenisKelamin: null,
  tanggalBergabung: null,
  idPosisi: null,
  roles: ['user']
})

const resetForm = () => {
  formData.value = {
    id: null,
    namaKaryawan: '',
    email: '',
    noTelepon: '',
    alamat: '',
    jenisKelamin: null,
    tanggalBergabung: null,
    idPosisi: null,
    roles: ['user']
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

// Table Columns
const columns = [
  {
    name: 'namaKaryawan',
    required: true,
    label: 'Nama Karyawan',
    align: 'left',
    field: 'namaKaryawan',
    sortable: true
  },
  {
    name: 'alamat',
    required: true,
    label: 'Alamat',
    align: 'left',
    field: 'alamat',
    sortable: true
  },
  {
    name: 'idPosisi',
    label: 'Posisi',
    align: 'center',
    field: 'namePosisi',
    sortable: true
  },
  {
    name: 'tanggalBergabung',
    label: 'Tanggal Bergabung',
    align: 'center',
    field: 'tanggalBergabung',
    sortable: true
  },
  {
    name: 'noTelepon',
    label: 'No Telepon',
    align: 'left',
    field: 'noTelepon'
  }
]

// Lifecycle
onMounted(() => {
  fetchData()
  fetchPosisi()
  fetchRoles()
})
</script>

<style lang="sass" scoped>
</style>
