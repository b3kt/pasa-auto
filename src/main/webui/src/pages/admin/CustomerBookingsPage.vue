<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">Customer service bookings</div>

    <q-table :rows="bookings" :columns="columns" row-key="id" flat bordered :loading="loading">
      <template v-slot:body-cell-status="props">
        <q-td :props="props">
          <q-badge :color="statusColor(props.value)">{{ props.value }}</q-badge>
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <template v-if="props.row.status === 'REQUESTED'">
            <q-btn dense flat color="positive" label="Confirm" @click="confirm(props.row)" />
            <q-btn dense flat color="negative" label="Reject" @click="reject(props.row)" />
          </template>
          <q-btn v-else-if="props.row.status === 'CONFIRMED'" dense flat color="primary" label="Convert to SPK"
            @click="openConvert(props.row)" />
        </q-td>
      </template>
    </q-table>

    <q-dialog v-model="convertDialog">
      <q-card style="min-width: 350px">
        <q-card-section class="text-h6">Convert to work order</q-card-section>
        <q-card-section class="q-gutter-sm">
          <q-input v-model="convertForm.namaKaryawan" label="Mechanic name" outlined dense />
          <q-input v-model.number="convertForm.km" label="Odometer (km)" type="number" outlined dense />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" v-close-popup />
          <q-btn color="primary" label="Convert" @click="convert" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { api } from 'boot/axios'

const $q = useQuasar()
const bookings = ref([])
const loading = ref(false)
const convertDialog = ref(false)
const convertForm = reactive({ id: null, namaKaryawan: '', km: null })

const columns = [
  { name: 'customerEmail', label: 'Customer', field: 'customerEmail', align: 'left' },
  { name: 'namaPelanggan', label: 'Pelanggan', field: 'namaPelanggan', align: 'left' },
  { name: 'nopol', label: 'Plate', field: 'nopol', align: 'left' },
  { name: 'requestedDateTime', label: 'Requested for', field: 'requestedDateTime', align: 'left',
    format: (v) => v ? new Date(v).toLocaleString() : '' },
  { name: 'status', label: 'Status', field: 'status', align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

const statusColor = (status) => ({
  REQUESTED: 'grey', CONFIRMED: 'primary', CONVERTED: 'positive', REJECTED: 'negative', CANCELLED: 'negative'
})[status] || 'grey'

onMounted(load)

async function load() {
  loading.value = true
  try {
    const response = await api.get('/api/pazaauto/customer-bookings')
    bookings.value = response.data?.data || []
  } finally {
    loading.value = false
  }
}

async function confirm(row) {
  try {
    await api.post(`/api/pazaauto/customer-bookings/${row.id}/confirm`, {})
    $q.notify({ type: 'positive', message: 'Booking confirmed' })
    await load()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || 'Could not confirm' })
  }
}

async function reject(row) {
  try {
    await api.post(`/api/pazaauto/customer-bookings/${row.id}/reject`, {})
    $q.notify({ type: 'info', message: 'Booking rejected' })
    await load()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || 'Could not reject' })
  }
}

function openConvert(row) {
  convertForm.id = row.id
  convertForm.namaKaryawan = ''
  convertForm.km = null
  convertDialog.value = true
}

async function convert() {
  try {
    await api.post(`/api/pazaauto/customer-bookings/${convertForm.id}/convert-to-spk`, {
      namaKaryawan: convertForm.namaKaryawan || null,
      km: convertForm.km
    })
    $q.notify({ type: 'positive', message: 'Converted to work order' })
    convertDialog.value = false
    await load()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || 'Could not convert' })
  }
}
</script>
