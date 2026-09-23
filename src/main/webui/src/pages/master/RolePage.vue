<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
          @update:pagination="pagination = $event" @request="onRequest" @search="onSearch" :on-create="openCreateDialog"
          :on-edit="openEditDialog" :create-label="$t('pages.rolePage.createLabel')" ref="tableRef"
          :search-placeholder="$t('pages.rolePage.searchPlaceholder')">

            <template v-slot:title>
              <div class="text-h6 q-mb-md">{{ $t('pages.rolePage.title') }}</div>
            </template>
          <template v-slot:body-cell-active="props">
              <q-badge :color="props.row.active ? 'green' : 'red'">
                {{ props.row.active ? $t('active') : $t('inactive') }}
              </q-badge>
          </template>

          <template v-slot:body-cell-actions="props">
              <q-btn flat dense round icon="preview" color="info" @click.stop="viewRole(props.row)">
                <q-tooltip>{{ $t('viewDetails') }}</q-tooltip>
              </q-btn>
          </template>
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6">{{ isEditMode ? $t('pages.rolePage.editTitle') : $t('pages.rolePage.createLabel') }}</div>
            <q-space />
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>{{ $t('new') }}</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="role-form" class="q-gutter-md">
            <q-input v-model="formData.name" :label="$t('name') + ' *'" outlined dense
              :rules="[val => !!val || $t('nameRequired')]" />

            <q-input v-model="formData.description" :label="$t('description')" outlined dense type="textarea" rows="3" />

            <q-checkbox v-model="formData.active" :label="$t('active')" />

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode" :label="$t('delete')" color="negative" flat @click="confirmDelete(formData)" :loading="deleting" />
              <q-btn :label="$t('save')" type="submit" color="primary" :loading="saving" :disable="isEditMode && !isDirty(formData)" />
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" :title="$t('confirmDelete')" min-width="400px" position="standard">
      {{ $t('confirmDeleteMessage', { item: itemToDelete?.name }) }}
      <template #actions>
        <q-btn flat :label="$t('cancel')" color="primary" @click="showDeleteDialog = false" />
        <q-btn flat :label="$t('delete')" color="negative" @click="deleteRole" :loading="deleting" />
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import { useCrud } from 'src/composables/useCrud'
import { useKeyboardShortcuts } from 'src/composables/useKeyboardShortcuts'

const router = useRouter()
const { t } = useI18n()

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
  baseApiUrl: '/api/roles'
})

const columns = computed(() => [
  {
    name: 'name',
    required: true,
    label: t('name'),
    align: 'left',
    field: 'name',
    sortable: true
  },
  {
    name: 'description',
    label: t('description'),
    align: 'left',
    field: 'description',
    sortable: true
  },
  {
    name: 'active',
    label: t('status'),
    align: 'center',
    field: 'active',
    sortable: true
  },
  {
    name: 'actions',
    label: t('actions'),
    align: 'center',
    field: 'actions'
  }
])

const splitterModel = ref(70)
const tableRef = ref(null)

// Form data
const formData = ref({
  id: null,
  name: '',
  description: '',
  active: true
})

// Methods
const viewRole = (row) => {
  router.push(`/roles/${row.id}`)
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

const resetForm = () => {
  formData.value = {
    id: null,
    name: '',
    description: '',
    active: true
  }
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

const deleteRole = async () => {
  await deleteItem()
}

// Lifecycle
onMounted(() => {
  fetchData()
})
</script>

<style lang="sass" scoped>
.my-sticky-header-table
  max-height: 70vh

  thead tr th
    position: sticky
    z-index: 1
    background-color: #ffffff

  thead tr:first-child th
    top: 0
</style>
