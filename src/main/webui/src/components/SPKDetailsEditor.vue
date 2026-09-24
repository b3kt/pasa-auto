<template>
  <div class="row q-col-gutter-md q-mb-md q-pl-md q-py-md">
    <!-- Jasa Section -->
    <div class="col-12">
      <q-card flat bordered class="full-height">
        <q-card-section class="bg-grey-2 q-py-xs">
          <div class="text-subtitle2">{{ $t('components.spkDetailsEditor.jasaListTitle') }}</div>
        </q-card-section>
        <q-card-section class="q-pa-none">
          <q-table flat :rows="jasaRows" :columns="jasaColumns" row-key="tempId" dense hide-pagination
                   :rows-per-page-options="[0]" separator="cell">
            <template v-slot:header>
              <q-tr>
                <q-th v-for="column in jasaColumns" :key="column.name" :align="column.align"
                      :auto-width="column.autoWidth" :style="`width:`+column.minWidth">
                  {{ column.label }}
                </q-th>
              </q-tr>
              <q-tr v-if="canEdit">
                <q-th class="text-center" colspan="5">
                  <q-select v-model="newJasa.item" :options="jasaOptions" option-label="namaJasa" dense outlined
                            :label="$t('components.spkDetailsEditor.selectJasaLabel')" use-input input-debounce="300" @filter="filterJasa" emit-value
                            map-options @update:model-value="addJasa" :option-disable="isJasaDisabled">
                    <template v-slot:option="scope">
                      <q-item v-bind="scope.itemProps">
                        <q-item-section>
                          <q-item-label>{{ scope.opt.namaJasa }}</q-item-label>
                          <q-item-label caption>{{ formatCurrency(scope.opt.hargaJasa) }}</q-item-label>
                        </q-item-section>
                      </q-item>
                    </template>
                  </q-select>
                </q-th>
              </q-tr>
            </template>
            <template v-slot:body="props">
              <q-tr :props="props">
                <q-td key="no" :props="props">{{ props.rowIndex + 1 }}</q-td>
                <q-td key="namaJasa" :props="props">{{ props.row.namaItem }}</q-td>
                <q-td key="harga" :props="props" class="text-right">
                  {{ formatCurrency(props.row.harga) }}
                  <q-tooltip v-if="canEdit" anchor="top middle" self="bottom middle">{{ $t('components.spkDetailsEditor.doubleClickToEdit') }}</q-tooltip>
                  <q-popup-edit v-if="canEdit" :model-value="props.row.harga" auto-save v-slot="scope" @save="val => onHargaSave(props.row, val)">
                    <q-input v-model.number="scope.value" dense outlined autofocus counter @keyup.enter="scope.set"
                             type="number"
                             :rules="[(val) => val > 0 || $t('components.spkDetailsEditor.hargaInvalid')]"/>
                  </q-popup-edit>
                </q-td>
                <q-td key="jumlah" :props="props" class="text-center">
                  {{ props.row.jumlah }}
                  <q-tooltip v-if="canEdit" anchor="top middle" self="bottom middle">{{ $t('components.spkDetailsEditor.doubleClickToEdit') }}</q-tooltip>
                  <q-popup-edit v-if="canEdit" :model-value="props.row.jumlah" auto-save v-slot="scope" @save="val => onJumlahSave(props.row, val)">
                    <q-input v-model.number="scope.value" type="number" dense outlined autofocus counter
                             :rules="[(val) => val > 0 || $t('components.spkDetailsEditor.jumlahInvalid')]"
                             @keyup.enter="scope.set" :label="$t('components.spkDetailsEditor.jumlah')"/>
                  </q-popup-edit>
                </q-td>
                <q-td key="total" :props="props" class="text-right">
                  {{ formatCurrency(props.row.harga * props.row.jumlah) }}
                </q-td>
                <q-td key="actions" :props="props" class="text-center" >
                  <q-btn flat dense round icon="delete" color="negative" size="sm"
                         @click="removeDetail(props.row)" v-if="canEdit"/>
                </q-td>
              </q-tr>
            </template>
          </q-table>
        </q-card-section>
        <q-separator/>
        <q-card-section class="q-py-xs text-right bg-grey-2">
          <span class="text-weight-bold">{{ $t('components.spkDetailsEditor.total') }}: {{ formatCurrency(subtotalJasa) }}</span>
        </q-card-section>
      </q-card>
    </div>

    <!-- Barang Section -->
    <div class="col-12">
      <q-card flat bordered class="full-height">
        <q-card-section class="bg-grey-2 q-py-xs">
          <div class="text-subtitle2">{{ $t('components.spkDetailsEditor.barangListTitle') }}</div>
        </q-card-section>
        <q-card-section class="q-pa-none">
          <q-table flat :rows="barangRows" :columns="barangColumns" row-key="tempId" dense hide-pagination
                   :rows-per-page-options="[0]" separator="cell">
            <template v-slot:header>
              <q-tr>
                <q-th v-for="column in barangColumns" :key="column.name" :align="column.align"
                      :auto-width="column.autoWidth" :style="`width:`+column.minWidth">
                  {{ column.label }}
                </q-th>
              </q-tr>
              <q-tr v-if="canEdit">
                <q-th class="text-center" colspan="6">
                  <q-select v-model="newBarang.item" :options="barangOptions" option-label="namaBarang" dense outlined
                            :label="$t('components.spkDetailsEditor.selectBarangLabel')" use-input input-debounce="300" @filter="filterBarang" emit-value
                            map-options @update:model-value="addBarang" :option-disable="isBarangDisabled">
                    <template v-slot:option="scope">
                      <q-item v-bind="scope.itemProps">
                        <q-item-section>
                          <q-item-label>{{ scope.opt.namaBarang }}</q-item-label>
                          <q-item-label caption>{{ formatCurrency(scope.opt.hargaJual) }}</q-item-label>
                        </q-item-section>
                      </q-item>
                    </template>
                  </q-select>
                </q-th>
              </q-tr>
            </template>
            <template v-slot:body="props">
              <q-tr :props="props">
                <q-td key="no" :props="props">{{ props.rowIndex + 1 }}</q-td>
                <q-td key="namaBarang" :props="props">{{ props.row.namaItem }}</q-td>
                <q-td key="harga" :props="props" class="text-right">
                  {{ formatCurrency(props.row.harga) }}
                  <q-tooltip v-if="canEdit" anchor="top middle" self="bottom middle">{{ $t('components.spkDetailsEditor.doubleClickToEdit') }}</q-tooltip>
                  <q-popup-edit v-if="canEdit" :model-value="props.row.harga" auto-save v-slot="scope" @save="val => onHargaSave(props.row, val)">
                    <q-input v-model.number="scope.value" dense outlined autofocus counter @keyup.enter="scope.set"
                             type="number" :label="$t('components.spkDetailsEditor.hargaBarangSatuanLabel')"
                             :rules="[(val) => val > 0 || $t('components.spkDetailsEditor.hargaInvalid')]"/>
                  </q-popup-edit>
                </q-td>
                <q-td key="jumlah" :props="props" class="text-center">
                  {{ props.row.jumlah }}
                  <q-tooltip v-if="canEdit" anchor="top middle" self="bottom middle">{{ $t('components.spkDetailsEditor.doubleClickToEdit') }}</q-tooltip>
                  <q-popup-edit v-if="canEdit" :model-value="props.row.jumlah" auto-save v-slot="scope" @save="val => onJumlahSave(props.row, val)">
                    <q-input v-model.number="scope.value" type="number" dense outlined autofocus counter
                             :rules="[(val) => val > 0 || $t('components.spkDetailsEditor.jumlahInvalid')]"
                             @keyup.enter="scope.set" :label="$t('components.spkDetailsEditor.jumlahBarang')"/>
                  </q-popup-edit>
                </q-td>
                <q-td key="total" :props="props" class="text-right">
                  {{ formatCurrency(props.row.harga * props.row.jumlah) }}
                </q-td>
                <q-td key="actions" :props="props" class="text-center" >
                  <q-btn flat dense round icon="delete" color="negative" size="sm"
                         @click="removeDetail(props.row)" v-if="canEdit"/>
                </q-td>
              </q-tr>
            </template>
          </q-table>
        </q-card-section>
        <q-separator/>
        <q-card-section class="q-py-xs text-right bg-grey-2">
          <span class="text-weight-bold">{{ $t('components.spkDetailsEditor.total') }}: {{ formatCurrency(subtotalBarang) }}</span>
        </q-card-section>
      </q-card>
    </div>
  </div>

  <!-- Master Data Update Confirmation Dialog -->
  <q-dialog v-model="showMasterUpdateDialog" persistent>
    <q-card style="min-width: 350px">
      <q-card-section class="row items-center">
        <q-avatar icon="warning" color="warning" text-color="white" />
        <span class="q-ml-sm text-h6">{{ $t('components.spkDetailsEditor.masterUpdateTitle') }}</span>
      </q-card-section>

      <q-card-section class="q-pt-none">
        {{ $t('components.spkDetailsEditor.masterUpdateMessage', {
          item: pendingUpdateData?.row.namaItem,
          oldHarga: formatCurrency(pendingUpdateData?.type === 'jasa' ? pendingUpdateData?.master.hargaJasa : pendingUpdateData?.master.hargaJual),
          newHarga: formatCurrency(pendingUpdateData?.newValue)
        }) }}
      </q-card-section>

      <q-card-actions align="right" class="text-primary">
        <q-btn flat :label="$t('components.spkDetailsEditor.cancelButton')" v-close-popup @click="pendingUpdateData = null" />
        <q-btn flat :label="$t('components.spkDetailsEditor.localOnlyButton')" @click="confirmUpdateLocalOnly" />
        <q-btn color="primary" :label="$t('components.spkDetailsEditor.updateBothButton')" @click="confirmUpdateBoth" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import {ref, computed} from 'vue'
