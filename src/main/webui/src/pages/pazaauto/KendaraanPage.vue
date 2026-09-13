<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                      :on-create="openCreateDialog" ref="tableRef"
                      :on-edit="openEditDialog" create-label="Tambah data Kendaraan"
                      search-placeholder="Search...">
          <template v-slot:body-cell-actions="props">
            <q-btn flat dense round icon="group" color="primary" @click.stop="openOwnersDialog(props.row)">
              <q-tooltip>Riwayat pemilik kendaraan</q-tooltip>
            </q-btn>
          </template>
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6 q-mb-md">{{ isEditMode ? 'Edit Kendaraan' : 'Tambah data Kendaraan' }}</div>
            <q-space/>
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>New</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="kendaraan-form" class="q-gutter-md">
            <q-select v-model="formData.merk" label="Merk *" outlined dense :options="filteredMerkOptions" use-input
                      input-debounce="300" @filter="filterMerk" @new-value="createMerkValue" :loading="loadingMerk"
                      new-value-mode="add-unique" :rules="[val => !!val || 'Merk harus diisi']"
                      hide-bottom-space
            >
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    No results - type to add new
                  </q-item-section>
                </q-item>
              </template>
            </q-select>
            <q-input v-model="formData.jenis" label="Jenis *" outlined dense
                     :rules="[val => !!val || 'Jenis harus diisi']"
                     hide-bottom-space/>
            <q-input v-model="formData.model" label="Model" outlined dense/>
            <q-input v-model="formData.keterangan" label="Keterangan" outlined dense type="textarea" rows="2"/>

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
      Are you sure you want to delete this kendaraan?
      <template #actions>
        <q-btn flat label="Batalkan" color="primary" @click="showDeleteDialog = false"/>
        <q-btn flat label="Hapus saja" color="negative" @click="deleteItem" :loading="deleting"/>
      </template>
    </GenericDialog>

    <!-- Owner History Dialog -->
    <GenericDialog v-model="showOwnersDialog" title="Riwayat Pemilik Kendaraan" min-width="800px" position="drawer">
      <q-list v-if="owners.length === 0" class="text-grey">
        <q-item><q-item-section>Belum ada data kepemilikan.</q-item-section></q-item>
      </q-list>
      <q-table flat bordered :rows="owners" :columns="ownerColumns" row-key="id" :pagination="{rowsPerPage: 10}"
               :loading="loadingOwners">
        <template v-slot:body-cell-current="props">
          <q-td :props="props">
            <q-chip :color="props.value ? 'positive' : 'grey'" text-color="white" size="sm" dense>
              {{ props.value ? 'Aktif' : 'Lama' }}
            </q-chip>
          </q-td>
        </template>
        <template v-slot:body-cell-actions="props">
          <q-td :props="props">
            <q-btn v-if="props.row.current" flat dense color="primary" icon="swap_horiz"
                   label="Transfer" @click="openTransferDialog(props.row)"/>
          </q-td>
        </template>
      </q-table>
    </GenericDialog>

    <!-- Transfer Dialog -->
    <GenericDialog v-model="showTransferDialog" title="Transfer Kendaraan" min-width="500px" position="standard">
      <q-form @submit="doTransfer" class="q-gutter-md q-mt-sm">
        <div class="text-subtitle2">
          Transfer <strong>{{ transferForm.nopol }}</strong>
          ({{ transferMaster?.merk }} {{ transferMaster?.jenis }})
        </div>
        <q-select v-model="transferForm.idPelangganBaru" label="Pelanggan Tujuan *" outlined dense
                  :options="pelangganOptions" emit-value map-options use-input input-debounce="300"
                  @filter="filterPelanggan" option-label="label" option-value="value"
                  :rules="[val => !!val || 'Pilih pelanggan tujuan']"
                  hide-bottom-space/>
        <q-input v-model="transferForm.tanggalAwal" label="Tanggal Transfer" outlined dense type="date"
                 hint="Mulai tanggal ini, kendaraan menjadi milik pelanggan tujuan."/>
        <q-input v-model="transferForm.keterangan" label="Keterangan (alasan)" outlined dense type="textarea" rows="2"/>
        <div class="row justify-end q-mt-md q-gutter-sm">
          <q-btn flat label="Batalkan" color="primary" @click="showTransferDialog = false"/>
          <q-btn label="Transfer" type="submit" color="primary" :loading="transferring"/>
        </div>
      </q-form>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue'
import {useQuasar} from 'quasar'
import {api} from 'boot/axios'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import {useCrud} from 'src/composables/useCrud'
import {useKeyboardShortcuts} from 'src/composables/useKeyboardShortcuts'

const $q = useQuasar()

// CRUD Composable configuration
const {
  rows,
  loading,
  saving,
  deleting,
  showDeleteDialog,
  isEditMode,
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
  baseApiUrl: '/api/pazaauto/kendaraan',
  onSuccess: () => {
  },
  enableCache: true
})

const splitterModel = ref(60)
const tableRef = ref(null)

// Merk Logic
const loadingMerk = ref(false)
const merkOptions = ref([])
const filteredMerkOptions = ref([])

