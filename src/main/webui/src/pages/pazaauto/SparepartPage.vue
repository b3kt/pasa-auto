<template>
  <q-page padding>
    <GenericTable :rows="rows" :columns="columns" :loading="loading" :pagination="pagination"
      @update:pagination="pagination = $event" @request="onRequest" @search="onSearch" :on-create="openCreateDialog"
      :on-edit="openEditDialog" :on-delete="confirmDelete" :create-label="$t('pages.sparepartPage.createLabel')"
      :search-placeholder="$t('pages.sparepartPage.searchPlaceholder')">
      <template v-slot:body-cell-active="props">
        <q-td :props="props">
          <q-badge :color="props.row.active ? 'green' : 'red'">
            {{ props.row.active ? $t('active') : $t('inactive') }}
          </q-badge>
        </q-td>
      </template>

      <template v-slot:body-cell-hargaJual="props">
        <q-td :props="props">
          {{ formatCurrency(props.row.hargaJual) }}
        </q-td>
      </template>

      <template v-slot:body-cell-hargaBeli="props">
        <q-td :props="props">
          {{ formatCurrency(props.row.hargaBeli) }}
        </q-td>
      </template>
    </GenericTable>

    <!-- Create/Edit Dialog -->
    <GenericDialog v-model="showDialog" :title="isEditMode ? $t('pages.sparepartPage.editTitle') : $t('pages.sparepartPage.createLabel')" min-width="600px">
      <q-form @submit="handleSave" id="sparepart-form" class="q-gutter-md">
        <q-input v-model="formData.kodeSparepart" :label="$t('pages.sparepartPage.codeLabel')" outlined dense
          :rules="[val => !!val || t('pages.sparepartPage.codeRequired')]" />

        <q-input v-model="formData.namaSparepart" :label="$t('pages.sparepartPage.nameLabel')" outlined dense
          :rules="[val => !!val || t('pages.sparepartPage.nameRequired')]" />

        <div class="row q-col-gutter">
          <div class="col-6">
            <q-input v-model.number="formData.hargaJual" :label="$t('pages.sparepartPage.sellPriceLabel')" outlined dense type="number" step="0.01"
              prefix="Rp" class="q-mr-md" />
          </div>
          <div class="col-6">
            <q-input v-model.number="formData.hargaBeli" :label="$t('pages.sparepartPage.buyPriceLabel')" outlined dense type="number" step="0.01"
              prefix="Rp" />
          </div>
        </div>

        <div class="row q-col-gutter">
          <div class="col-4">
            <q-input v-model.number="formData.stok" :label="$t('pages.sparepartPage.stockLabel')" outlined dense type="number" class="q-mr-md" />
          </div>
          <div class="col-4">
            <q-input v-model.number="formData.stokMinimal" :label="$t('pages.sparepartPage.minStockLabel')" outlined dense type="number"
              class="q-mr-md" />
          </div>
          <div class="col-4">
            <q-input v-model="formData.satuan" :label="$t('pages.sparepartPage.unitLabel')" outlined dense />
          </div>
        </div>

        <div class="row q-col-gutter">
          <div class="col-6">
            <q-input v-model="formData.merek" :label="$t('pages.sparepartPage.brandLabel')" outlined dense class="q-mr-md" />
          </div>
          <div class="col-6">
            <q-input v-model="formData.tipeKendaraan" :label="$t('pages.sparepartPage.vehicleTypeLabel')" outlined dense />
          </div>
        </div>

        <q-select v-model="formData.supplierId" :label="$t('pages.sparepartPage.supplierLabel')" outlined dense use-input input-debounce="300"
          :options="supplierOptions" option-value="id" option-label="namaSupplier" @filter="filterSuppliers" emit-value
          map-options clearable>
          <template v-slot:no-option>
            <q-item>
              <q-item-section class="text-grey">
                {{ $t('noResults') }}
              </q-item-section>
            </q-item>
          </template>
        </q-select>

        <q-input v-model="formData.keterangan" :label="$t('notes')" outlined dense type="textarea" rows="2" />

        <q-checkbox v-model="formData.active" :label="$t('active')" />
      </q-form>
      <template #actions>
        <q-btn flat :label="$t('cancel')" color="primary" @click="showDialog = false" />
        <q-btn :label="$t('save')" type="submit" form="sparepart-form" color="primary" :loading="saving" />
      </template>
    </GenericDialog>

    <!-- Delete Confirmation Dialog -->
    <GenericDialog v-model="showDeleteDialog" :title="$t('confirmDeleteTitle')" min-width="400px" position="standard">
      {{ $t('pages.sparepartPage.confirmDeleteMessage', { item: itemToDelete?.namaSparepart }) }}
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
import { api } from 'boot/axios'
import GenericTable from 'components/GenericTable.vue'
import GenericDialog from 'components/GenericDialog.vue'
import { useCrud } from 'src/composables/useCrud'

