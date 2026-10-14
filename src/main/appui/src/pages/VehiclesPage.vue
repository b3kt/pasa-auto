<template>
  <q-page padding>
    <div class="row items-center justify-between q-mb-md">
      <div class="text-h6">{{ $t('pages.vehicles.title') }}</div>
      <q-btn v-if="!notLinked" round dense size="sm" color="primary" icon="add" @click="openAddDialog" />
    </div>

    <q-banner v-if="notLinked" class="bg-warning text-white">
      {{ $t('pages.vehicles.linkFirst') }}
      <template v-slot:action>
        <q-btn flat color="white" :label="$t('common.linkNow')" to="/link-account" />
      </template>
    </q-banner>

    <q-list v-else bordered separator>
      <q-item v-for="v in vehicles" :key="v.id" clickable @click="openEditDialog(v)">
        <q-item-section>
          <q-item-label>{{ v.nopol }} &mdash; {{ v.merk }} {{ v.jenis }}</q-item-label>
          <q-item-label caption>{{ v.model }}</q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-btn flat round dense color="negative" icon="delete" @click.stop="confirmDelete(v)" />
        </q-item-section>
      </q-item>
      <q-item v-if="!vehicles.length">
        <q-item-section class="text-grey-7">{{ $t('pages.vehicles.none') }}</q-item-section>
      </q-item>
    </q-list>

    <q-dialog v-model="showDialog" persistent :maximized="$q.screen.lt.sm" @show="onDialogShow" @hide="onDialogHide">
      <q-card class="column no-wrap" :style="$q.screen.lt.sm ? 'width: 100%; height: 100%' : 'min-width: 350px; max-width: 500px'">
        <q-card-section class="row items-center justify-between">
          <div class="text-h6">{{ isEditMode ? $t('pages.vehicles.editVehicle') : $t('pages.vehicles.addVehicle') }}</div>
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-separator />

        <q-card-section class="col scroll q-pa-md">
          <q-form @submit="saveVehicle" class="q-gutter-sm">
            <q-input v-model="form.nopol" :label="$t('pages.vehicles.plateNumber')" outlined dense
              :rules="[val => !!val || $t('common.required')]" :disable="isEditMode" />
            <q-select v-model="form.merk" :label="$t('pages.vehicles.brand')" outlined dense clearable
              emit-value map-options :options="merkOptions" :loading="loadingMerk"
              @update:model-value="onMerkChange" />
            <q-select v-model="form.jenis" :label="$t('pages.vehicles.type')" outlined dense clearable
              emit-value map-options :options="jenisOptions" :loading="loadingJenis"
              :disable="!form.merk" />
          </q-form>
        </q-card-section>

        <q-separator />
        <q-card-actions align="right">
          <q-btn flat :label="$t('common.cancel')" v-close-popup @click="onDialogHide" />
          <q-btn flat :label="isEditMode ? $t('common.save') : $t('pages.vehicles.register')" color="primary"
            :loading="submitting" :disable="submitting" @click="saveVehicle" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { api } from 'boot/axios'

const $q = useQuasar()
const { t } = useI18n()
const vehicles = ref([])
const notLinked = ref(false)
const submitting = ref(false)
const loadingMerk = ref(false)
const loadingJenis = ref(false)
const merkList = ref([])
const jenisList = ref([])
const showDialog = ref(false)
const isEditMode = ref(false)
const originalNopol = ref('')
const form = reactive({ nopol: '', merk: '', jenis: '' })

const merkOptions = computed(() => merkList.value.map(m => ({ label: m, value: m })))
const jenisOptions = computed(() => jenisList.value.map(j => ({ label: j, value: j })))

onMounted(async () => {
  try {
    const response = await api.get('/api/customer/vehicles')
    vehicles.value = response.data?.data || []
  } catch (e) {
    if (e.response?.status === 400) {
      notLinked.value = true
    } else {
      console.error('Failed to load vehicles', e)
    }
  }
})

async function loadMerkList() {
  loadingMerk.value = true
  try {
    const response = await api.get('/api/customer/vehicles/merk')
    merkList.value = response.data?.data || []
  } catch (e) {
    console.error('Failed to load merk list', e)
  } finally {
    loadingMerk.value = false
  }
}

async function loadJenisList(merk = null) {
  loadingJenis.value = true
  try {
    const url = merk ? `/api/customer/vehicles/jenis?merk=${encodeURIComponent(merk)}` : '/api/customer/vehicles/jenis'
    const response = await api.get(url)
    jenisList.value = response.data?.data || []
  } catch (e) {
    console.error('Failed to load jenis list', e)
  } finally {
    loadingJenis.value = false
  }
}

function onMerkChange() {
  form.jenis = ''
  if (form.merk) {
    loadJenisList(form.merk)
  } else {
    jenisList.value = []
  }
}

async function saveVehicle() {
  submitting.value = true
  try {
    if (isEditMode.value) {
      await api.put(`/api/customer/vehicles/${encodeURIComponent(originalNopol.value)}`, {
        merk: form.merk || null,
        jenis: form.jenis || null
      })
      $q.notify({ type: 'positive', message: t('pages.vehicles.updated') })
    } else {
      await api.post('/api/customer/vehicles', {
        nopol: form.nopol,
        merk: form.merk || null,
        jenis: form.jenis || null
      })
      $q.notify({ type: 'positive', message: t('pages.vehicles.registered') })
    }
    showDialog.value = false
    resetForm()
    const response = await api.get('/api/customer/vehicles')
    vehicles.value = response.data?.data || []
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || t('pages.vehicles.saveFailed') })
  } finally {
    submitting.value = false
  }
}

async function confirmDelete(vehicle) {
  $q.dialog({
    title: t('pages.vehicles.deleteConfirmTitle'),
    message: t('pages.vehicles.deleteConfirmMessage', { nopol: vehicle.nopol }),
    cancel: true,
    persistent: true,
    maximized: $q.screen.lt.sm
  }).onOk(async () => {
    try {
      await api.delete(`/api/customer/vehicles/${encodeURIComponent(vehicle.nopol)}`)
      $q.notify({ type: 'positive', message: t('pages.vehicles.deleted') })
      const response = await api.get('/api/customer/vehicles')
      vehicles.value = response.data?.data || []
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.error || t('pages.vehicles.deleteFailed') })
    }
  })
}

function openAddDialog() {
  isEditMode.value = false
  resetForm()
  showDialog.value = true
}

function openEditDialog(vehicle) {
  isEditMode.value = true
  originalNopol.value = vehicle.nopol
  form.nopol = vehicle.nopol
  form.merk = vehicle.merk
  form.jenis = vehicle.jenis
  showDialog.value = true
}

function onDialogShow() {
  loadMerkList()
  if (isEditMode.value && form.merk) {
    loadJenisList(form.merk)
  }
}

function onDialogHide() {
  resetForm()
}

function resetForm() {
  form.nopol = ''
  form.merk = ''
  form.jenis = ''
  jenisList.value = []
  originalNopol.value = ''
}
</script>
