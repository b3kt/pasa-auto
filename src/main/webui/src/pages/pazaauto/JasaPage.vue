<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
          @update:pagination="pagination = $event" @request="onRequest" @search="onSearch" :on-create="openCreateDialog"
          :on-edit="openEditDialog" :create-label="$t('pages.jasaPage.createLabel')" ref="tableRef"
          :search-placeholder="$t('pages.jasaPage.searchPlaceholder')">
            <template v-slot:title>
              <div class="text-h6 q-mb-md">{{ $t('pages.jasaPage.title') }}</div>
            </template>
          <template v-slot:body-cell-hargaJasa="props">
              {{ formatCurrency(props.row.hargaJasa) }}
          </template>
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6 q-mb-md">{{ isEditMode ? $t('pages.jasaPage.editTitle') : $t('pages.jasaPage.createLabel') }}</div>
            <q-space />
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>{{ $t('new') }}</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="jasa-form" class="q-gutter-md">
            <q-input v-model="formData.namaJasa" :label="$t('pages.jasaPage.nameLabel')" outlined dense
              :rules="[val => !!val || $t('pages.jasaPage.nameRequired')]" hide-bottom-space/>

            <q-input v-model.number="formData.hargaJasa" :label="$t('pages.jasaPage.priceLabel')" outlined dense type="number" prefix="Rp"
              :rules="[val => val >= 0 || $t('pages.jasaPage.priceInvalid')]" hide-bottom-space/>

            <q-input v-model.number="formData.estimasiWaktu" :label="$t('pages.jasaPage.estimatedTimeLabel')" outlined dense type="number"
              :rules="[val => val >= 0 || $t('pages.jasaPage.estimatedTimeInvalid')]" hide-bottom-space/>

            <q-input v-model="formData.deskripsi" :label="$t('pages.jasaPage.descriptionLabel')" outlined dense type="textarea" rows="3" />

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode" :label="$t('delete')" color="negative" flat @click="confirmDelete(formData)" :loading="deleting" />
              <q-btn :label="$t('save')" type="submit" color="primary" :loading="saving" :disable="isEditMode && !isDirty(formData)" />
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" :title="$t('confirmDeleteTitle')" min-width="400px" position="standard">
      {{ $t('pages.jasaPage.confirmDeleteMessage', { item: itemToDelete?.namaJasa }) }}
      <template #actions>
        <q-btn flat :label="$t('cancel')" color="primary" @click="showDeleteDialog = false" />
        <q-btn flat :label="$t('deleteOnlyButton')" color="negative" @click="deleteItem" :loading="deleting" />
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
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
  baseApiUrl: '/api/pazaauto/jasa',
  enableCache: true
})

const { t } = useI18n()
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
const columns = computed(() => [
  {
    name: 'namaJasa',
    required: true,
    label: t('pages.jasaPage.nameColumn'),
    align: 'left',
    field: 'namaJasa',
    sortable: true
  },
  {
    name: 'hargaJasa',
    label: t('pages.jasaPage.priceColumn'),
    align: 'right',
    field: 'hargaJasa',
    sortable: true
  },
  {
    name: 'estimasiWaktu',
    label: t('pages.jasaPage.estimatedTimeColumn'),
    align: 'center',
    field: 'estimasiWaktu',
    sortable: true
  },
  {
    name: 'deskripsi',
    label: t('pages.jasaPage.descriptionColumn'),
    align: 'left',
    field: 'deskripsi'
  }
])

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