const fetchDistinctMerks = async () => {
  loadingMerk.value = true
  try {
    const response = await api.get('/api/pazaauto/kendaraan/merk/distinct')
    if (response.data.success) {
      merkOptions.value = response.data.data || []
      filteredMerkOptions.value = merkOptions.value
    }
  } catch (error) {
    console.error('Failed to fetch merk options', error)
  } finally {
    loadingMerk.value = false
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

const createMerkValue = (val, done) => {
  if (val.length > 0) {
    if (!merkOptions.value.includes(val)) {
      merkOptions.value.push(val)
    }
    done(val, 'add-unique')
  }
}

// Form Data
const formData = ref({
  id: null,
  merk: '',
  jenis: '',
  model: '',
  keterangan: ''
})

const resetForm = () => {
  formData.value = {
    id: null,
    merk: '',
    jenis: '',
    model: '',
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
    if (isEditMode.value) {
      // Updated initialData is already handled in useCrud
    } else {
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
    name: 'merk',
    required: true,
    label: 'Merk',
    align: 'left',
    field: 'merk',
    sortable: true
  },
  {
    name: 'jenis',
    required: true,
    label: 'Jenis',
    align: 'left',
    field: 'jenis',
    sortable: true
  },
  {
    name: 'model',
    label: 'Model',
    align: 'left',
    field: 'model',
    sortable: true
  },
  {
    name: 'actions',
    label: '',
    align: 'right',
    field: 'actions'
  }
]

// Owner History
const showOwnersDialog = ref(false)
const owners = ref([])
const loadingOwners = ref(false)

const ownerColumns = [
  {name: 'nopol', label: 'Nopol', field: 'nopol', sortable: true},
  {name: 'namaPelanggan', label: 'Pelanggan', field: 'namaPelanggan', sortable: true},
  {name: 'merk', label: 'Merk', field: 'merk'},
  {name: 'jenis', label: 'Jenis', field: 'jenis'},
  {name: 'tanggalMulai', label: 'Mulai', field: 'tanggalMulai'},
  {name: 'tanggalAkhir', label: 'Berakhir', field: 'tanggalAkhir'},
  {name: 'current', label: 'Status', field: 'current'},
  {name: 'keterangan', label: 'Keterangan', field: 'keterangan'},
  {name: 'actions', label: '', field: 'actions'}
]

const openOwnersDialog = async (row) => {
  showOwnersDialog.value = true
  loadingOwners.value = true
  owners.value = []
  try {
    const response = await api.get(`/api/pazaauto/kendaraan/${row.id}/owners`)
    if (response.data.success) {
      owners.value = response.data.data || []
    } else {
      $q.notify({type: 'negative', message: response.data.message || 'Gagal memuat riwayat pemilik'})
    }
  } catch (error) {
    console.error('Failed to fetch owner history', error)
    $q.notify({type: 'negative', message: 'Gagal memuat riwayat pemilik'})
  } finally {
    loadingOwners.value = false
  }
}
const showTransferDialog = ref(false)
const transferring = ref(false)
const transferMaster = ref(null)
const allPelanggan = ref([])
const filteredPelanggan = ref([])
const transferForm = ref({
  nopol: '',
  idPelangganBaru: null,
  tanggalAwal: '',
  keterangan: ''
})

const pelangganOptions = computed(() => filteredPelanggan.value)

const filterPelanggan = (val, update) => {
  update(() => {
    if (val === '') {
      filteredPelanggan.value = allPelanggan.value
    } else {
      const needle = val.toLowerCase()
      filteredPelanggan.value = allPelanggan.value.filter(p =>
        p.label.toLowerCase().indexOf(needle) > -1
      )
    }
  })
}

const fetchPelanggan = async () => {
  try {
    const response = await api.get('/api/pazaauto/pelanggan')
    if (response.data.success) {
      allPelanggan.value = (response.data.data || []).map(p => ({
        value: p.id,
        label: `${p.namaPelanggan} (${p.nopol || '-'})`
      }))
      filteredPelanggan.value = allPelanggan.value
    }
  } catch (error) {
    console.error('Failed to fetch pelanggan', error)
  }
}

const openTransferDialog = async (row) => {
  transferMaster.value = row
  transferForm.value = {
    nopol: row.nopol,
    idPelangganBaru: null,
    tanggalAwal: '',
    keterangan: ''
  }
  if (allPelanggan.value.length === 0) {
    await fetchPelanggan()
  } else {
    filteredPelanggan.value = allPelanggan.value
  }
  showTransferDialog.value = true
}

const doTransfer = async () => {
  transferring.value = true
  try {
    const body = {
      nopol: transferForm.value.nopol,
      idPelangganBaru: transferForm.value.idPelangganBaru,
      tanggalAwal: transferForm.value.tanggalAwal || null,
      keterangan: transferForm.value.keterangan || null
    }
    const response = await api.post('/api/pazaauto/kendaraan/transfer', body)
    if (response.data.success) {
      $q.notify({type: 'positive', message: response.data.message || 'Kendaraan ditransfer'})
      showTransferDialog.value = false
      if (transferMaster.value) {
        const reload = await api.get(`/api/pazaauto/kendaraan/${transferMaster.value.idKendaraan}/owners`)
        if (reload.data.success) {
          owners.value = reload.data.data || []
        }
      }
    } else {
      $q.notify({type: 'negative', message: response.data.message || 'Transfer gagal'})
    }
  } catch (error) {
    console.error('Failed to transfer', error)
    $q.notify({type: 'negative', message: 'Transfer gagal: ' + (error.response?.data?.message || error.message)})
  } finally {
    transferring.value = false
  }
}

// Lifecycle
onMounted(() => {
  fetchData()
  fetchDistinctMerks()
})
</script>

<style lang="sass" scoped>
</style>