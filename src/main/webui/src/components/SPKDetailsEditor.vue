<template>
  <div class="row q-col-gutter-md q-mb-md q-pl-md q-py-md">
    <!-- Jasa Section -->
    <div class="col-12">
      <q-card flat bordered class="full-height">
        <q-card-section class="bg-grey-2 q-py-xs">
          <div class="text-subtitle2">DAFTAR LAYANAN PERBAIKAN / SERVIS</div>
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
                <q-th class="text-center" colspan="4">
                  <q-select v-model="newJasa.item" :options="jasaOptions" option-label="namaJasa" dense outlined
                            label="Pilih Jasa" use-input input-debounce="300" @filter="filterJasa" emit-value
                            map-options @update:model-value="addJasa">
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
                  <q-popup-edit :model-value="props.row.harga" auto-save v-slot="scope" @save="val => onHargaSave(props.row, val)">
                    <q-input v-model.number="scope.value" dense outlined autofocus counter @keyup.enter="scope.set"
                             type="number"
                             :rules="[(val) => val > 0 || 'Harga harus lebih dari 0']"/>
                  </q-popup-edit>
                </q-td>
                <q-td key="jumlah" :props="props">
                  {{ props.row.jumlah }}
                </q-td>
                <q-td key="total" :props="props" class="text-right">
                  {{ formatCurrency(props.row.harga * props.row.jumlah) }}
                </q-td>
                <q-td key="actions" :props="props" class="text-center" v-if="canEdit">
                  <q-btn flat dense round icon="delete" color="negative" size="sm"
                         @click="removeDetail(props.row)"/>
                </q-td>
              </q-tr>
            </template>
          </q-table>
        </q-card-section>
        <q-separator/>
        <q-card-section class="q-py-xs text-right bg-grey-2">
          <span class="text-weight-bold">Total: {{ formatCurrency(subtotalJasa) }}</span>
        </q-card-section>
      </q-card>
    </div>

    <!-- Barang Section -->
    <div class="col-12">
      <q-card flat bordered class="full-height">
        <q-card-section class="bg-grey-2 q-py-xs">
          <div class="text-subtitle2">BARANG / SPAREPART</div>
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
                            label="Pilih Barang" use-input input-debounce="300" @filter="filterBarang" emit-value
                            map-options @update:model-value="addBarang">
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
                  <q-popup-edit :model-value="props.row.harga" auto-save v-slot="scope" @save="val => onHargaSave(props.row, val)">
                    <q-input v-model.number="scope.value" dense outlined autofocus counter @keyup.enter="scope.set"
                             type="number" label="Harga Barang Satuan"
                             :rules="[(val) => val > 0 || 'Harga harus lebih dari 0']"/>
                  </q-popup-edit>
                </q-td>
                <q-td key="jumlah" :props="props">
                  {{ props.row.jumlah }}
                  <q-popup-edit v-if="canEdit" v-model.number="props.row.jumlah" v-slot="scope">
                    <q-input v-model.number="scope.value" type="number" dense outlined autofocus counter
                             :rules="[(val) => val > 0 || 'Harga harus lebih dari 0']"
                             @keyup.enter="scope.set" label="Jumlah barang"/>
                  </q-popup-edit>
                </q-td>
                <q-td key="total" :props="props" class="text-right">
                  {{ formatCurrency(props.row.harga * props.row.jumlah) }}
                </q-td>
                <q-td key="actions" :props="props" class="text-center" v-if="canEdit">
                  <q-btn flat dense round icon="delete" color="negative" size="sm"
                         @click="removeDetail(props.row)"/>
                </q-td>
              </q-tr>
            </template>
          </q-table>
        </q-card-section>
        <q-separator/>
        <q-card-section class="q-py-xs text-right bg-grey-2">
          <span class="text-weight-bold">Total: {{ formatCurrency(subtotalBarang) }}</span>
        </q-card-section>
      </q-card>
    </div>
  </div>

  <!-- Master Data Update Confirmation Dialog -->
  <q-dialog v-model="showMasterUpdateDialog" persistent>
    <q-card style="min-width: 350px">
      <q-card-section class="row items-center">
        <q-avatar icon="warning" color="warning" text-color="white" />
        <span class="q-ml-sm text-h6">Update Master Data?</span>
      </q-card-section>

      <q-card-section class="q-pt-none">
        Harga master untuk <strong>"{{ pendingUpdateData?.row.namaItem }}"</strong> berbeda
        ({{ formatCurrency(pendingUpdateData?.type === 'jasa' ? pendingUpdateData?.master.hargaJasa : pendingUpdateData?.master.hargaJual) }}).
        Apakah Anda ingin memperbarui harga master menjadi <strong>{{ formatCurrency(pendingUpdateData?.newValue) }}</strong> juga?
      </q-card-section>

      <q-card-actions align="right" class="text-primary">
        <q-btn flat label="Batal" v-close-popup @click="pendingUpdateData = null" />
        <q-btn flat label="Hanya Item Ini" @click="confirmUpdateLocalOnly" />
        <q-btn color="primary" label="Update Keduanya" @click="confirmUpdateBoth" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import {ref, computed} from 'vue'
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

