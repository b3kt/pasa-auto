<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                      :on-create="openCreateDialog" ref="tableRef"
                      :on-edit="openEditDialog" :create-label="$t('create') + ' supplier'"
                      search-placeholder="Search by name or email...">
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6 q-mb-md">{{ isEditMode ? $t('edit') + ' Supplier' : $t('create') + ' Supplier' }}</div>
            <q-space/>
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>New</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="supplier-form" class="q-gutter-md">
            <q-input v-model="formData.namaSupplier" label="Nama Supplier *" outlined dense
                     :rules="[val => !!val || 'Nama Supplier harus diisi']" hide-bottom-space/>

            <q-input v-model="formData.email" label="Email" outlined dense type="email"/>
            <q-input v-model="formData.noTelepon" label="No Telepon" outlined dense/>

            <q-input v-model="formData.alamat" label="Alamat" outlined dense type="textarea" rows="2"/>

            <q-input v-model="formData.kontakPerson" label="Kontak Person" outlined dense/>
            <q-input v-model="formData.noHpKontak" label="No HP Kontak" outlined dense/>

            <q-input v-model="formData.keterangan" label="Keterangan" outlined dense type="textarea" rows="2"/>

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode" label="Hapus" color="negative" flat @click="confirmDelete(formData)"
                     :loading="deleting"/>
              <q-btn label="Simpan" type="submit" color="primary" :loading="saving"
                     :disable="isEditMode && !isDirty(formData)"/>
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" title="Konfirmasi hapus data" min-width="400px" position="standard">
      Are you sure you want to delete <strong>{{ itemToDelete?.namaSupplier }}</strong>?
      <template #actions>
        <q-btn flat label="Batalkan" color="primary" @click="showDeleteDialog = false"/>
        <q-btn flat label="Hapus saja" color="negative" @click="deleteItem" :loading="deleting"/>
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import {ref, onMounted} from 'vue'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import {useCrud} from 'src/composables/useCrud'
import {useKeyboardShortcuts} from 'src/composables/useKeyboardShortcuts'

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
  baseApiUrl: '/api/pazaauto/supplier',
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
  namaSupplier: '',
  email: '',
  noTelepon: '',
  alamat: '',
  kota: '',
  kodePos: '',
  kontakPerson: '',
  noHpKontak: '',
  keterangan: ''
})

const resetForm = () => {
  formData.value = {
    id: null,
    namaSupplier: '',
    email: '',
    noTelepon: '',
    alamat: '',
    kota: '',
    kodePos: '',
    kontakPerson: '',
    noHpKontak: '',
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

// Table Columns
const columns = [
  {
    name: 'namaSupplier',
    required: true,
    label: 'Nama Supplier',
    align: 'left',
    field: 'namaSupplier',
    sortable: true
  },
  {
    name: 'email',
    label: 'Email',
    align: 'left',
    field: 'email'
  },
  {
    name: 'noTelepon',
    label: 'No Telepon',
    align: 'left',
    field: 'noTelepon'
  },
  {
    name: 'kota',
    label: 'Kota',
    align: 'left',
    field: 'kota',
    sortable: true
  },
  {
    name: 'kontakPerson',
    label: 'Kontak Person',
    align: 'left',
    field: 'kontakPerson'
  }
]

// Lifecycle
onMounted(() => {
  fetchData()
})
</script>

<style lang="sass" scoped>
</style>
