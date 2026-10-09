<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">Service bookings</div>

    <q-banner v-if="notLinked" class="bg-warning text-white q-mb-md">
      Link your account first to book a service.
      <template v-slot:action>
        <q-btn flat color="white" label="Link now" to="/link-account" />
      </template>
    </q-banner>

    <template v-else>
      <q-card class="q-mb-lg" style="max-width: 500px">
        <q-card-section>
          <div class="text-subtitle1 q-mb-sm">Request a new booking</div>
          <q-form @submit="createBooking" class="q-gutter-sm">
            <q-select v-model="form.nopol" label="Plate number" outlined dense clearable emit-value map-options
              :options="vehicleOptions" :hint="!vehicles.length ? 'No registered vehicles found on your account' : ''" />
            <q-input v-model="form.requestedDateTime" label="Preferred date/time" type="datetime-local"
              outlined dense :rules="[val => !!val || 'Required']" />
            <q-input v-model="form.serviceType" label="Service type" outlined dense />
            <q-input v-model="form.description" label="Notes" type="textarea" outlined dense />
            <q-btn label="Request booking" type="submit" color="primary" :loading="submitting" />
          </q-form>
        </q-card-section>
      </q-card>

      <q-list bordered separator>
        <q-item v-for="b in bookings" :key="b.id">
          <q-item-section>
            <q-item-label>{{ b.nopol || 'No plate given' }} &mdash; {{ b.serviceType || 'General service' }}</q-item-label>
            <q-item-label caption>{{ formatDate(b.requestedDateTime) }}</q-item-label>
            <q-item-label caption v-if="b.staffNote">Note from shop: {{ b.staffNote }}</q-item-label>
          </q-item-section>
          <q-item-section side top>
            <q-badge :color="statusColor(b.status)">{{ b.status }}</q-badge>
            <q-btn v-if="b.status === 'REQUESTED'" flat dense size="sm" color="negative" label="Cancel"
              class="q-mt-xs" @click="cancelBooking(b.id)" />
          </q-item-section>
        </q-item>
        <q-item v-if="!bookings.length">
          <q-item-section class="text-grey-7">No bookings yet.</q-item-section>
        </q-item>
      </q-list>
    </template>
  </q-page>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { api } from 'boot/axios'

const $q = useQuasar()
const bookings = ref([])
const vehicles = ref([])
const notLinked = ref(false)
const submitting = ref(false)
const form = reactive({ nopol: null, requestedDateTime: '', serviceType: '', description: '' })

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

const formatDate = (value) => value ? new Date(value).toLocaleString() : ''

onMounted(() => {
  loadBookings()
  loadVehicles()
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
  submitting.value = true
  try {
    await api.post('/api/customer/bookings', {
      nopol: form.nopol || null,
      requestedDateTime: form.requestedDateTime,
      serviceType: form.serviceType || null,
      description: form.description || null
    })
    $q.notify({ type: 'positive', message: 'Booking requested' })
    form.nopol = null
    form.requestedDateTime = ''
    form.serviceType = ''
    form.description = ''
    await loadBookings()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || 'Could not request booking' })
  } finally {
    submitting.value = false
  }
}

async function cancelBooking(id) {
  try {
    await api.post(`/api/customer/bookings/${id}/cancel`)
    $q.notify({ type: 'info', message: 'Booking cancelled' })
    await loadBookings()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || 'Could not cancel booking' })
  }
}
</script>
