<template>
  <q-page padding>
    <q-card flat bordered>
      <q-card-section>
        <div class="text-h6">Persetujuan Akun Google</div>
        <div class="text-caption text-grey-7">
          Akun yang dibuat lewat login Google dan menunggu persetujuan. Akun yang disetujui belum
          memiliki hak akses apa pun &mdash; tetapkan role lewat halaman Users.
        </div>
      </q-card-section>

      <q-separator />

      <q-table :rows="rows" :columns="columns" row-key="id" :loading="loading" flat
               :rows-per-page-options="[10, 25, 50]" no-data-label="Tidak ada akun yang menunggu persetujuan">
        <template v-slot:body-cell-createdAt="props">
          <q-td :props="props">{{ formatDateTime(props.row.createdAt) }}</q-td>
        </template>

        <template v-slot:body-cell-actions="props">
          <q-td :props="props" class="q-gutter-xs">
            <q-btn dense color="positive" icon="check" label="Setujui" no-caps
                   :loading="actingOn === props.row.id" @click="confirm(props.row, 'approve')" />
            <q-btn dense flat color="negative" icon="block" label="Tolak" no-caps
                   :loading="actingOn === props.row.id" @click="confirm(props.row, 'reject')" />
          </q-td>
        </template>
      </q-table>
    </q-card>

    <GenericDialog v-model="showConfirm" :title="pendingAction === 'approve' ? 'Setujui akun' : 'Tolak akun'"
                   min-width="400px" position="standard">
      <template v-if="pendingAction === 'approve'">
        Setujui <strong>{{ selected?.username }}</strong> ({{ selected?.email }})? Akun menjadi aktif,
        tetapi belum bisa mengakses apa pun sampai Anda menetapkan role.
      </template>
      <template v-else>
        Tolak <strong>{{ selected?.username }}</strong> ({{ selected?.email }})? Sesi yang sedang
        berjalan untuk akun ini akan diakhiri.
      </template>
      <template #actions>
        <q-btn flat label="Batal" color="primary" @click="showConfirm = false" />
        <q-btn flat :label="pendingAction === 'approve' ? 'Setujui' : 'Tolak'"
               :color="pendingAction === 'approve' ? 'positive' : 'negative'" @click="act" />
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar, date } from 'quasar'
import { api } from 'boot/axios'
import GenericDialog from 'components/GenericDialog.vue'

const $q = useQuasar()

const rows = ref([])
const loading = ref(false)
const actingOn = ref(null)
const showConfirm = ref(false)
const selected = ref(null)
const pendingAction = ref('approve')

const columns = [
  { name: 'username', label: 'Username', field: 'username', align: 'left', sortable: true },
  { name: 'email', label: 'Email', field: 'email', align: 'left', sortable: true },
  { name: 'createdAt', label: 'Didaftarkan', field: 'createdAt', align: 'left', sortable: true },
  { name: 'actions', label: 'Aksi', field: 'actions', align: 'right' }
]

const formatDateTime = (value) => (value ? date.formatDate(value, 'YYYY-MM-DD HH:mm') : '')

const fetchPending = async () => {
  loading.value = true
  try {
    const response = await api.get('/api/users/pending')
    rows.value = response.data?.data || []
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Gagal memuat daftar persetujuan',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    loading.value = false
  }
}

const confirm = (row, action) => {
  selected.value = row
  pendingAction.value = action
  showConfirm.value = true
}

const act = async () => {
  const row = selected.value
  showConfirm.value = false
  actingOn.value = row.id

  try {
    await api.post(`/api/users/${row.id}/${pendingAction.value}`)
    $q.notify({
      type: 'positive',
      message: pendingAction.value === 'approve'
        ? `${row.username} disetujui. Tetapkan role lewat halaman Users.`
        : `${row.username} ditolak.`
    })
    await fetchPending()
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Gagal memproses akun',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    actingOn.value = null
  }
}

onMounted(fetchPending)
</script>
