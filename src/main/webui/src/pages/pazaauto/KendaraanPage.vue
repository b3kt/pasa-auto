<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <q-splitter v-model="tableSplitterModel" horizontal :limits="[20, 80]" style="height: 100%">
          <template v-slot:before>
            <!-- Merk Kendaraan (master) -->
            <GenericTable :rows="merkRows" :columns="merkColumns" :loading="merkLoading" :pagination="merkPagination"
                          @update:pagination="merkPagination = $event" @request="merkOnRequest" @search="merkOnSearch"
                          :on-edit="openMerkEdit" ref="merkTableRef" :table-height="merkTableHeight"
                          search-placeholder="Cari merk...">
              <template v-slot:title>
              <div class="row items-center q-px-sm">
                <div class="text-h6 q-mb-md">Master | Merk Kendaraan</div>
                  <q-space/>
                <q-btn flat dense icon="add" label="Merk" color="primary" @click="openMerkCreate"/>
              </div>


              </template>
            </GenericTable>
          </template>

          <template v-slot:after>
            <!-- Kendaraan (detail, filtered by selected merk) -->
            <GenericTable :key="selectedMerk?.id ?? 'none'" :rows="kendaraanRows" :columns="kendaraanColumns"
                          :loading="kendaraanLoading" :pagination="kendaraanPagination"
                          @update:pagination="kendaraanPagination = $event" @request="onKendaraanRequest"
                          @search="onKendaraanSearch" :on-edit="openKendaraanEdit" ref="kendaraanTableRef"
                          :table-height="kendaraanTableHeight" search-placeholder="Cari jenis / model...">
              <template v-slot:title>
              <div class="row items-center q-px-sm q-pt-md">

                <div class="text-h6 q-mb-md">Master | Jenis Kendaraan |
                  <span v-if="selectedMerk" class="text-primary"> {{ selectedMerk.nama }}</span>
                </div>
                <q-space/>
                <q-btn flat dense icon="add" label="Kendaraan" color="primary" :disable="!selectedMerk"
                       @click="openKendaraanCreate">
                  <q-tooltip v-if="!selectedMerk">Pilih merk terlebih dahulu</q-tooltip>
                </q-btn>
              </div>

              </template>
              <template v-slot:no-data>
                <div class="full-width text-center text-grey q-pa-md">
                  {{ selectedMerk ? 'Belum ada kendaraan untuk merk ini' : 'Pilih merk untuk melihat kendaraan' }}
                </div>
              </template>
            </GenericTable>
          </template>
        </q-splitter>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <!-- Merk form -->
          <template v-if="activeForm === 'merk'">
            <div class="row items-center q-mb-md">
              <div class="text-h6">{{ merkIsEditMode ? 'Edit Merk Kendaraan' : 'Tambah Merk Kendaraan' }}</div>
              <q-space/>
              <q-btn v-if="merkIsEditMode" flat round dense icon="add" @click="openMerkCreate">
                <q-tooltip>Merk baru</q-tooltip>
              </q-btn>
            </div>
            <q-form @submit="handleMerkSave" class="q-gutter-md">
              <q-input v-model="merkForm.nama" label="Nama Merk *" outlined dense maxlength="50"
                       hint="Disimpan dalam huruf kapital, mis. HONDA"
                       :rules="[val => !!val && !!val.trim() || 'Nama merk harus diisi']"/>
              <q-input v-model="merkForm.keterangan" label="Keterangan" outlined dense type="textarea" rows="2"/>

              <div class="row justify-end q-mt-md q-gutter-sm">
                <q-btn v-if="merkIsEditMode" label="Hapus" color="negative" flat
                       @click="merkConfirmDelete(merkForm)" :loading="merkDeleting"/>
                <q-btn label="Simpan" type="submit" color="primary" :loading="merkSaving"
                       :disable="merkIsEditMode && !merkIsDirty(merkForm)"/>
              </div>
            </q-form>
          </template>

          <!-- Kendaraan form -->
          <template v-else>
            <div class="row items-center q-mb-md">
              <div class="text-h6">{{ kendaraanIsEditMode ? 'Edit Kendaraan' : 'Tambah data Kendaraan' }}</div>
              <q-space/>
              <q-btn v-if="kendaraanIsEditMode" flat round dense icon="add" @click="openKendaraanCreate">
                <q-tooltip>Kendaraan baru</q-tooltip>
              </q-btn>
            </div>
            <q-form @submit="handleKendaraanSave" class="q-gutter-md">
              <q-select v-model="kendaraanForm.merkId" label="Merk *" outlined dense
                        :options="merkOptions" option-value="id" option-label="nama" emit-value map-options
                        :disable="kendaraanIsEditMode"
                        :rules="[val => !!val || 'Merk harus dipilih']" hide-bottom-space/>
              <q-input v-model="kendaraanForm.jenis" label="Jenis *" outlined dense
                       :rules="[val => !!val || 'Jenis harus diisi']" hide-bottom-space/>
              <q-input v-model="kendaraanForm.model" label="Model" outlined dense/>
              <q-input v-model="kendaraanForm.keterangan" label="Keterangan" outlined dense type="textarea" rows="2"/>

              <div class="row justify-end q-mt-md q-gutter-sm">
                <q-btn v-if="kendaraanIsEditMode" label="Hapus" color="negative" flat
                       @click="kendaraanConfirmDelete(kendaraanForm)" :loading="kendaraanDeleting"/>
                <q-btn label="Simpan" type="submit" color="primary" :loading="kendaraanSaving"
                       :disable="kendaraanIsEditMode && !kendaraanIsDirty(kendaraanForm)"/>
              </div>
            </q-form>
          </template>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialogs -->
    <GenericDialog v-model="merkShowDeleteDialog" title="Konfirmasi hapus data" min-width="400px" position="standard">
      Hapus merk {{ merkForm.nama }}? Merk yang masih memiliki kendaraan tidak dapat dihapus.
      <template #actions>
        <q-btn flat label="Batalkan" color="primary" @click="merkShowDeleteDialog = false"/>
        <q-btn flat label="Hapus saja" color="negative" @click="deleteMerk" :loading="merkDeleting"/>
      </template>
    </GenericDialog>

    <GenericDialog v-model="kendaraanShowDeleteDialog" title="Konfirmasi hapus data" min-width="400px"
                   position="standard">
      Are you sure you want to delete this kendaraan?
      <template #actions>
        <q-btn flat label="Batalkan" color="primary" @click="kendaraanShowDeleteDialog = false"/>
        <q-btn flat label="Hapus saja" color="negative" @click="deleteKendaraan" :loading="kendaraanDeleting"/>
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import {ref, computed, watch, onMounted} from 'vue'
import {api} from 'boot/axios'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import {useCrud} from 'src/composables/useCrud'
import {useKeyboardShortcuts} from 'src/composables/useKeyboardShortcuts'

