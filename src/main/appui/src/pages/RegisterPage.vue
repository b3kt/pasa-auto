<template>
  <q-page class="flex flex-center">
    <q-card class="register-card no-shadow" style="min-width: 350px">
      <q-card-section>
        <div class="text-h6 text-center q-mb-lg">{{ $t('pages.register.title') }}</div>
        <q-form @submit="onSubmit" class="q-gutter-md">
          <q-input v-model="fullName" :label="$t('pages.register.fullName')"
            :rules="[val => !!val || $t('pages.register.fullNameRequired')]" outlined dense hide-bottom-space />

          <!-- Email, phone or both; each one can be used to sign in -->
          <q-input v-model="email" :label="$t('pages.register.email')" type="email" autocomplete="email"
            :rules="[() => hasContact() || $t('pages.register.contactRequired')]"
            outlined dense hide-bottom-space />

          <q-input v-model="phone" :label="$t('pages.register.phone')" type="tel" autocomplete="tel"
            :hint="$t('pages.register.contactHint')"
            :rules="[() => hasContact() || $t('pages.register.contactRequired')]"
            outlined dense />

          <q-input v-model="password" :label="$t('pages.register.password')" type="password"
            :rules="[val => !!val && val.length >= 8 || $t('pages.register.passwordRule')]"
            outlined dense hide-bottom-space />

          <q-banner v-if="error" class="bg-negative text-white" dense>{{ error }}</q-banner>

          <div>
            <q-btn :label="$t('pages.register.submit')" type="submit" color="primary" class="full-width"
              :loading="loading" />
          </div>
          <div class="text-center">
            <router-link to="/login">{{ $t('pages.register.hasAccount') }}</router-link>
          </div>
        </q-form>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from 'stores/auth-store'

const router = useRouter()
const $q = useQuasar()
const { t } = useI18n()
const authStore = useAuthStore()

const fullName = ref('')
const email = ref('')
const phone = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

const hasContact = () => !!email.value?.trim() || !!phone.value?.trim()

const onSubmit = async () => {
  error.value = ''
  loading.value = true
  try {
    const result = await authStore.register({
      fullName: fullName.value,
      email: email.value?.trim() || null,
      phone: phone.value?.trim() || null,
      password: password.value
    })
    if (result.success) {
      $q.notify({ type: 'positive', message: t('pages.register.success') })
      router.push('/link-account')
    } else {
      error.value = result.error || t('pages.register.failed')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-card {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}
</style>
