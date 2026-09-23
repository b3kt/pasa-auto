<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
          @update:pagination="pagination = $event" @request="onRequest" @search="onSearch" :on-create="openCreateDialog"
          :on-edit="openEditDialog" :create-label="$t('pages.userPage.createLabel')" ref="tableRef"
          :search-placeholder="$t('pages.userPage.searchPlaceholder')">
            <template v-slot:title>
              <div class="text-h6 q-mb-md">{{ $t('pages.userPage.title') }}</div>
            </template>
          <template v-slot:body-cell-active="props">
              <q-badge :color="statusColor(props.row)">
                {{ statusLabel(props.row) }}
              </q-badge>
          </template>
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6">{{ isEditMode ? $t('pages.userPage.editTitle') : $t('pages.userPage.createLabel') }}</div>
            <q-space />
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>{{ $t('new') }}</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="user-form" class="q-gutter-md">
            <q-input v-model="formData.username" :label="$t('pages.userPage.usernameLabel')" outlined dense
              :rules="[val => !!val || $t('pages.userPage.usernameRequired')]" />

            <q-input v-model="formData.email" :label="$t('pages.userPage.emailLabel')" outlined dense type="email"
              :rules="[val => !!val || $t('pages.userPage.emailRequired')]" />

            <!-- Always a temporary password: the user must change it at their next login -->
            <q-input v-model="newPassword" :label="isEditMode ? $t('pages.userPage.resetPasswordLabel') : $t('pages.userPage.temporaryPasswordLabel')" outlined dense
              autocomplete="new-password" :type="showPassword ? 'text' : 'password'"
              :hint="isEditMode ? $t('pages.userPage.resetPasswordHint') : $t('pages.userPage.temporaryPasswordHint')"
              :rules="[
                val => isEditMode || !!val || $t('pages.userPage.passwordRequired'),
                val => !val || val.length >= 8 || $t('pages.userPage.minLength8')
              ]">
              <template v-slot:append>
                <q-icon :name="showPassword ? 'visibility_off' : 'visibility'" class="cursor-pointer"
                  @click="showPassword = !showPassword" />
              </template>
            </q-input>

            <q-select v-model="formData.karyawanId" :options="filteredKaryawanOptions" option-value="id"
              option-label="namaKaryawan" emit-value map-options :label="$t('pages.userPage.selectKaryawanLabel')" outlined dense use-input
              input-debounce="300" @filter="filterKaryawan" clearable>
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    {{ $t('noResults') }}
                  </q-item-section>
                </q-item>
              </template>
            </q-select>

            <q-checkbox v-model="formData.active" :label="$t('active')" />

            <div>
              <q-checkbox v-model="formData.googleLoginEnabled" :label="$t('pages.userPage.allowGoogleLoginLabel')" />
              <div class="text-caption text-grey-7 q-ml-lg">
                {{ $t('pages.userPage.allowGoogleLoginHint') }}
              </div>
            </div>

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode" :label="$t('delete')" color="negative" flat @click="confirmDelete(formData)" :loading="deleting" />
              <q-btn :label="$t('save')" type="submit" color="primary" :loading="saving" :disable="isEditMode && !isDirty(formData) && !newPassword" />
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" :title="$t('confirmDelete')" min-width="400px" position="standard">
      {{ $t('confirmDeleteMessage', { item: itemToDelete?.username }) }}
      <template #actions>
        <q-btn flat :label="$t('cancel')" color="primary" @click="showDeleteDialog = false" />
        <q-btn flat :label="$t('delete')" color="negative" @click="deleteItem" :loading="deleting" />
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from 'boot/axios'
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
  baseApiUrl: '/api/users'
})

const { t } = useI18n()
const splitterModel = ref(70)
const tableRef = ref(null)

// Additional State
const showPassword = ref(false)
// Kept out of formData: the API never returns passwords, and an empty value means "keep the current one"
const newPassword = ref('')
const karyawanOptions = ref([])
const filteredKaryawanOptions = ref([])

// A pending or rejected account is inactive too, so the status has to say which it is
const statusLabel = (row) => {
  if (row.approvalStatus === 'PENDING') return t('pages.userPage.pendingApproval')
  if (row.approvalStatus === 'REJECTED') return t('pages.userPage.rejected')
  return row.active ? t('active') : t('inactive')
}

const statusColor = (row) => {
  if (row.approvalStatus === 'PENDING') return 'orange'
  if (row.approvalStatus === 'REJECTED') return 'red'
  return row.active ? 'green' : 'red'
}

// Form Data
const formData = ref({
  id: null,
  username: '',
  email: '',
  karyawanId: null,
  active: true,
  googleLoginEnabled: false
})

const resetForm = () => {
  formData.value = {
    id: null,
    username: '',
    email: '',
    karyawanId: null,
    active: true,
    googleLoginEnabled: false
  }
  newPassword.value = ''
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
  newPassword.value = ''
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
  const payload = newPassword.value ? { ...formData.value, password: newPassword.value } : formData.value
  const result = await saveData(payload)
  if (result) {
    newPassword.value = ''
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
    if (!saving.value && !(isEditMode.value && !isDirty(formData.value) && !newPassword.value)) {
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
const columns = computed(() => [
  {
    name: 'username',
    required: true,
    label: t('username'),
    align: 'left',
    field: 'username',
    sortable: true
  },
  {
    name: 'email',
    required: true,
    label: t('email'),
    align: 'left',
    field: 'email',
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

// Lifecycle
onMounted(() => {
  fetchData()
})
</script>

<style lang="sass" scoped>
</style>