// Use CRUD Composable
const {
  rows,
  loading,
  saving,
  deleting,
  showDialog,
  showDeleteDialog,
  isEditMode,
  itemToDelete,
  pagination,
  fetchData,
  onRequest,
  onSearch,
  saveData,
  confirmDelete,
  deleteItem,
  openCreateDialog: baseOpenCreateDialog,
  openEditDialog: baseOpenEditDialog
} = useCrud({
  baseApiUrl: '/api/pazaauto/sparepart',
  idField: 'kodeBarang',
  defaultPagination: {
    sortBy: null,
    descending: false,
    page: 1,
    rowsPerPage: 10,
    rowsNumber: 0
  }
})

const { t } = useI18n()

// Supplier Logic
const supplierOptions = ref([])

const filterSuppliers = async (val, update) => {
  if (val === '') {
    update(() => {
      supplierOptions.value = []
    })
    return
  }

  try {
    const response = await api.get('/api/pazaauto/supplier', {
      params: { search: val }
    })
    update(() => {
      if (response.data.success) {
        supplierOptions.value = response.data.data || []
      }
    })
  } catch (error) {
    update(() => {
      console.error('Error fetching suppliers:', error)
      supplierOptions.value = []
    })
  }
}

// Form Data
const formData = ref({
  kodeBarang: '',
  kodeSparepart: '',
  namaSparepart: '',
  hargaJual: null,
  hargaBeli: null,
  stok: null,
  stokMinimal: null,
  satuan: '',
  merek: '',
  tipeKendaraan: '',
  supplierId: null,
  keterangan: '',
  active: true
})

const resetForm = () => {
  formData.value = {
    kodeBarang: '',
    kodeSparepart: '',
    namaSparepart: '',
    hargaJual: null,
    hargaBeli: null,
    stok: null,
    stokMinimal: null,
    satuan: '',
    merek: '',
    tipeKendaraan: '',
    supplierId: null,
    keterangan: '',
    active: true
  }
  supplierOptions.value = []
}

const openCreateDialog = () => {
  baseOpenCreateDialog(resetForm)
}

const openEditDialog = async (row) => {
  baseOpenEditDialog(row, (r) => {
    formData.value = { ...r }
  })

  // Pre-load supplier if exists
  if (row.supplierId) {
    try {
      const response = await api.get(`/api/pazaauto/supplier/${row.supplierId}`)
      if (response.data.success) {
        supplierOptions.value = [response.data.data]
      }
    } catch (error) {
      console.error('Error fetching supplier details:', error)
    }
  }
}

const handleSave = async () => {
  await saveData(formData.value)
}

// Table Columns
const columns = computed(() => [
  {
    name: 'kodeSparepart',
    required: true,
    label: t('pages.sparepartPage.codeColumn'),
    align: 'left',
    field: 'kodeSparepart',
    sortable: true
  },
  {
    name: 'namaSparepart',
    required: true,
    label: t('pages.sparepartPage.nameColumn'),
    align: 'left',
    field: 'namaSparepart',
    sortable: true
  },
  {
    name: 'hargaJual',
    label: t('pages.sparepartPage.sellPriceColumn'),
    align: 'right',
    field: 'hargaJual',
    sortable: true
  },
  {
    name: 'hargaBeli',
    label: t('pages.sparepartPage.buyPriceColumn'),
    align: 'right',
    field: 'hargaBeli',
    sortable: true
  },
  {
    name: 'stok',
    label: t('pages.sparepartPage.stockColumn'),
    align: 'center',
    field: 'stok',
    sortable: true
  },
  {
    name: 'merek',
    label: t('pages.sparepartPage.brandColumn'),
    align: 'left',
    field: 'merek'
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
