<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">{{ $t('pages.linkAccount.title') }}</div>
    <div class="text-body2 text-grey-7 q-mb-lg">
      {{ $t('pages.linkAccount.intro') }}
    </div>

    <q-banner v-if="profile?.linked" class="bg-positive text-white q-mb-md">
      {{ $t('pages.linkAccount.alreadyLinked', { name: profile.namaPelanggan }) }}
    </q-banner>

    <template v-else>
      <div class="q-gutter-md" style="max-width: 400px">
        <q-input v-model="nopol" :label="$t('pages.linkAccount.plateNumber')" outlined dense
          :rules="[val => !!val || $t('pages.linkAccount.plateRequired')]" @keyup.enter="search" />
        <div class="row q-gutter-sm">
          <q-btn :label="$t('pages.linkAccount.verify')" color="primary" :loading="searching" @click="search" />
          <q-btn flat :label="$t('pages.linkAccount.skip')" color="grey-8" @click="skipLinking" />
        </div>
      </div>

      <q-banner v-if="error" class="bg-negative text-white q-mt-md" dense>{{ error }}</q-banner>

      <q-card v-if="candidate" class="q-mt-md" style="max-width: 500px">
        <q-card-section>
          <div class="text-subtitle2">{{ candidate.maskedName }}</div>
          <div class="text-caption text-grey-7">
            <span v-if="candidate.nopol">{{ $t('pages.linkAccount.plate', { nopol: candidate.nopol }) }}</span>
            <span v-if="candidate.maskedPhone"> &middot; {{ $t('pages.linkAccount.phone', { phone: candidate.maskedPhone }) }}</span>
          </div>
        </q-card-section>
        <q-card-actions>
          <q-btn :label="$t('pages.linkAccount.confirm')" color="primary" :loading="linking" @click="confirmLink" />
        </q-card-actions>
      </q-card>
    </template>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { api } from 'boot/axios'
import { useAuthStore } from 'stores/auth-store'

const router = useRouter()
const $q = useQuasar()
const { t } = useI18n()
const authStore = useAuthStore()

const profile = ref(null)
const nopol = ref('')
const candidate = ref(null)
const searching = ref(false)
const linking = ref(false)
const error = ref('')

onMounted(loadProfile)

async function loadProfile() {
  try {
    const response = await api.get('/api/customer/profile')
    profile.value = response.data?.data
  } catch (e) {
    console.error('Failed to load profile', e)
  }
}

async function search() {
  error.value = ''
  candidate.value = null
  searching.value = true
  try {
    const response = await api.get(`/api/customer/link/search-by-nopol/${encodeURIComponent(nopol.value)}`)
    const result = response.data?.data
    if (result) {
      candidate.value = result
    } else {
      error.value = t('pages.linkAccount.notFoundOrLinked')
    }
  } catch (e) {
    error.value = e.response?.data?.error || t('pages.linkAccount.notFoundOrLinked')
  } finally {
    searching.value = false
  }
}

async function skipLinking() {
  try {
    await api.post('/api/customer/link/skip')
    // Skipping links a placeholder record server-side; re-mint the access token so it carries the
    // pelangganId claim the rest of the portal scopes its queries by.
    authStore.markLinked()
    await authStore.refreshAccessToken()
    $q.notify({ type: 'info', message: t('pages.linkAccount.skipped') })
  } catch (e) {
    console.error('Failed to skip linking', e)
  } finally {
    router.push('/')
  }
}

async function confirmLink() {
  error.value = ''
  linking.value = true
  try {
    const response = await api.post('/api/customer/link', {
      pelangganId: candidate.value.pelangganId,
      matchType: 'NOPOL',
      matchValue: nopol.value
    })
    const result = response.data?.data
    if (result?.status === 'LINKED') {
      authStore.markLinked()
      await authStore.refreshAccessToken()
      $q.notify({ type: 'positive', message: result.message })
      router.push('/')
    } else {
      $q.notify({ type: 'info', message: result?.message || t('pages.linkAccount.requestSubmitted') })
      candidate.value = null
    }
  } catch (e) {
    error.value = e.response?.data?.error || t('pages.linkAccount.submitFailed')
  } finally {
    linking.value = false
  }
}
</script>
