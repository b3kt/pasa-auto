<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">My vehicles</div>

    <q-banner v-if="notLinked" class="bg-warning text-white">
      Link your account first to see your vehicles.
      <template v-slot:action>
        <q-btn flat color="white" label="Link now" to="/link-account" />
      </template>
    </q-banner>

    <q-list v-else bordered separator>
      <q-item v-for="v in vehicles" :key="v.id">
        <q-item-section>
          <q-item-label>{{ v.nopol }} &mdash; {{ v.merk }} {{ v.jenis }}</q-item-label>
          <q-item-label caption>{{ v.model }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item v-if="!vehicles.length">
        <q-item-section class="text-grey-7">No vehicles found on your record.</q-item-section>
      </q-item>
    </q-list>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from 'boot/axios'

const vehicles = ref([])
const notLinked = ref(false)

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
</script>