import {useI18n} from 'vue-i18n'
// import {useQuasar} from 'quasar'
// const $q = useQuasar();
const props = defineProps({
  details: {
    type: Array,
    required: true
  },
  allJasaOptions: {
    type: Array,
    default: () => []
  },
  allBarangOptions: {
    type: Array,
    default: () => []
  },
  canEdit: {
    type: Boolean,
    default: true
  },
  noSpk: {
    type: String,
    default: ''
  },
})

const emit = defineEmits(['update:details', 'update-master-jasa', 'update-master-barang'])

const { t } = useI18n()

const showMasterUpdateDialog = ref(false)
const pendingUpdateData = ref(null)

const newJasa = ref({item: null, jumlah: 1})
const newBarang = ref({item: null, jumlah: 1})
const jasaOptions = ref([])
const barangOptions = ref([])

const formatCurrency = (val) => {
  if (!val) return 'Rp 0'
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    minimumFractionDigits: 0
  }).format(val)
}

const jasaColumns = computed(() => [
  {name: 'no', label: t('components.spkDetailsEditor.noColumn'), align: 'left', field: 'no', autoWidth: true},
  {name: 'namaJasa', label: t('components.spkDetailsEditor.jasaColumn'), align: 'left', field: 'namaItem', autoWidth: false},
  {name: 'harga', label: t('components.spkDetailsEditor.biayaColumn'), align: 'right', field: 'harga', autoWidth: true, minWidth: '200px'},
  {name: 'jumlah', label: t('components.spkDetailsEditor.jumlah'), align: 'center', field: 'jumlah', minWidth: '100px'},
  {name: 'actions', label: '', align: 'center',autoWidth: true }
])