const MERK_API = '/api/pazaauto/merk-kendaraan'
const KENDARAAN_API = '/api/pazaauto/kendaraan'

const splitterModel = ref(60)
// Horizontal split between the merk (top) and kendaraan (bottom) tables, in percent
const tableSplitterModel = ref(40)

// Each table fills its splitter pane: pane height minus section header + GenericTable padding
// (the search toolbar lives inside the table's top bar, so it's part of the table height)
const PANE_CHROME = '80px'
const paneTableHeight = (percent) => `calc((100vh - 100px) * ${percent / 100} - ${PANE_CHROME})`
const merkTableHeight = computed(() => paneTableHeight(tableSplitterModel.value))
const kendaraanTableHeight = computed(() => paneTableHeight(100 - tableSplitterModel.value))
const merkTableRef = ref(null)
const kendaraanTableRef = ref(null)

// Which entity the right-hand form is editing
const activeForm = ref('merk')
const selectedMerk = ref(null)

// ── Merk (master) ──────────────────────────────────────────────────────────
const {
  rows: merkRows,
  loading: merkLoading,
  saving: merkSaving,
  deleting: merkDeleting,
  showDeleteDialog: merkShowDeleteDialog,
  isEditMode: merkIsEditMode,
  pagination: merkPagination,
  fetchData: merkFetchData,
  onRequest: merkOnRequest,
  onSearch: merkOnSearch,
  saveData: merkSaveData,
  confirmDelete: merkConfirmDelete,
  deleteItem: merkDeleteItem,
  openCreateDialog: merkOpenCreate,
  openEditDialog: merkOpenEdit,
  isDirty: merkIsDirty
} = useCrud({baseApiUrl: MERK_API, enableCache: true})

const emptyMerk = () => ({id: null, nama: '', keterangan: ''})
const merkForm = ref(emptyMerk())

// All merk, for the kendaraan form's merk select
const merkOptions = ref([])
const fetchMerkOptions = async () => {
  try {
    const response = await api.get(MERK_API)
    if (response.data.success) {
      merkOptions.value = response.data.data || []
    }
  } catch (error) {
    console.error('Failed to fetch merk options', error)
  }
}

const openMerkCreate = () => {
  activeForm.value = 'merk'
  merkOpenCreate(() => {
    merkForm.value = emptyMerk()
  })
}

const openMerkEdit = (row) => {
  activeForm.value = 'merk'
  merkOpenEdit(row, (r) => {
    merkForm.value = {...r}
  })
  selectMerk(row)
}

const handleMerkSave = async () => {
  const wasEdit = merkIsEditMode.value
  const result = await merkSaveData(merkForm.value)
  if (result) {
    await fetchMerkOptions()
    openMerkEdit(result)
    if (!wasEdit) {
      merkTableRef.value?.selectRowByItem(result)
    }
  }
}

const deleteMerk = async () => {
  const deletedId = merkForm.value.id
  const success = await merkDeleteItem()
  if (success) {
    await fetchMerkOptions()
    if (selectedMerk.value?.id === deletedId) {
      selectMerk(null)
    }
    if (merkRows.value.length > 0) {
      openMerkEdit(merkRows.value[0])
      merkTableRef.value?.selectRow(0)
    } else {
      openMerkCreate()
    }
  }
}

