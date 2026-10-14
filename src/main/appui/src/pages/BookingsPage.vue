<template>
  <q-page padding>
    <div class="row items-center justify-between q-mb-md">
      <div class="text-h6">{{ $t('pages.bookings.title') }}</div>
      <q-btn v-if="!notLinked" round dense size="sm" color="primary" icon="add" @click="showDialog = true" />
    </div>

    <q-banner v-if="notLinked" class="bg-warning text-white">
      {{ $t('pages.bookings.linkFirst') }}
      <template v-slot:action>
        <q-btn flat color="white" :label="$t('common.linkNow')" to="/link-account" />
      </template>
    </q-banner>

    <q-list v-else bordered separator>
      <q-item v-for="b in bookings" :key="b.id">
        <q-item-section>
          <q-item-label>{{ b.nopol || $t('pages.bookings.noPlate') }} &mdash; {{ b.serviceType || $t('pages.bookings.generalService') }}</q-item-label>
          <q-item-label caption>{{ formatDate(b.requestedDateTime) }}</q-item-label>
          <q-item-label caption v-if="b.staffNote">{{ $t('pages.bookings.staffNote', { note: b.staffNote }) }}</q-item-label>
        </q-item-section>
        <q-item-section side top>
          <q-badge :color="statusColor(b.status)">{{ statusLabel(b.status) }}</q-badge>
          <q-btn v-if="b.status === 'REQUESTED'" flat dense size="sm" color="negative" :label="$t('common.cancel')"
            class="q-mt-xs" @click="cancelBooking(b.id)" />
        </q-item-section>
      </q-item>
      <q-item v-if="!bookings.length">
        <q-item-section class="text-grey-7">{{ $t('pages.bookings.none') }}</q-item-section>
      </q-item>
    </q-list>

    <q-dialog v-model="showDialog" persistent :maximized="$q.screen.lt.sm" @show="onDialogShow" @hide="onDialogHide">
      <q-card class="column no-wrap" :style="$q.screen.lt.sm ? 'width: 100%; height: 100%' : 'min-width: 350px; max-width: 500px'">
        <q-card-section class="row items-center justify-between">
          <div class="text-h6">{{ $t('pages.bookings.requestTitle') }}</div>
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-separator />

        <q-card-section class="col scroll q-pa-md">
          <q-form @submit="createBooking" class="q-gutter-sm">
            <q-select v-model="form.nopol" :label="$t('pages.bookings.plateNumber')" outlined dense clearable
              emit-value map-options :options="vehicleOptions"
              :hint="!vehicles.length ? $t('pages.bookings.noVehicles') : ''" />
            <q-input v-model.number="form.km" :label="$t('pages.bookings.km')" type="number" outlined dense
              :rules="[val => (val !== null && val !== '' && val >= 0) || $t('pages.bookings.kmRequired')]" />
            <q-input v-model="form.requestedDateTime" :label="$t('pages.bookings.preferredDateTime')"
              type="datetime-local" outlined dense :rules="[val => !!val || $t('common.required')]" />
            <q-input v-model="form.serviceType" :label="$t('pages.bookings.serviceType')" outlined dense />
            <q-input v-model="form.description" :label="$t('pages.bookings.notes')" type="textarea" outlined dense />
          </q-form>
        </q-card-section>

        <q-separator />
        <q-card-actions align="right">
          <q-btn flat :label="$t('common.cancel')" v-close-popup @click="onDialogHide" />
          <q-btn flat :label="$t('pages.bookings.submit')" color="primary" :loading="submitting"
            :disable="submitting" @click="createBooking" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { api } from 'boot/axios'

const $q = useQuasar()
const { t, te, locale } = useI18n()
const bookings = ref([])
const vehicles = ref([])
const notLinked = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const form = reactive({ nopol: null, km: null, requestedDateTime: '', serviceType: '', description: '' })

const vehicleOptions = computed(() => vehicles.value.map(v => ({
  label: [v.nopol, [v.merk, v.jenis].filter(Boolean).join(' ')].filter(Boolean).join(' — '),
  value: v.nopol
})))

const statusColor = (status) => ({
  REQUESTED: 'grey',
  CONFIRMED: 'primary',
  CONVERTED: 'positive',
  REJECTED: 'negative',
  CANCELLED: 'negative'
})[status] || 'grey'

const statusLabel = (status) => te(`pages.bookings.status.${status}`)
  ? t(`pages.bookings.status.${status}`)
  : status

const formatDate = (value) => value ? new Date(value).toLocaleString(locale.value) : ''

onMounted(() => {
  loadBookings()
})

async function loadBookings() {
  try {
    const response = await api.get('/api/customer/bookings')
    bookings.value = response.data?.data || []
  } catch (e) {
    if (e.response?.status === 400) {
      notLinked.value = true
    } else {
      console.error('Failed to load bookings', e)
    }
  }
}

async function loadVehicles() {
  try {
    const response = await api.get('/api/customer/vehicles')
    vehicles.value = response.data?.data || []
  } catch (e) {
    if (e.response?.status !== 400) {
      console.error('Failed to load vehicles', e)
    }
  }
}

async function createBooking() {
  // The submit button lives outside the q-form, so the input rules never block it here
  if (form.km === null || form.km === '' || Number(form.km) < 0) {
    $q.notify({ type: 'warning', message: t('pages.bookings.kmRequired') })
    return
  }
  submitting.value = true
  try {
    await api.post('/api/customer/bookings', {
      nopol: form.nopol || null,
      km: Number(form.km),
      requestedDateTime: form.requestedDateTime,
      serviceType: form.serviceType || null,
      description: form.description || null
    })
    $q.notify({ type: 'positive', message: t('pages.bookings.requested') })
    showDialog.value = false
    form.nopol = null
    form.km = null
    form.requestedDateTime = ''
    form.serviceType = ''
    form.description = ''
    await loadBookings()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || t('pages.bookings.requestFailed') })
  } finally {
    submitting.value = false
  }
}

async function cancelBooking(id) {
  try {
    await api.post(`/api/customer/bookings/${id}/cancel`)
    $q.notify({ type: 'info', message: t('pages.bookings.cancelled') })
    await loadBookings()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || t('pages.bookings.cancelFailed') })
  }
}

function onDialogShow() {
  loadVehicles()
}

function onDialogHide() {
  form.nopol = null
  form.km = null
  form.requestedDateTime = ''
  form.serviceType = ''
  form.description = ''
}
</script>
