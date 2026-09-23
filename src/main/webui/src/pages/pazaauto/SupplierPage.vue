<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                      :on-create="openCreateDialog" ref="tableRef"
                      :on-edit="openEditDialog" :create-label="$t('create') + ' Supplier'"
                      :search-placeholder="$t('pages.supplierPage.searchPlaceholder')">
          <template v-slot:title>
            <div class="text-h6 q-mb-md">{{ $t('pages.supplierPage.title') }}</div>
          </template>
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6 q-mb-md">{{ isEditMode ? $t('edit') + ' Supplier' : $t('create') + ' Supplier' }}</div>
            <q-space/>
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>{{ $t('new') }}</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="supplier-form" class="q-gutter-md">
            <q-input v-model="formData.namaSupplier" :label="$t('pages.supplierPage.nameLabel')" outlined dense
                     :rules="[val => !!val || $t('pages.supplierPage.nameRequired')]" hide-bottom-space/>

            <q-input v-model="formData.email" :label="$t('email')" outlined dense type="email"/>
            <q-input v-model="formData.noTelepon" :label="$t('pages.supplierPage.phoneLabel')" outlined dense/>

            <q-input v-model="formData.alamat" :label="$t('pages.supplierPage.addressLabel')" outlined dense type="textarea" rows="2"/>

            <q-input v-model="formData.kontakPerson" :label="$t('pages.supplierPage.contactPersonLabel')" outlined dense/>
            <q-input v-model="formData.noHpKontak" :label="$t('pages.supplierPage.contactPhoneLabel')" outlined dense/>

            <q-input v-model="formData.keterangan" :label="$t('notes')" outlined dense type="textarea" rows="2"/>

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode" :label="$t('delete')" color="negative" flat @click="confirmDelete(formData)"
                     :loading="deleting"/>
              <q-btn :label="$t('save')" type="submit" color="primary" :loading="saving"
                     :disable="isEditMode && !isDirty(formData)"/>
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" :title="$t('confirmDeleteTitle')" min-width="400px" position="standard">
      {{ $t('pages.supplierPage.confirmDeleteMessage', { item: itemToDelete?.namaSupplier }) }}
      <template #actions>
        <q-btn flat :label="$t('cancel')" color="primary" @click="showDeleteDialog = false"/>
        <q-btn flat :label="$t('deleteOnlyButton')" color="negative" @click="deleteItem" :loading="deleting"/>
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue'
import {useI18n} from 'vue-i18n'
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
  },
  enableCache: true
})

const { t } = useI18n()
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
const columns = computed(() => [
  {
    name: 'namaSupplier',
    required: true,
    label: t('pages.supplierPage.nameColumn'),
    align: 'left',
    field: 'namaSupplier',
    sortable: true
  },
  {
    name: 'email',
    label: t('email'),
    align: 'left',
    field: 'email'
  },
  {
    name: 'noTelepon',
    label: t('pages.supplierPage.phoneColumn'),
    align: 'left',
    field: 'noTelepon'
  },
  {
    name: 'kota',
    label: t('pages.supplierPage.cityColumn'),
    align: 'left',
    field: 'kota',
    sortable: true
  },
  {
    name: 'kontakPerson',
    label: t('pages.supplierPage.contactPersonColumn'),
    align: 'left',
    field: 'kontakPerson'
  }
])

// Lifecycle
onMounted(() => {
  fetchData()
})
</script>

<style lang="sass" scoped>
</style>
