<template>
  <q-page class="flex flex-center">
    <q-card class="login-card no-shadow" style="min-width: 350px">
      <q-card-section>
        <div class="text-h6 text-center q-mb-lg">{{ $t('pages.login.title') }}</div>
        <q-form @submit="onSubmit" class="q-gutter-md">
          <q-input v-model="identifier" :label="$t('pages.login.identifier')" autocomplete="username"
            :rules="[val => !!val || $t('pages.login.identifierRequired')]" outlined dense hide-bottom-space>
            <template v-slot:prepend>
              <q-icon name="person" />
            </template>
          </q-input>

          <q-input v-model="password" :label="$t('pages.login.password')" type="password"
            :rules="[val => !!val || $t('pages.login.passwordRequired')]" outlined dense hide-bottom-space>
            <template v-slot:prepend>
              <q-icon name="lock" />
            </template>
          </q-input>

          <q-banner v-if="error" class="bg-negative text-white" dense>{{ error }}</q-banner>

          <div>
            <q-btn :label="$t('pages.login.submit')" type="submit" color="primary" class="full-width"
              :loading="loading" />
          </div>
          <div class="text-center">
            <router-link to="/register">{{ $t('pages.login.noAccount') }}</router-link>
          </div>
        </q-form>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from 'stores/auth-store'

const router = useRouter()
const route = useRoute()
const $q = useQuasar()
const { t } = useI18n()
const authStore = useAuthStore()

// Email or phone number: the server tells them apart (an email contains "@").
const identifier = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

onMounted(() => {
  if (route.query.expired === 'true') {
    error.value = t('pages.login.sessionExpired')
  }
})

const onSubmit = async () => {
  error.value = ''
  loading.value = true
  try {
    const result = await authStore.login(identifier.value, password.value)
    if (result.success) {
      $q.notify({ type: 'positive', message: t('pages.login.success') })
      router.push('/')
    } else {
      error.value = result.error || t('pages.login.failed')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-card {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}
</style>
