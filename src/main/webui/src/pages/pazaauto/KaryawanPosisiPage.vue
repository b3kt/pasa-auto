<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
          @update:pagination="pagination = $event" @request="onRequest" @search="onSearch" :on-create="openCreateDialog"
          :on-edit="openEditDialog" :create-label="$t('pages.posisiPage.createLabel')" ref="tableRef"
          :search-placeholder="$t('search')">
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6 q-mb-md">{{ isEditMode ? $t('pages.posisiPage.editTitle') : $t('pages.posisiPage.createLabel') }}</div>
            <q-space />
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>{{ $t('new') }}</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="posisi-form" class="q-gutter-md">
            <q-input v-model="formData.posisi" :label="$t('pages.posisiPage.nameLabel')" outlined dense
              :rules="[val => !!val || $t('pages.posisiPage.nameRequired')]" />

            <q-input v-model="formData.keterangan" :label="$t('notes')" outlined dense type="textarea" rows="3" />

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
      {{ $t('confirmDeleteMessage', { item: itemToDelete?.posisi }) }}
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
  baseApiUrl: '/api/pazaauto/karyawan-posisi'
})

const { t } = useI18n()
const splitterModel = ref(70)
const tableRef = ref(null)

// Form Data
const formData = ref({
  id: null,
  posisi: '',
  keterangan: ''
})

const resetForm = () => {
  formData.value = {
    id: null,
    posisi: '',
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
    name: 'posisi',
    required: true,
    label: t('pages.posisiPage.nameColumn'),
    align: 'left',
    field: 'posisi',
    sortable: true
  },
  {
    name: 'keterangan',
    label: t('notes'),
    align: 'left',
    field: 'keterangan'
  },
  {
    name: 'actions',
    label: t('actions'),
    align: 'center',
    field: 'actions'
  }
])

// Lifecycle
onMounted(() => {
  fetchData()
})
</script>

<style lang="sass" scoped>
</style>