const barangColumns = computed(() => [
  {name: 'no', label: t('components.spkDetailsEditor.noColumn'), align: 'left', field: 'no', autoWidth: true},
  {name: 'namaBarang', label: t('components.spkDetailsEditor.barangColumn'), align: 'left', field: 'namaItem', autoWidth: false},
  {name: 'harga', label: t('components.spkDetailsEditor.hargaColumn'), align: 'right', field: 'harga', autoWidth: true, minWidth: '200px'},
  {name: 'jumlah', label: t('components.spkDetailsEditor.jumlah'), align: 'center', field: 'jumlah', minWidth: '100px'},
  {name: 'actions', label: '', align: 'center', autoWidth: true}
])

const jasaRows = computed(() => {
  return props.details
    .filter(d => d.jasaId)
    .map(d => {
      const jasa = props.allJasaOptions.find(j => j.id === d.jasaId)
      const harga = d.harga !== undefined && d.harga !== null ? d.harga : (jasa ? jasa.hargaJasa : 0)
      const hargaMaster = jasa ? jasa.hargaJasa : 0
      const namaItem = jasa ? jasa.namaJasa : d.namaItem || 'Unknown Service'
      return {...d, harga, hargaMaster, namaItem}
    })
})

const barangRows = computed(() => {
  return props.details
    .filter(d => d.sparepartId)
    .map(d => {
      const barang = props.allBarangOptions.find(b => b.id === d.sparepartId)
      const harga = d.harga !== undefined && d.harga !== null ? d.harga : (barang ? barang.hargaJual : 0)
      const hargaMaster = barang ? barang.hargaJual : 0
      const namaItem = barang ? barang.namaBarang : d.namaItem || 'Unknown Part'
      return {...d, harga, hargaMaster, namaItem}
    })
})

const subtotalJasa = computed(() => {
  return jasaRows.value.reduce((sum, row) => sum + (row.harga * row.jumlah), 0)
})

const subtotalBarang = computed(() => {
  return barangRows.value.reduce((sum, row) => sum + (row.harga * row.jumlah), 0)
})

const filterJasa = (val, update) => {
  update(() => {
    if (val === '') {
      jasaOptions.value = props.allJasaOptions
    } else {
      const needle = val.toLowerCase()
      jasaOptions.value = props.allJasaOptions.filter(v => v.namaJasa.toLowerCase().indexOf(needle) > -1)
    }
  })
}

const filterBarang = (val, update) => {
  update(() => {
    if (val === '') {
      barangOptions.value = props.allBarangOptions
    } else {
      const needle = val.toLowerCase()
      barangOptions.value = props.allBarangOptions.filter(v => v.namaBarang.toLowerCase().indexOf(needle) > -1)
    }
  })
}

