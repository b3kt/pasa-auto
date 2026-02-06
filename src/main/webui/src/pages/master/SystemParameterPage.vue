<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
          @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
          :on-create="openCreateDialog" :on-edit="openEditDialog" ref="tableRef"
          create-label="Tambah data System Parameter" search-placeholder="Search by name or value..." />
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6">{{ isEditMode ? 'Edit System Parameter' : 'Tambah data System Parameter' }}</div>
            <q-space />
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>New</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="system-parameter-form" class="q-gutter-md">
            <q-input v-model="formData.name" label="Parameter Name *" outlined dense
              :rules="[val => !!val || 'Name harus diisi']" :readonly="isEditMode" />

            <q-input v-model="formData.value" label="Value *" outlined dense
              :rules="[val => !!val || 'Value harus diisi']" />

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode" label="Hapus" color="negative" flat @click="confirmDelete(formData)" :loading="deleting" />
              <q-btn label="Simpan" type="submit" color="primary" :loading="saving" :disable="isEditMode && !isDirty(formData)" />
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialog -->
        <GenericDialog v-model="showDeleteDialog" title="Confirm Delete" min-width="400px" position="standard">
            Are you sure you want to delete parameter <strong>{{ itemToDelete?.name }}</strong>?
            <template #actions>
                <q-btn flat label="Cancel" color="primary" @click="showDeleteDialog = false" />
                <q-btn flat label="Hapus" color="negative" @click="deleteItem" :loading="deleting" />
            </template>
        </GenericDialog>
    </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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
    baseApiUrl: '/api/system-parameters'
})

const splitterModel = ref(70)
const tableRef = ref(null)

// Form Data
const formData = ref({
    id: null,
    name: '',
    value: ''
})

const resetForm = () => {
    formData.value = {
        id: null,
        name: '',
        value: ''
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
const columns = [
    {
        name: 'name',
        required: true,
        label: 'Parameter Name',
        align: 'left',
        field: 'name',
        sortable: true
    },
    {
        name: 'value',
        required: true,
        label: 'Value',
        align: 'left',
        field: 'value'
    },
    {
        name: 'actions',
        label: 'Actions',
        align: 'center',
        field: 'actions'
    }
]

// Lifecycle
onMounted(() => {
    fetchData()
})
</script>

<style lang="sass" scoped>
</style>
