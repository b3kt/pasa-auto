<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
          @update:pagination="pagination = $event" @request="onRequest" @search="onSearch" :on-create="openCreateDialog"
          :on-edit="openEditDialog" create-label="Tambah data User" ref="tableRef"
          search-placeholder="Search by username or email...">
          <template v-slot:body-cell-active="props">
              <q-badge :color="props.row.active ? 'green' : 'red'">
                {{ props.row.active ? 'Active' : 'Inactive' }}
              </q-badge>
          </template>
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6">{{ isEditMode ? 'Edit User' : 'Tambah data User' }}</div>
            <q-space />
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>New</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="user-form" class="q-gutter-md">
            <q-input v-model="formData.username" label="Username *" outlined dense
              :rules="[val => !!val || 'Username harus diisi']" />

            <q-input v-model="formData.email" label="Email *" outlined dense type="email"
              :rules="[val => !!val || 'Email harus diisi']" />

            <q-input v-model="formData.passwordHash" label="Password *" outlined dense
              :type="showPassword ? 'text' : 'password'" :rules="[val => !!val || 'Password harus diisi']">
              <template v-slot:append>
                <q-icon :name="showPassword ? 'visibility_off' : 'visibility'" class="cursor-pointer"
                  @click="showPassword = !showPassword" />
              </template>
            </q-input>

            <q-select v-model="formData.karyawanId" :options="filteredKaryawanOptions" option-value="id"
              option-label="namaKaryawan" emit-value map-options label="Pilih Karyawan" outlined dense use-input
              input-debounce="300" @filter="filterKaryawan" clearable>
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    No results
                  </q-item-section>
                </q-item>
              </template>
            </q-select>

            <q-checkbox v-model="formData.active" label="Active" />

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
      Are you sure you want to delete <strong>{{ itemToDelete?.username }}</strong>?
      <template #actions>
        <q-btn flat label="Cancel" color="primary" @click="showDeleteDialog = false" />
        <q-btn flat label="Hapus" color="negative" @click="deleteItem" :loading="deleting" />
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from 'boot/axios'
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
  baseApiUrl: '/api/users'
})

const splitterModel = ref(70)
const tableRef = ref(null)

// Additional State
const showPassword = ref(false)
const karyawanOptions = ref([])
const filteredKaryawanOptions = ref([])

// Form Data
const formData = ref({
  id: null,
  username: '',
  email: '',
  passwordHash: '',
  karyawanId: null,
  active: true
})

const resetForm = () => {
  formData.value = {
    id: null,
    username: '',
    email: '',
    passwordHash: '',
    karyawanId: null,
    active: true
  }
  showPassword.value = false
}

const deleteItem = async () => {
  const success = await baseDeleteItem()
  if (success) {
    resetForm()
  }
}

const openCreateDialog = async () => {
  baseOpenCreateDialog(resetForm)
  await fetchKaryawan()
}

const openEditDialog = async (row) => {
  baseOpenEditDialog(row, (r) => {
    formData.value = { ...r }
  })
  await fetchKaryawan()

  // If editing a user with an assigned Karyawan, fetch that Karyawan's details
  // so it appears in the dropdown (it won't be in the unregistered list)
  if (row.karyawanId) {
    try {
      const response = await api.get(`/api/pazaauto/karyawan/${row.karyawanId}`)
      if (response.data.success && response.data.data) {
        const currentKaryawan = response.data.data
        // Check if already in options (shouldn't be, but good to be safe)
        const exists = karyawanOptions.value.some(k => k.id === currentKaryawan.id)
        if (!exists) {
          karyawanOptions.value.push(currentKaryawan)
          // Re-trigger filter update if needed or just let reactivity handle it
          filteredKaryawanOptions.value = karyawanOptions.value
        }
      }
    } catch (error) {
      console.error('Failed to fetch current karyawan details', error)
    }
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

// Karyawan Logic
const fetchKaryawan = async () => {
  try {
    const response = await api.get('/api/pazaauto/karyawan/unregistered')
    if (response.data.success) {
      karyawanOptions.value = response.data.data || []
      filteredKaryawanOptions.value = karyawanOptions.value
    }
  } catch (error) {
    console.error('Failed to fetch karyawan:', error)
  }
}

const filterKaryawan = (val, update) => {
  update(() => {
    if (val === '') {
      filteredKaryawanOptions.value = karyawanOptions.value
    } else {
      const needle = val.toLowerCase()
      filteredKaryawanOptions.value = karyawanOptions.value.filter(
        v => v.namaKaryawan.toLowerCase().indexOf(needle) > -1
      )
    }
  })
}

// Table Columns
const columns = [
  {
    name: 'username',
    required: true,
    label: 'Username',
    align: 'left',
    field: 'username',
    sortable: true
  },
  {
    name: 'email',
    required: true,
    label: 'Email',
    align: 'left',
    field: 'email',
    sortable: true
  },
  {
    name: 'active',
    label: 'Status',
    align: 'center',
    field: 'active',
    sortable: true
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
