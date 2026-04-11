<template>
  <div>
      <div class="q-mb-md">
        <span class="text-caption text-bold">Informasi Pelanggan</span>
      </div>
      <div class="q-mb-md">
        <q-input :model-value="namaPelanggan" label="Nama *" outlined dense
                 @update:model-value="$emit('update:namaPelanggan', $event)"
                 :readonly="!isNewCustomer"
                 :rules="isNewCustomer ? [val => !!val || 'Nama harus diisi'] : []"/>
      </div>
      <div class="q-mb-md">
        <q-input :model-value="alamat" label="Alamat" outlined dense
                 @update:model-value="$emit('update:alamat', $event)"
                 :readonly="!isNewCustomer"
                 type="textarea" rows="4"/>
      </div>
      <div class="q-mb-md">
        <q-input :model-value="nopol" label="Kendaraan" outlined dense readonly/>
      </div>
      <div class="row q-col-gutter-sm">
        <div class="q-mb-md col-6">
          <q-select :model-value="merk" label="Merk *" outlined dense
                   @update:model-value="handleMerkChange"
                   :options="merkOptions"
                   :option-label="opt => typeof opt === 'string' ? opt : opt"
                   option-value="merk" emit-value map-options use-input
                   input-debounce="300" @filter="filterMerk" 
                   :loading="loadingMerk"
                   :readonly="!isNewCustomer"
                   :rules="isNewCustomer ? [val => !!val || 'Merk harus diisi'] : []"
                   new-value-mode="add-unique"
                   hide-bottom-space>
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  No results found. Type to add new merk.
                </q-item-section>
              </q-item>
            </template>
          </q-select>
        </div>
        <div class="q-mb-md col-6">
          <q-select :model-value="jenis" label="Jenis" outlined dense
                   @update:model-value="handleJenisChange"
                   :options="filteredJenisOptions"
                   :option-label="opt => typeof opt === 'string' ? opt : opt"
                   option-value="jenis" emit-value map-options use-input
                   input-debounce="300" @filter="filterJenis"
                   :loading="loadingJenis"
                   :readonly="!isNewCustomer"
                   new-value-mode="add-unique"
                   hide-bottom-space>
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  No results found. Type to add new jenis.
                </q-item-section>
              </q-item>
            </template>
          </q-select>
        </div>
      </div>
  </div>
</template>

<script setup>
const props = defineProps({
  namaPelanggan: String,
  alamat: String,
  nopol: String,
  merk: String,
  jenis: String,
  isNewCustomer: Boolean,
  merkOptions: Array,
  filteredJenisOptions: Array,
  loadingMerk: Boolean,
  loadingJenis: Boolean
})

const emit = defineEmits(['update:namaPelanggan', 'update:alamat', 'update:merk', 'update:jenis', 'filter:merk', 'filter:jenis', 'check:vehicle'])

const handleMerkChange = (value) => {
  emit('update:merk', value)
  if (props.isNewCustomer && value && props.jenis) {
    emit('check:vehicle', value, props.jenis)
  }
}

const handleJenisChange = (value) => {
  emit('update:jenis', value)
  if (props.isNewCustomer && props.merk && value) {
    emit('check:vehicle', props.merk, value)
  }
}

const filterMerk = (inputVal, doneFn) => {
  emit('filter:merk', inputVal, doneFn)
}

const filterJenis = (inputVal, doneFn) => {
  emit('filter:jenis', inputVal, doneFn)
}
</script>
