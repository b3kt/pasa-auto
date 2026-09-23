<template>
  <q-page padding>
    <q-splitter v-model="splitterModel" :limits="[50, 100]" style="height: calc(100vh - 100px)">
      <template v-slot:before>
        <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
                      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch"
                      :on-create="openCreateDialog"
                      :on-edit="openEditDialog" :create-label="$t('pages.karyawanPage.createLabel')" ref="tableRef"
                      :search-placeholder="$t('pages.karyawanPage.searchPlaceholder')">
          <template v-slot:title>
            <div class="text-h6 q-mb-md">{{ $t('pages.karyawanPage.title') }}</div>
          </template>
          <template v-slot:body-cell-jenisKelamin="props">
              <q-badge :color="props.row.jenisKelamin === 'L' ? 'blue' : 'pink'">
                {{ props.row.jenisKelamin === 'L' ? $t('pages.karyawanPage.male') : $t('pages.karyawanPage.female') }}
              </q-badge>
          </template>
        </GenericTable>
      </template>

      <template v-slot:after>
        <div class="q-pa-md scroll" style="height: 100%">
          <div class="row items-center q-mb-md">
            <div class="text-h6 q-mb-md">{{ isEditMode ? $t('pages.karyawanPage.editTitle') : $t('pages.karyawanPage.createLabel') }}</div>
            <q-space/>
            <q-btn v-if="isEditMode" flat round dense icon="add" @click="openCreateDialog">
              <q-tooltip>{{ $t('new') }}</q-tooltip>
            </q-btn>
          </div>
          <q-form @submit="handleSave" id="karyawan-form" class="q-gutter-md">
            <q-input v-model="formData.namaKaryawan" :label="$t('pages.karyawanPage.nameLabel')" outlined dense
                     :rules="[val => !!val || $t('pages.karyawanPage.nameRequired')]"
                     hide-bottom-space/>

            <q-select v-model="formData.idPosisi" :label="$t('pages.karyawanPage.positionLabel')" outlined dense :options="filteredPosisiOptions"
                      option-label="posisi" option-value="id" emit-value map-options use-input input-debounce="300"
                      @filter="filterPosisi" :rules="[val => !!val || $t('pages.karyawanPage.positionRequired')]" :loading="loadingPosisi"
                      hide-bottom-space>
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    {{ $t('noResults') }}
                  </q-item-section>
                </q-item>
              </template>
            </q-select>

            <q-input v-model="formData.alamat" :label="$t('pages.karyawanPage.addressLabel')" outlined dense type="textarea" rows="2"/>

            <q-input v-model="formData.tanggalBergabung" :label="$t('pages.karyawanPage.joinDateLabel')" outlined dense type="date"/>

            <q-input v-model="formData.email" :label="$t('email')" outlined dense type="email"/>
            <q-input v-model="formData.noTelepon" :label="$t('pages.karyawanPage.phoneLabel')" outlined dense/>

            <div class="row justify-end q-mt-md q-gutter-sm">
              <q-btn v-if="isEditMode" :label="$t('delete')" color="negative" flat @click="confirmDelete(formData)"
                     :loading="deleting"/>
              <q-btn :label="$t('save')" type="submit" color="primary" :loading="saving" :disable="isEditMode && !isDirty(formData)"/>
            </div>
          </q-form>
        </div>
      </template>
    </q-splitter>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" :title="$t('pages.karyawanPage.confirmDeleteTitle')" min-width="400px" position="standard">
      {{ $t('pages.karyawanPage.confirmDeleteMessage', { item: itemToDelete?.namaKaryawan }) }}
      <template #actions>
        <q-btn flat :label="$t('cancel')" color="primary" @click="showDeleteDialog = false"/>
        <q-btn flat :label="$t('pages.karyawanPage.deleteOnlyButton')" color="negative" @click="deleteItem" :loading="deleting"/>
      </template>
    </GenericDialog>

    <!-- Login created for a new employee: the temporary password is shown only this once -->
    <GenericDialog v-model="showCredentials" :title="$t('pages.karyawanPage.credentialsTitle')" min-width="400px" position="standard">
      <p>{{ $t('pages.karyawanPage.credentialsNotice') }}</p>
      <q-list bordered separator dense>
        <q-item>
          <q-item-section>
            <q-item-label caption>{{ $t('username') }}</q-item-label>
            <q-item-label>{{ credentials?.username }}</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-btn flat round dense icon="content_copy" @click="copy(credentials?.username)"/>
          </q-item-section>
        </q-item>
        <q-item>
          <q-item-section>
            <q-item-label caption>{{ $t('pages.karyawanPage.temporaryPassword') }}</q-item-label>
            <q-item-label class="text-weight-bold" style="font-family: monospace">{{ credentials?.password }}</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-btn flat round dense icon="content_copy" @click="copy(credentials?.password)"/>
          </q-item-section>
        </q-item>
      </q-list>
      <template #actions>
        <q-btn flat :label="$t('pages.karyawanPage.doneButton')" color="primary" @click="closeCredentials"/>
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue'
import {copyToClipboard, useQuasar} from 'quasar'
import {useI18n} from 'vue-i18n'
import {api} from 'boot/axios'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import {useCrud} from 'src/composables/useCrud'
import { useKeyboardShortcuts } from 'src/composables/useKeyboardShortcuts'

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
  baseApiUrl: '/api/pazaauto/karyawan',
  enableCache: true
})

