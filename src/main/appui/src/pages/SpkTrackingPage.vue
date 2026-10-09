<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">Service status</div>

    <q-banner v-if="notLinked" class="bg-warning text-white">
      Link your account first to track your service status.
      <template v-slot:action>
        <q-btn flat color="white" label="Link now" to="/link-account" />
      </template>
    </q-banner>

    <q-list v-else bordered separator>
      <q-item v-for="s in spks" :key="s.id">
        <q-item-section>
          <q-item-label>{{ s.noSpk }} &mdash; {{ s.nopol }}</q-item-label>
          <q-item-label caption>{{ s.tanggalJamSpk }}</q-item-label>
          <q-item-label caption v-if="s.keterangan">{{ s.keterangan }}</q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-badge :color="statusColor(s.statusSpk)">{{ s.statusSpk }}</q-badge>
        </q-item-section>
      </q-item>
      <q-item v-if="!spks.length">
        <q-item-section class="text-grey-7">No service records yet.</q-item-section>
      </q-item>
    </q-list>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from 'boot/axios'

const spks = ref([])
const notLinked = ref(false)

const statusColor = (status) => ({
  MENUNGGU: 'grey',
  PROSES: 'orange',
  SELESAI: 'positive',
  BATAL: 'negative'
})[status] || 'grey'

onMounted(async () => {
  try {
    const response = await api.get('/api/customer/spk')
    spks.value = response.data?.data || []
  } catch (e) {
    if (e.response?.status === 400) {
      notLinked.value = true
    } else {
      console.error('Failed to load service records', e)
    }
  }
})
</script>
