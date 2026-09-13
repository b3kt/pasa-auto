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

          <q-expansion-item v-if="isEditMode" icon="directions_car" label="Kendaraan Terdaftar" class="q-mt-md"
                            expand-icon-toggle default-closed>
            <div class="row items-center q-mb-sm q-gutter-sm">
              <q-space/>
              <q-btn label="Tambah Kendaraan" color="primary" dense unelevated icon="add" @click="openAttachDialog"/>
            </div>
            <q-table flat bordered :rows="vehicles" :columns="vehicleColumns" row-key="id"
                     :pagination="{rowsPerPage: 5}" :loading="loadingVehicles" class="bg-white"
                     :rows-per-page-options="[5]">
              <template v-slot:body-cell-current="props">
                <q-td :props="props">
                  <q-chip :color="props.value ? 'positive' : 'grey'" text-color="white" size="sm" dense>
                    {{ props.value ? 'Aktif' : 'Lama' }}
                  </q-chip>
                </q-td>
              </template>
              <template v-slot:no-data>
                <div class="text-grey q-pa-md">Belum ada kendaraan terdaftar.</div>
              </template>
            </q-table>
          </q-expansion-item>
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

    <!-- Attach Vehicle Dialog -->
    <GenericDialog v-model="showAttachDialog" title="Tambah Kendaraan" min-width="500px" position="standard">
      <q-form @submit="doAttachVehicle" class="q-gutter-md q-mt-sm">
        <q-input v-model="attachForm.nopol" label="Nopol *" outlined dense
                 :rules="[val => !!val || 'Nopol harus diisi']" hide-bottom-space/>
        <q-select v-model="attachForm.merk" label="Merk *" outlined dense use-input input-debounce="300"
                  new-value-mode="add-unique" :options="filteredMerkOptions" @filter="filterMerk"
                  :rules="[val => !!val || 'Merk harus diisi']" hide-bottom-space/>
        <q-input v-model="attachForm.jenis" label="Jenis" outlined dense/>
        <q-input v-model="attachForm.tanggalMulai" label="Tanggal Mulai" outlined dense type="date"/>
        <q-input v-model="attachForm.keterangan" label="Keterangan" outlined dense type="textarea" rows="2"/>
        <div class="row justify-end q-mt-md q-gutter-sm">
          <q-btn flat label="Batalkan" color="primary" @click="showAttachDialog = false"/>
          <q-btn label="Simpan" type="submit" color="primary" :loading="attaching"/>
        </div>
      </q-form>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import {ref, onMounted, watch} from 'vue'
import {useQuasar} from 'quasar'
import {api} from 'boot/axios'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import {useCrud} from 'src/composables/useCrud'
import { useKeyboardShortcuts } from 'src/composables/useKeyboardShortcuts'

const $q = useQuasar()

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
  },
  enableCache: true
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
  if (row?.id) {
    fetchVehicles(row.id)
  }
}

const handleSave = async () => {
  const result = await saveData(formData.value)
  if (result) {
    formData.value = {...result}
    if (!isEditMode.value) {
      openEditDialog(result)
      tableRef.value?.selectRowByItem(result)
    } else {
      fetchVehicles(result.id)
    }
  }
}

// Vehicles owned by this pelanggan (via tb_pelanggan_kendaraan)
const vehicles = ref([])
const loadingVehicles = ref(false)
const vehicleColumns = [
  {name: 'nopol', label: 'Nopol', field: 'nopol', sortable: true},
  {name: 'merk', label: 'Merk', field: 'merk'},
  {name: 'jenis', label: 'Jenis', field: 'jenis'},
  {name: 'tanggalMulai', label: 'Mulai', field: 'tanggalMulai'},
  {name: 'tanggalAkhir', label: 'Berakhir', field: 'tanggalAkhir'},
  {name: 'current', label: 'Status', field: 'current'}
]

const fetchVehicles = async (pelangganId) => {
  if (!pelangganId) return
  loadingVehicles.value = true
  try {
    const response = await api.get(`/api/pazaauto/pelanggan/${pelangganId}/vehicles`)
    if (response.data.success) {
      vehicles.value = response.data.data || []
    }
  } catch (error) {
    console.error('Failed to fetch pelanggan vehicles', error)
  } finally {
    loadingVehicles.value = false
  }
}

// Attach an additional vehicle
const showAttachDialog = ref(false)
const attaching = ref(false)
const attachForm = ref({
  nopol: '',
  merk: '',
  jenis: '',
  tanggalMulai: '',
  keterangan: ''
})

const openAttachDialog = () => {
  attachForm.value = {nopol: '', merk: '', jenis: '', tanggalMulai: '', keterangan: ''}
  filteredMerkOptions.value = merkOptions.value
  showAttachDialog.value = true
}

const doAttachVehicle = async () => {
  if (!formData.value.id) return
  attaching.value = true
  try {
    const body = {
      nopol: attachForm.value.nopol,
      merk: attachForm.value.merk || null,
      jenis: attachForm.value.jenis || null,
      tanggalMulai: attachForm.value.tanggalMulai || null,
      keterangan: attachForm.value.keterangan || null
    }
    const response = await api.post(`/api/pazaauto/pelanggan/${formData.value.id}/kendaraan`, body)
    if (response.data.success) {
      $q.notify({type: 'positive', message: response.data.message || 'Kendaraan ditambahkan'})
      showAttachDialog.value = false
      await fetchVehicles(formData.value.id)
    } else {
      $q.notify({type: 'negative', message: response.data.message || 'Gagal menambahkan kendaraan'})
    }
  } catch (error) {
    console.error('Failed to attach vehicle', error)
    $q.notify({type: 'negative', message: 'Gagal menambahkan kendaraan: ' + (error.response?.data?.message || error.message)})
  } finally {
    attaching.value = false
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
