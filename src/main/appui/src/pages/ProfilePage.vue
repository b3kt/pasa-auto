<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">{{ $t('pages.profile.title') }}</div>

    <q-banner v-if="notLinked" class="bg-warning text-white q-mb-md">
      {{ $t('pages.profile.linkFirst') }}
      <template v-slot:action>
        <q-btn flat color="white" :label="$t('common.linkNow')" to="/link-account" />
      </template>
    </q-banner>

    <q-card v-else style="max-width: 500px">
      <q-card-section>
        <q-form @submit="save" class="q-gutter-sm">
          <q-input v-model="form.namaPelanggan" :label="$t('pages.profile.fullName')" outlined dense
            :rules="[val => !!val || $t('pages.profile.nameRequired')]" />
          <q-input v-model="form.alamat" :label="$t('pages.profile.address')" type="textarea" outlined dense />
          <q-input v-model="form.kota" :label="$t('pages.profile.city')" outlined dense />
          <q-input v-model="form.kodePos" :label="$t('pages.profile.postalCode')" outlined dense />
          <q-input v-model="form.telepon" :label="$t('pages.profile.phone')" outlined dense />
          <q-input v-model="form.noHp" :label="$t('pages.profile.mobile')" outlined dense />
          <q-input v-model="form.noTelepon" :label="$t('pages.profile.otherPhone')" outlined dense />
          <q-input v-model="form.email" :label="$t('pages.profile.email')" type="email" outlined dense />
          <q-input v-model="form.contactPerson" :label="$t('pages.profile.contactPerson')" outlined dense />

          <q-banner v-if="error" class="bg-negative text-white" dense>{{ error }}</q-banner>

          <q-btn :label="$t('common.save')" type="submit" color="primary" :loading="saving" />
        </q-form>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { api } from 'boot/axios'
import { useAuthStore } from 'stores/auth-store'

const $q = useQuasar()
const { t } = useI18n()
const authStore = useAuthStore()
const notLinked = ref(false)
const saving = ref(false)
const error = ref('')
const form = reactive({
  namaPelanggan: '', alamat: '', kota: '', kodePos: '',
  telepon: '', noHp: '', noTelepon: '', email: '', contactPerson: ''
})

onMounted(loadProfile)

async function loadProfile() {
  try {
    const response = await api.get('/api/customer/profile')
    const data = response.data?.data
    if (!data?.linked) {
      notLinked.value = true
      return
    }
    applyToForm(data)
  } catch (e) {
    console.error('Failed to load profile', e)
  }
}

function applyToForm(data) {
  form.namaPelanggan = data.namaPelanggan || ''
  form.alamat = data.alamat || ''
  form.kota = data.kota || ''
  form.kodePos = data.kodePos || ''
  form.telepon = data.telepon || ''
  form.noHp = data.noHp || ''
  form.noTelepon = data.noTelepon || ''
  form.email = data.pelangganEmail || ''
  form.contactPerson = data.contactPerson || ''
}

async function save() {
  error.value = ''
  saving.value = true
  try {
    const response = await api.put('/api/customer/profile', { ...form })
    const data = response.data?.data || {}
    applyToForm(data)
    if (data.linked) {
      // Saving may have created and linked a pelanggan record server-side; re-mint the access
      // token so it carries the pelangganId claim the rest of the portal scopes its queries by.
      authStore.markLinked()
      await authStore.refreshAccessToken()
    }
    $q.notify({ type: 'positive', message: t('pages.profile.updated') })
  } catch (e) {
    error.value = e.response?.data?.error || t('pages.profile.updateFailed')
  } finally {
    saving.value = false
  }
}
</script>
