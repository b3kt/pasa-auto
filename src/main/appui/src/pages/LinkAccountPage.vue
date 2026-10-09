<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">Link your account</div>
    <div class="text-body2 text-grey-7 q-mb-lg">
      Find your existing customer record by your vehicle's plate number, or by the phone/email on file.
    </div>

    <q-banner v-if="profile?.linked" class="bg-positive text-white q-mb-md">
      Your account is already linked to {{ profile.namaPelanggan }}.
    </q-banner>

    <template v-else>
      <q-tabs v-model="tab" dense class="text-grey" active-color="primary" indicator-color="primary">
        <q-tab name="nopol" label="By plate number" />
        <q-tab name="contact" label="By phone/email" />
      </q-tabs>
      <q-separator class="q-mb-md" />

      <div v-if="tab === 'nopol'" class="q-gutter-md" style="max-width: 400px">
        <q-input v-model="nopol" label="Plate number (nopol)" outlined dense />
        <q-btn label="Search" color="primary" :loading="searching" @click="searchByNopol" />
      </div>

      <div v-else class="q-gutter-md" style="max-width: 400px">
        <q-input v-model="phone" label="Phone" outlined dense />
        <q-input v-model="contactEmail" label="Email" outlined dense />
        <q-btn label="Search" color="primary" :loading="searching" @click="searchByContact" />
      </div>

      <q-banner v-if="error" class="bg-negative text-white q-mt-md" dense>{{ error }}</q-banner>

      <q-list v-if="candidates.length" bordered class="q-mt-md" style="max-width: 500px">
        <q-item v-for="c in candidates" :key="c.pelangganId" clickable @click="confirmLink(c)">
          <q-item-section>
            <q-item-label>{{ c.maskedName }}</q-item-label>
            <q-item-label caption>
              <span v-if="c.nopol">Plate: {{ c.nopol }}</span>
              <span v-if="c.maskedPhone"> &middot; Phone: {{ c.maskedPhone }}</span>
              <span v-if="c.maskedEmail"> &middot; Email: {{ c.maskedEmail }}</span>
            </q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-btn flat color="primary" label="This is me" />
          </q-item-section>
        </q-item>
      </q-list>
    </template>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from 'boot/axios'
import { useAuthStore } from 'stores/auth-store'

const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()

const tab = ref('nopol')
const nopol = ref('')
const phone = ref('')
const contactEmail = ref('')
const searching = ref(false)
const error = ref('')
const candidates = ref([])
const profile = ref(null)

onMounted(loadProfile)

async function loadProfile() {
  try {
    const response = await api.get('/api/customer/profile')
    profile.value = response.data?.data
  } catch (e) {
    console.error('Failed to load profile', e)
  }
}

async function searchByNopol() {
  error.value = ''
  candidates.value = []
  searching.value = true
  try {
    const response = await api.get(`/api/customer/link/search-by-nopol/${encodeURIComponent(nopol.value)}`)
    const result = response.data?.data
    candidates.value = result ? [{ ...result, matchType: 'NOPOL', matchValue: nopol.value }] : []
    if (!result) error.value = 'No vehicle found with that plate number'
  } catch (e) {
    error.value = e.response?.data?.error || 'No vehicle found with that plate number'
  } finally {
    searching.value = false
  }
}

async function searchByContact() {
  error.value = ''
  candidates.value = []
  searching.value = true
  try {
    const response = await api.get('/api/customer/link/search-by-contact', {
      params: { phone: phone.value || undefined, email: contactEmail.value || undefined }
    })
    const results = (response.data?.data || []).map(r => ({
      ...r, matchType: 'CONTACT', matchValue: phone.value || contactEmail.value
    }))
    candidates.value = results
    if (!results.length) error.value = 'No matching customer record found'
  } catch (e) {
    error.value = e.response?.data?.error || 'Search failed'
  } finally {
    searching.value = false
  }
}

async function confirmLink(candidate) {
  try {
    const response = await api.post('/api/customer/link', {
      pelangganId: candidate.pelangganId,
      matchType: candidate.matchType,
      matchValue: candidate.matchValue
    })
    const result = response.data?.data
    if (result?.status === 'LINKED') {
      authStore.markLinked()
      $q.notify({ type: 'positive', message: result.message })
      router.push('/')
    } else {
      $q.notify({ type: 'info', message: result?.message || 'Request submitted for approval' })
      candidates.value = []
    }
  } catch (e) {
    error.value = e.response?.data?.error || 'Could not submit link request'
  }
}
</script>
