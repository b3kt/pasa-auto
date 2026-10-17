<template>
  <!-- Full-height form layout like the add/edit dialogs: title on top, scrolling fields,
       actions pinned to the bottom. -->
  <q-page class="column no-wrap" :style-fn="fullHeight">
    <div v-if="notLinked" class="q-pa-md">
      <div class="text-h6 q-mb-md">{{ $t('pages.profile.title') }}</div>
      <q-banner class="bg-warning text-white">
        {{ $t('pages.profile.linkFirst') }}
        <template v-slot:action>
          <q-btn flat color="white" :label="$t('common.linkNow')" to="/link-account" />
        </template>
      </q-banner>
    </div>

    <q-form v-else @submit="save" class="col column no-wrap">
      <div class="q-pa-md">
        <div class="text-h6">{{ $t('pages.profile.title') }}</div>
      </div>
      <q-separator />

      <div class="col scroll q-pa-md">
        <div class="q-gutter-sm profile-fields">
          <q-input v-model="form.namaPelanggan" :label="$t('pages.profile.fullName')" outlined dense
            :rules="[val => !!val || $t('pages.profile.nameRequired')]" />
          <q-input v-model="form.alamat" :label="$t('pages.profile.address')" type="textarea" outlined dense />
          <q-input v-model="form.kota" :label="$t('pages.profile.city')" outlined dense />
          <q-input v-model="form.kodePos" :label="$t('pages.profile.postalCode')" outlined dense />
          <q-input v-model="form.noHp" :label="$t('pages.profile.mobile')" :hint="$t('pages.profile.mobileHint')"
            type="tel" outlined dense />
          <q-input v-if="email" :model-value="email" :label="$t('pages.profile.email')"
            :hint="$t('pages.profile.emailHint')" type="email" outlined dense readonly />
          <q-input v-model="form.contactPerson" :label="$t('pages.profile.contactPerson')" outlined dense />

          <q-banner v-if="error" class="bg-negative text-white" dense>{{ error }}</q-banner>
        </div>
      </div>

      <q-separator />
      <div class="row justify-end q-gutter-sm q-pa-sm">
        <q-btn flat :label="$t('common.cancel')" :disable="saving" @click="resetForm" />
        <q-btn flat :label="$t('common.save')" type="submit" color="primary" :loading="saving" />
      </div>
    </q-form>
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
// Phone and email belong to the account (ADR-0012): the mobile number is saved on the account and
// the email is the login email, shown read-only.
const form = reactive({
  namaPelanggan: '', alamat: '', kota: '', kodePos: '', noHp: '', contactPerson: ''
})
const email = ref('')
// Last values loaded from / saved to the server, so Cancel can discard unsaved edits.
let loaded = {}

// Pin the page to the viewport height (instead of Quasar's default min-height) so the fields
// scroll inside it and the action bar stays at the bottom.
function fullHeight(offset, height) {
  return { height: `${height - offset}px` }
}

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
  loaded = data
  form.namaPelanggan = data.namaPelanggan || ''
  form.alamat = data.alamat || ''
  form.kota = data.kota || ''
  form.kodePos = data.kodePos || ''
  form.noHp = data.noHp || ''
  email.value = data.email || ''
  form.contactPerson = data.contactPerson || ''
}

function resetForm() {
  error.value = ''
  applyToForm(loaded)
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

<style scoped>
.profile-fields {
  max-width: 600px;
}
</style>