const { t } = useI18n()
const splitterModel = ref(70)
const tableRef = ref(null)

// Posisi Logic
const loadingPosisi = ref(false)
const posisiOptions = ref([])
const filteredPosisiOptions = ref([])

const fetchPosisi = async () => {
  loadingPosisi.value = true
  try {
    const response = await api.get('/api/pazaauto/karyawan-posisi')
    if (response.data.success) {
      posisiOptions.value = response.data.data || []
      filteredPosisiOptions.value = posisiOptions.value
    }
  } catch (error) {
    console.error('Failed to fetch posisi data', error)
  } finally {
    loadingPosisi.value = false
  }
}

const filterPosisi = (val, update) => {
  update(() => {
    if (val === '') {
      filteredPosisiOptions.value = posisiOptions.value
    } else {
      const needle = val.toLowerCase()
      filteredPosisiOptions.value = posisiOptions.value.filter(
        v => v.posisi.toLowerCase().indexOf(needle) > -1
      )
    }
  })
}

// Roles Logic
const loadingRoles = ref(false)
const roleOptions = ref([])

const fetchRoles = async () => {
  loadingRoles.value = true
  try {
    const response = await api.get('/api/roles/lookup')
    if (response.data.success) {
      roleOptions.value = response.data.data || []
    }
  } catch (error) {
    console.error('Failed to fetch roles', error)
  } finally {
    loadingRoles.value = false
  }
}

// Form Data
const formData = ref({
  id: null,
  namaKaryawan: '',
  email: '',
  noTelepon: '',
  alamat: '',
  jenisKelamin: null,
  tanggalBergabung: null,
  idPosisi: null,
  roles: ['user']
})

const resetForm = () => {
  formData.value = {
    id: null,
    namaKaryawan: '',
    email: '',
    noTelepon: '',
    alamat: '',
    jenisKelamin: null,
    tanggalBergabung: null,
    idPosisi: null,
    roles: ['user']
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

// Login details of a just-created employee; cleared as soon as the dialog is closed
const $q = useQuasar()
const showCredentials = ref(false)
const credentials = ref(null)

const closeCredentials = () => {
  showCredentials.value = false
  credentials.value = null
}

const copy = async (text) => {
  try {
    await copyToClipboard(text)
    $q.notify({ type: 'positive', message: t('pages.karyawanPage.copiedNotify'), timeout: 1000 })
  } catch {
    $q.notify({ type: 'warning', message: t('pages.karyawanPage.copyFailedNotify') })
  }
}

const handleSave = async () => {
  const saved = await saveData(formData.value)
  // Keep the one-time credentials out of the form state
  const { loginUsername, initialPassword, ...result } = saved && typeof saved === 'object' ? saved : {}
  if (initialPassword) {
    credentials.value = { username: loginUsername, password: initialPassword }
    showCredentials.value = true
  }
  if (saved) {
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
    name: 'namaKaryawan',
    required: true,
    label: t('pages.karyawanPage.nameLabel'),
    align: 'left',
    field: 'namaKaryawan',
    sortable: true
  },
  {
    name: 'alamat',
    required: true,
    label: t('pages.karyawanPage.addressLabel'),
    align: 'left',
    field: 'alamat',
    sortable: true
  },
  {
    name: 'idPosisi',
    label: t('pages.karyawanPage.positionColumn'),
    align: 'center',
    field: 'namePosisi',
    sortable: true
  },
  {
    name: 'tanggalBergabung',
    label: t('pages.karyawanPage.joinDateColumn'),
    align: 'center',
    field: 'tanggalBergabung',
    sortable: true
  },
  {
    name: 'noTelepon',
    label: t('pages.karyawanPage.phoneLabel'),
    align: 'left',
    field: 'noTelepon'
  }
])

// Lifecycle
onMounted(() => {
  fetchData()
  fetchPosisi()
  fetchRoles()
})
</script>

<style lang="sass" scoped>
</style>