const jasaColumns = [
  {name: 'no', label: 'No', align: 'left', field: 'no', autoWidth: true},
  {name: 'namaJasa', label: 'Jasa', align: 'left', field: 'namaItem', autoWidth: false},
  {name: 'harga', label: 'Biaya', align: 'right', field: 'harga', autoWidth: true, minWidth: '200px'},
  {name: 'actions', label: '', align: 'center'}
]

const barangColumns = [
  {name: 'no', label: 'NO', align: 'left', field: 'no'},
  {name: 'namaBarang', label: 'Barang', align: 'left', field: 'namaItem'},
  {name: 'harga', label: 'Harga', align: 'right', field: 'harga'},
  {name: 'jumlah', label: 'Jumlah', align: 'center', field: 'jumlah'},
  {name: 'actions', label: '', align: 'center'}
]

const jasaRows = computed(() => {
  return props.details
    .filter(d => d.jasaId)
    .map(d => {
      const jasa = props.allJasaOptions.find(j => j.id === d.jasaId)
      const harga = d.harga !== undefined && d.harga !== null ? d.harga : (jasa ? jasa.hargaJasa : 0)
      const namaItem = jasa ? jasa.namaJasa : d.namaItem || 'Unknown Service'
      return {...d, harga, namaItem}
    })
})

const barangRows = computed(() => {
  return props.details
    .filter(d => d.sparepartId)
    .map(d => {
      const barang = props.allBarangOptions.find(b => b.id === d.sparepartId)
      const harga = d.harga !== undefined && d.harga !== null ? d.harga : (barang ? barang.hargaJual : 0)
      const namaItem = barang ? barang.namaBarang : d.namaItem || 'Unknown Part'
      return {...d, harga, namaItem}
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

const addJasa = () => {
  if (!newJasa.value.item) return
  const item = {
    id: {noSpk: props.noSpk, namaJasa: newJasa.value.item.namaJasa},
    namaItem: newJasa.value.item.namaJasa,
    jasaId: newJasa.value.item.id,
    harga: newJasa.value.item.hargaJasa,
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

const updateLocalHarga = (row, newValue) => {
  const newDetails = [...props.details]
  const index = newDetails.findIndex(d =>
    (d.tempId && d.tempId === row.tempId) ||
    (d.id && row.id && d.id.namaJasa === row.id.namaJasa && d.id.noSpk === row.id.noSpk)
  )

  if (index > -1) {
    newDetails[index] = { ...newDetails[index], harga: newValue }
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
    jumlah: newBarang.value.jumlah,
    keterangan: '',
    tempId: Date.now()
  }
  const newDetails = [...props.details, item]
  emit('update:details', newDetails)
  newBarang.value = {item: null, jumlah: 1}
}

const removeDetail = (row) => {
  const index = props.details.findIndex(d =>
    (d.tempId && d.tempId === row.tempId) ||
    (d.id && row.id && d.id.namaJasa === row.id.namaJasa && d.id.noSpk === row.id.noSpk)
  )
  if (index > -1) {
    const newDetails = [...props.details]
    newDetails.splice(index, 1)
    emit('update:details', newDetails)
  }
}
</script>
