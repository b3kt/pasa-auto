<template>
  <div>
      <div class="q-mb-md">
        <span class="text-caption text-bold">Informasi Pelanggan</span>
      </div>
      <div class="q-mb-md">
        <q-input :model-value="editableFields.namaPelanggan !== undefined ? editableFields.namaPelanggan : namaPelanggan" 
                 label="Nama *" outlined dense
                 @update:model-value="handleFieldUpdate('namaPelanggan', $event)"
                 @blur="handleFieldBlur('namaPelanggan')"
                 @dblclick="handleDoubleClick('namaPelanggan')"
                 :readonly="!isFieldEditable('namaPelanggan')"
                 :class="{ 'editable-field': isFieldEditable('namaPelanggan') }"
                 :rules="isNewCustomer ? [val => !!val || 'Nama harus diisi'] : []"/>
      </div>
      <div class="q-mb-md">
        <q-input :model-value="editableFields.alamat !== undefined ? editableFields.alamat : alamat" 
                 label="Alamat" outlined dense
                 @update:model-value="handleFieldUpdate('alamat', $event)"
                 @blur="handleFieldBlur('alamat')"
                 @dblclick="handleDoubleClick('alamat')"
                 :readonly="!isFieldEditable('alamat')"
                 :class="{ 'editable-field': isFieldEditable('alamat') }"
                 type="textarea" rows="4"/>
      </div>
      <div class="q-mb-md">
        <q-input :model-value="nopol" label="Kendaraan" outlined dense readonly/>
      </div>
      <div class="row q-col-gutter-sm">
        <div class="q-mb-md col-6">
          <q-select :model-value="editableFields.merk !== undefined ? editableFields.merk : merk" 
                   label="Merk *" outlined dense
                   @update:model-value="handleFieldUpdate('merk', $event)"
                   @blur="handleFieldBlur('merk')"
                   @dblclick="handleDoubleClick('merk')"
                   :readonly="!isFieldEditable('merk')"
                   :class="{ 'editable-field': isFieldEditable('merk') }"
                   :options="merkOptions"
                   :option-label="opt => typeof opt === 'string' ? opt : opt"
                   option-value="merk" emit-value map-options use-input
                   input-debounce="300" @filter="filterMerk" 
                   :loading="loadingMerk"
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
          <q-select :model-value="editableFields.jenis !== undefined ? editableFields.jenis : jenis" 
                   label="Jenis" outlined dense
                   @update:model-value="handleFieldUpdate('jenis', $event)"
                   @blur="handleFieldBlur('jenis')"
                   @dblclick="handleDoubleClick('jenis')"
                   :readonly="!isFieldEditable('jenis')"
                   :class="{ 'editable-field': isFieldEditable('jenis') }"
                   :options="filteredJenisOptions"
                   :option-label="opt => typeof opt === 'string' ? opt : opt"
                   option-value="jenis" emit-value map-options use-input
                   input-debounce="300" @filter="filterJenis"
                   :loading="loadingJenis"
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

      <!-- Confirmation Dialog -->
      <q-dialog v-model="showConfirmDialog" persistent>
        <q-card>
          <q-card-section class="row items-center">
            <q-avatar icon="info" color="primary" text-color="white" />
            <span class="q-ml-sm">Update data pelanggan untuk field <strong>{{ fieldToUpdate }}</strong>?</span>
          </q-card-section>

          <q-card-actions align="right">
            <q-btn flat label="Tidak" color="primary" @click="handleDialogCancel" />
            <q-btn flat label="Ya, Update" color="green" @click="confirmUpdate" />
          </q-card-actions>
        </q-card>
      </q-dialog>
  </div>
</template>

<script setup>
import { ref, watchEffect } from 'vue'
import { useQuasar } from 'quasar'
import { api } from 'boot/axios'

const $q = useQuasar()

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

const emit = defineEmits(['update:namaPelanggan', 'update:alamat', 'update:merk', 'update:jenis', 'filter:merk', 'filter:jenis', 'check:vehicle', 'pelanggan-updated'])

// Inline editing state
const editableFields = ref({})
const editingFields = ref(new Set())
const showConfirmDialog = ref(false)
const fieldToUpdate = ref('')
const pendingValue = ref(null)
const pendingField = ref('')

// Ensure reactive objects are properly initialized
if (!editableFields.value) editableFields.value = {}
if (!editingFields.value) editingFields.value = new Set()

// Check if field is editable (for existing customers)
const isFieldEditable = (field) => {
  return !props.isNewCustomer && editingFields.value.has(field)
}

// Handle double click to make field editable
const handleDoubleClick = (field) => {
  if (!props.isNewCustomer) {
    editingFields.value.add(field)
  }
}

