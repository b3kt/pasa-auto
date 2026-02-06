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
          <q-input :model-value="merk" label="Merk *" outlined dense
                   @update:model-value="$emit('update:merk', $event)"
                   :readonly="!isNewCustomer"
                   :rules="isNewCustomer ? [val => !!val || 'Merk harus diisi'] : []"/>
        </div>
        <div class="q-mb-md col-6">
          <q-input :model-value="jenis" label="Jenis" outlined dense
                   @update:model-value="$emit('update:jenis', $event)"
                   :readonly="!isNewCustomer"/>
        </div>
      </div>
  </div>
</template>

<script setup>
defineProps({
  namaPelanggan: String,
  alamat: String,
  nopol: String,
  merk: String,
  jenis: String,
  isNewCustomer: Boolean
})

defineEmits(['update:namaPelanggan', 'update:alamat', 'update:merk', 'update:jenis'])
</script>
