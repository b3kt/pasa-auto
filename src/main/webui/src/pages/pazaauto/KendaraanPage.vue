<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                      :on-create="openCreateDialog" ref="tableRef"
                      :on-edit="openEditDialog" create-label="Tambah data Kendaraan"
                      search-placeholder="Search...">
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
  </q-page>
</template>

<script setup>
import {ref, onMounted} from 'vue'
import {api} from 'boot/axios'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import {useCrud} from 'src/composables/useCrud'
import {useKeyboardShortcuts} from 'src/composables/useKeyboardShortcuts'

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
    // If we just saved a new item, we might want to stay in edit mode for it or reset
    // For now, let's keep it simple
  }
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
  }
]

// Lifecycle
onMounted(() => {
  fetchData()
  fetchDistinctMerks()
})
</script>

<style lang="sass" scoped>
</style>