const isJasaDisabled = (opt) => {
  return props.details.some(d => d.jasaId === opt.id)
}

const isBarangDisabled = (opt) => {
  return props.details.some(d => d.sparepartId === opt.id)
}

const addJasa = () => {
  if (!newJasa.value.item) return
  const item = {
    id: {noSpk: props.noSpk, namaJasa: newJasa.value.item.namaJasa},
    namaItem: newJasa.value.item.namaJasa,
    jasaId: newJasa.value.item.id,
    harga: newJasa.value.item.hargaJasa,
    hargaMaster: newJasa.value.item.hargaJasa,
    jumlah: newJasa.value.jumlah,
    keterangan: '',
    tempId: Date.now()
  }
  const newDetails = [...props.details, item]
  emit('update:details', newDetails)
  newJasa.value = {item: null, jumlah: 1}
}

const onHargaSave = (row, newValue) => {
  if (row.jasaId) {
    const masterJasa = props.allJasaOptions.find(j => j.id === row.jasaId)
    if (masterJasa && masterJasa.hargaJasa !== newValue) {
      pendingUpdateData.value = { row, newValue, master: masterJasa, type: 'jasa' }
      showMasterUpdateDialog.value = true
    } else {
      updateLocalHarga(row, newValue)
    }
  } else if (row.sparepartId) {
    const masterBarang = props.allBarangOptions.find(b => b.id === row.sparepartId)
    if (masterBarang && masterBarang.hargaJual !== newValue) {
      pendingUpdateData.value = { row, newValue, master: masterBarang, type: 'barang' }
      showMasterUpdateDialog.value = true
    } else {
      updateLocalHarga(row, newValue)
    }
  }
}

// Existing (already-saved) rows come back from the backend as flat DTOs with no `.id` object -
// only newly-added, not-yet-saved rows carry a tempId. Match on whichever identifier the row
// actually has, same as removeDetail() below, or an edit to an already-saved line silently no-ops.
const findDetailIndex = (details, row) => details.findIndex(d =>
  (d.tempId && row.tempId && d.tempId === row.tempId) ||
  (d.jasaId && row.jasaId && d.jasaId === row.jasaId) ||
  (d.sparepartId && row.sparepartId && d.sparepartId === row.sparepartId) ||
  (d.id && row.id && d.id.namaJasa === row.id.namaJasa && d.id.noSpk === row.id.noSpk)
)

const updateLocalHarga = (row, newValue) => {
  const newDetails = [...props.details]
  const index = findDetailIndex(newDetails, row)

  if (index > -1) {
    newDetails[index] = { ...newDetails[index], harga: newValue }
    emit('update:details', newDetails)
  }
}

const onJumlahSave = (row, newValue) => {
  const newDetails = [...props.details]
  const index = findDetailIndex(newDetails, row)

  if (index > -1) {
    newDetails[index] = { ...newDetails[index], jumlah: newValue }
    emit('update:details', newDetails)
  }
}

const confirmUpdateBoth = () => {
  if (pendingUpdateData.value) {
    const { row, newValue, master, type } = pendingUpdateData.value
    updateLocalHarga(row, newValue)
    if (type === 'jasa') {
      emit('update-master-jasa', {
        ...master,
        hargaJasa: newValue
      })
    } else if (type === 'barang') {
      emit('update-master-barang', {
        ...master,
        hargaJual: newValue
      })
    }
    showMasterUpdateDialog.value = false
    pendingUpdateData.value = null
  }
}

const confirmUpdateLocalOnly = () => {
  if (pendingUpdateData.value) {
    const { row, newValue } = pendingUpdateData.value
    updateLocalHarga(row, newValue)
    showMasterUpdateDialog.value = false
    pendingUpdateData.value = null
  }
}

const addBarang = () => {
  if (!newBarang.value.item) return
  const item = {
    id: {noSpk: props.noSpk, namaJasa: newBarang.value.item.namaBarang},
    namaItem: newBarang.value.item.namaBarang,
    sparepartId: newBarang.value.item.id,
    harga: newBarang.value.item.hargaJual,
    hargaMaster: newBarang.value.item.hargaJual,
    jumlah: newBarang.value.jumlah,
    keterangan: '',
    tempId: Date.now()
  }
  const newDetails = [...props.details, item]
  emit('update:details', newDetails)
  newBarang.value = {item: null, jumlah: 1}
}

const removeDetail = (row) => {
  const index = findDetailIndex(props.details, row)
  if (index > -1) {
    const newDetails = [...props.details]
    newDetails.splice(index, 1)
    emit('update:details', newDetails)
  } else {
    console.error('Could not find detail to remove:', row)
    console.error('Current details:', props.details)
  }
}
</script>