// ── Kendaraan (detail, filtered by selected merk) ─────────────────────────
const {
  rows: kendaraanRows,
  loading: kendaraanLoading,
  saving: kendaraanSaving,
  deleting: kendaraanDeleting,
  showDeleteDialog: kendaraanShowDeleteDialog,
  isEditMode: kendaraanIsEditMode,
  pagination: kendaraanPagination,
  searchText: kendaraanSearchText,
  fetchData: kendaraanFetchData,
  onRequest: kendaraanOnRequest,
  onSearch: kendaraanOnSearch,
  saveData: kendaraanSaveData,
  confirmDelete: kendaraanConfirmDelete,
  deleteItem: kendaraanDeleteItem,
  openCreateDialog: kendaraanOpenCreate,
  openEditDialog: kendaraanOpenEdit,
  isDirty: kendaraanIsDirty
} = useCrud({
  baseApiUrl: KENDARAAN_API,
  enableCache: true,
  extraParams: () => (selectedMerk.value ? {merkId: selectedMerk.value.id} : {})
})

const emptyKendaraan = () => ({
  id: null,
  merkId: selectedMerk.value?.id ?? null,
  jenis: '',
  model: '',
  keterangan: ''
})
const kendaraanForm = ref(emptyKendaraan())

// Without a selected merk the kendaraan table stays empty instead of listing everything
const onKendaraanRequest = (props) => {
  if (selectedMerk.value) kendaraanOnRequest(props)
}
const onKendaraanSearch = (val) => {
  if (selectedMerk.value) kendaraanOnSearch(val)
}

const selectMerk = (merk) => {
  if (selectedMerk.value?.id === merk?.id) {
    selectedMerk.value = merk
    return
  }
  selectedMerk.value = merk
  kendaraanSearchText.value = ''
  kendaraanPagination.value.page = 1
  kendaraanRows.value = []
  if (merk) kendaraanFetchData()
}

const openKendaraanCreate = () => {
  if (!selectedMerk.value) return
  activeForm.value = 'kendaraan'
  kendaraanOpenCreate(() => {
    kendaraanForm.value = emptyKendaraan()
  })
}

const openKendaraanEdit = (row) => {
  activeForm.value = 'kendaraan'
  kendaraanOpenEdit(row, (r) => {
    kendaraanForm.value = {...r}
  })
}

const handleKendaraanSave = async () => {
  const wasEdit = kendaraanIsEditMode.value
  const result = await kendaraanSaveData(kendaraanForm.value)
  if (result) {
    kendaraanForm.value = {...result}
    if (!wasEdit) {
      openKendaraanEdit(result)
      kendaraanTableRef.value?.selectRowByItem(result)
    }
  }
}

const deleteKendaraan = async () => {
  const success = await kendaraanDeleteItem()
  if (success) {
    openKendaraanCreate()
  }
}

// Auto-select the first merk once the master table has data, matching the
// row GenericTable highlights by default.
watch(merkRows, (rows) => {
  if (!selectedMerk.value && rows && rows.length > 0) {
    openMerkEdit(rows[0])
  }
})

// Keyboard Shortcuts act on whichever form is active
useKeyboardShortcuts({
  onSave: () => {
    if (activeForm.value === 'merk') {
      if (!merkSaving.value && !(merkIsEditMode.value && !merkIsDirty(merkForm.value))) handleMerkSave()
    } else if (!kendaraanSaving.value && !(kendaraanIsEditMode.value && !kendaraanIsDirty(kendaraanForm.value))) {
      handleKendaraanSave()
    }
  },
  onDelete: () => {
    if (activeForm.value === 'merk') {
      if (merkIsEditMode.value && !merkDeleting.value) merkConfirmDelete(merkForm.value)
    } else if (kendaraanIsEditMode.value && !kendaraanDeleting.value) {
      kendaraanConfirmDelete(kendaraanForm.value)
    }
  },
  onNew: () => {
    if (activeForm.value === 'kendaraan') {
      openKendaraanCreate()
    } else {
      openMerkCreate()
    }
  }
})

// Table Columns
const merkColumns = [
  {name: 'nama', required: true, label: 'Merk', align: 'left', field: 'nama', sortable: true},
  {name: 'keterangan', label: 'Keterangan', align: 'left', field: 'keterangan', sortable: true}
]

const kendaraanColumns = [
  {name: 'jenis', required: true, label: 'Jenis', align: 'left', field: 'jenis', sortable: true},
  {name: 'model', label: 'Model', align: 'left', field: 'model', sortable: true},
  {name: 'keterangan', label: 'Keterangan', align: 'left', field: 'keterangan', sortable: true}
]

// Lifecycle
onMounted(() => {
  merkFetchData()
  fetchMerkOptions()
})
</script>

<style lang="sass" scoped>
</style>
