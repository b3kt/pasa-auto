<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">My profile</div>

    <q-banner v-if="notLinked" class="bg-warning text-white q-mb-md">
      Link your account first to manage your customer profile.
      <template v-slot:action>
        <q-btn flat color="white" label="Link now" to="/link-account" />
      </template>
    </q-banner>

    <q-card v-else style="max-width: 500px">
      <q-card-section>
        <q-form @submit="save" class="q-gutter-sm">
          <q-input v-model="form.namaPelanggan" label="Full name" outlined dense
            :rules="[val => !!val || 'Name is required']" />
          <q-input v-model="form.alamat" label="Address" type="textarea" outlined dense />
          <q-input v-model="form.kota" label="City" outlined dense />
          <q-input v-model="form.kodePos" label="Postal code" outlined dense />
          <q-input v-model="form.telepon" label="Phone" outlined dense />
          <q-input v-model="form.noHp" label="Mobile" outlined dense />
          <q-input v-model="form.noTelepon" label="Other phone" outlined dense />
          <q-input v-model="form.email" label="Email" type="email" outlined dense />
          <q-input v-model="form.contactPerson" label="Contact person" outlined dense />

          <q-banner v-if="error" class="bg-negative text-white" dense>{{ error }}</q-banner>

          <q-btn label="Save changes" type="submit" color="primary" :loading="saving" />
        </q-form>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { api } from 'boot/axios'

const $q = useQuasar()
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
    applyToForm(response.data?.data || {})
    $q.notify({ type: 'positive', message: 'Profile updated' })
  } catch (e) {
    error.value = e.response?.data?.error || 'Could not update profile'
  } finally {
    saving.value = false
  }
}
</script>
