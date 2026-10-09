<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">Customer portal access</div>
    <q-card style="max-width: 500px">
      <q-card-section>
        <q-toggle v-model="enabled" label="Customer self-service portal enabled" :disable="loading" @update:model-value="save" />
        <div class="text-caption text-grey-7 q-mt-sm">
          Takes effect immediately for new logins; a customer already using the portal will see it become
          unavailable on their next request.
        </div>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { api } from 'boot/axios'

const $q = useQuasar()
const enabled = ref(false)
const loading = ref(false)

onMounted(async () => {
  try {
    const response = await api.get('/api/settings/customer-access')
    enabled.value = response.data?.data?.enabled === true
  } catch (e) {
    console.error('Failed to load customer access setting', e)
  }
})

async function save(value) {
  loading.value = true
  try {
    await api.put('/api/settings/customer-access', { enabled: value })
    $q.notify({ type: 'positive', message: `Customer portal ${value ? 'enabled' : 'disabled'}` })
  } catch (e) {
    enabled.value = !value
    $q.notify({ type: 'negative', message: e.response?.data?.error || 'Could not update setting' })
  } finally {
    loading.value = false
  }
}
</script>