// Handle field update (immediate update for new customers, temporary for existing)
const handleFieldUpdate = (field, value) => {
  if (!props.isNewCustomer) {
    // For existing customers, store temporary value
    editableFields.value[field] = value
  } else {
    // For new customers, emit update directly
    emitFieldUpdate(field, value)
  }
}

// Handle field blur (trigger confirmation for existing customers)
const handleFieldBlur = (field) => {
  if (!props.isNewCustomer && editingFields.value.has(field)) {
    const currentValue = editableFields.value[field]
    const originalValue = props[field]
    
    // Only show confirmation if value actually changed
    if (currentValue !== undefined && currentValue !== originalValue) {
      pendingField.value = field
      pendingValue.value = currentValue
      fieldToUpdate.value = getFieldLabel(field)
      showConfirmDialog.value = true
    } else {
      // Revert if no change
      delete editableFields.value[field]
      editingFields.value.delete(field)
    }
  }
}

// Get field label for confirmation dialog
const getFieldLabel = (field) => {
  const labels = {
    namaPelanggan: 'Nama Pelanggan',
    alamat: 'Alamat',
    merk: 'Merk',
    jenis: 'Jenis'
  }
  return labels[field] || field
}

// Emit field update
const emitFieldUpdate = (field, value) => {
  emit(`update:${field}`, value)
  
  // Handle special cases for vehicle fields
  if (field === 'merk' && props.jenis) {
    emit('check:vehicle', value, props.jenis)
  } else if (field === 'jenis' && props.merk) {
    emit('check:vehicle', props.merk, value)
  }
}

// Confirm update to master pelanggan data
const confirmUpdate = async () => {
  try {
    // Emit the update to parent
    emitFieldUpdate(pendingField.value, pendingValue.value)
    
    // Update master pelanggan data
    await updateMasterPelanggan(pendingField.value, pendingValue.value)
    
    // Remove from editing fields
    editingFields.value.delete(pendingField.value)
    
    // Emit event to notify parent that pelanggan data was updated
    emit('pelanggan-updated', {
      field: pendingField.value,
      value: pendingValue.value,
      nopol: props.nopol
    })
    
    $q.notify({
      type: 'positive',
      message: `Data pelanggan berhasil diupdate untuk ${getFieldLabel(pendingField.value)}`
    })
  } catch (error) {
    // Revert changes on error
    delete editableFields.value[pendingField.value]
    editingFields.value.delete(pendingField.value)
    
    $q.notify({
      type: 'negative',
      message: 'Gagal update data pelanggan',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    showConfirmDialog.value = false
    pendingField.value = ''
    pendingValue.value = null
    fieldToUpdate.value = ''
  }
}

// Handle dialog cancellation
const handleDialogCancel = () => {
  // Revert changes
  delete editableFields.value[pendingField.value]
  editingFields.value.delete(pendingField.value)
  
  showConfirmDialog.value = false
  pendingField.value = ''
  pendingValue.value = null
  fieldToUpdate.value = ''
}

// Update master pelanggan data via API
const updateMasterPelanggan = async (field, value) => {
  if (!props.nopol) {
    throw new Error('No polisi tidak ditemukan')
  }
  
  const updateData = {
    [field]: value
  }
  
  const response = await api.put(`/api/pazaauto/pelanggan/by-nopol/${props.nopol}`, updateData)
  
  if (!response.data.success) {
    throw new Error(response.data.message || 'Failed to update pelanggan')
  }
  
  return response.data
}

// Original handlers for new customers
const handleMerkChange = (value) => {
  if (props.isNewCustomer) {
    emitFieldUpdate('merk', value)
  }
}

const handleJenisChange = (value) => {
  if (props.isNewCustomer) {
    emitFieldUpdate('jenis', value)
  }
}

const filterMerk = (inputVal, doneFn) => {
  emit('filter:merk', inputVal, doneFn)
}

const filterJenis = (inputVal, doneFn) => {
  emit('filter:jenis', inputVal, doneFn)
}

// Watch for prop changes to reset editing state when switching rows
watchEffect(() => {
  // Only reset if we have valid props and the component is properly mounted
  if (props.nopol && editableFields.value && editingFields.value) {
    // Reset all editing state when parent data changes (row selection changes)
    editableFields.value = {}
    editingFields.value.clear()
    showConfirmDialog.value = false
    pendingField.value = ''
    pendingValue.value = null
    fieldToUpdate.value = ''
  }
})
</script>

<style lang="sass" scoped>
.editable-field
  :deep(.q-field__control)
    border-color: #1976d2 !important
    background-color: #e3f2fd !important
  
  :deep(.q-field__native)
    cursor: text !important
</style>
