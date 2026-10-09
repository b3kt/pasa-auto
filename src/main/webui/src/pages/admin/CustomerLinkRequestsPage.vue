<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">Customer link requests</div>
    <div class="text-body2 text-grey-7 q-mb-md">
      Requests where a customer tried to claim a pelanggan record already linked to another account.
    </div>

    <q-table :rows="requests" :columns="columns" row-key="id" flat bordered :loading="loading">
      <template v-slot:body-cell-status="props">
        <q-td :props="props">
          <q-badge :color="statusColor(props.value)">{{ props.value }}</q-badge>
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <template v-if="props.row.status === 'PENDING'">
            <q-btn dense flat color="positive" label="Approve" @click="approve(props.row)" />
            <q-btn dense flat color="negative" label="Reject" @click="reject(props.row)" />
          </template>
        </q-td>
      </template>
    </q-table>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { api } from 'boot/axios'

const $q = useQuasar()
const requests = ref([])
const loading = ref(false)

const columns = [
  { name: 'customerEmail', label: 'Customer', field: 'customerEmail', align: 'left' },
  { name: 'namaPelanggan', label: 'Claiming pelanggan', field: 'namaPelanggan', align: 'left' },
  { name: 'matchType', label: 'Matched by', field: 'matchType', align: 'left' },
  { name: 'matchValue', label: 'Value', field: 'matchValue', align: 'left' },
  { name: 'status', label: 'Status', field: 'status', align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

const statusColor = (status) => ({ PENDING: 'orange', APPROVED: 'positive', REJECTED: 'negative' })[status] || 'grey'

onMounted(load)

async function load() {
  loading.value = true
  try {
    const response = await api.get('/api/pazaauto/customer-link-requests', { params: { status: 'PENDING' } })
    requests.value = response.data?.data || []
  } finally {
    loading.value = false
  }
}

async function approve(row) {
  try {
    await api.post(`/api/pazaauto/customer-link-requests/${row.id}/approve`)
    $q.notify({ type: 'positive', message: 'Link request approved' })
    await load()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || 'Could not approve' })
  }
}

async function reject(row) {
  try {
    await api.post(`/api/pazaauto/customer-link-requests/${row.id}/reject`, {})
    $q.notify({ type: 'info', message: 'Link request rejected' })
    await load()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.error || 'Could not reject' })
  }
}
</script>
