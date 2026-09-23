<template>
  <q-page class="flex flex-center">
    <q-card class="login-card" style="min-width: 350px">
      <q-card-section>
        <div class="text-h6 text-center q-mb-lg">{{ $t('pages.login.title') }}</div>
         <q-form @submit="onSubmit" class="q-gutter-md">
          <q-input v-model="username" :label="$t('pages.login.usernameLabel')" :rules="[val => !!val || $t('pages.login.usernameRequired')]" outlined dense
          hide-bottom-space>
            <template v-slot:prepend>
              <q-icon name="person" />
            </template>
          </q-input>

          <q-input v-model="password" :label="$t('pages.login.passwordLabel')" type="password" :rules="[val => !!val || $t('pages.login.passwordRequired')]"
            outlined dense
           hide-bottom-space>
            <template v-slot:prepend>
              <q-icon name="lock" />
            </template>
          </q-input>

          <q-banner v-if="error" class="bg-negative text-white q-mt-md" dense>
            {{ error }}
          </q-banner>

          <div>
            <q-btn :label="$t('pages.login.title')" type="submit" color="primary" class="full-width" :loading="loading" />
          </div>
        </q-form>

        <template v-if="googleEnabled">
          <div class="row items-center q-my-md">
            <q-separator class="col" />
            <div class="col-auto q-px-sm text-caption text-grey-7">{{ $t('pages.login.or') }}</div>
            <q-separator class="col" />
          </div>

          <q-btn outline color="primary" class="full-width" icon="login" :label="$t('pages.login.googleButton')"
                 :loading="googleRedirecting" @click="signInWithGoogle" />
        </template>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from 'stores/auth-store'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { api } from 'boot/axios'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const $q = useQuasar()
const { t } = useI18n()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const googleEnabled = ref(false)
const googleRedirecting = ref(false)

// Outcomes the Google callback redirects back with
const GOOGLE_MESSAGES = {
  pending: () => t('pages.login.google.pending'),
  rejected: () => t('pages.login.google.rejected'),
  disabled: () => t('pages.login.google.disabled'),
  link_required: () => t('pages.login.google.linkRequired'),
  denied: () => t('pages.login.google.denied'),
  error: () => t('pages.login.google.error')
}

// Clear all authentication data and cookies on mount
const clearAuthData = () => {
  // Clear tokens from the store and localStorage
  authStore.clearSession()

  // Clear all cookies
  document.cookie.split(';').forEach(cookie => {
    const eqPos = cookie.indexOf('=')
    const name = eqPos > -1 ? cookie.substr(0, eqPos).trim() : cookie.trim()
    if (name) {
      document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/;`
    }
  })
}

// Check for expired token on mount
onMounted(async () => {
  // Hash routing: the query lives in the route, not in window.location.search
  const { expired, message: logoutMessage } = route.query

  if (route.query.google) {
    await handleGoogleReturn(route.query.google)
    return
  }

  if (expired === 'true') {
    clearAuthData()

    if (logoutMessage) {
      error.value = decodeURIComponent(logoutMessage)
    } else {
      error.value = t('pages.login.sessionExpiredMessage')
    }

    $q.notify({
      type: 'warning',
      message: t('pages.login.sessionExpiredNotify'),
    })

    // Clean URL
    router.replace({ path: '/login' })
  }

  await loadGoogleConfig()
})

const loadGoogleConfig = async () => {
  try {
    const response = await api.get('/api/auth/config')
    googleEnabled.value = response.data?.data?.googleLoginEnabled === true
  } catch (err) {
    // The button simply stays hidden if the backend can't be asked
    console.warn('Failed to read auth config:', err)
  }
}

const signInWithGoogle = () => {
  googleRedirecting.value = true
  // A full-page navigation, not an XHR: the browser has to follow the redirect to Google. It also
  // has to be a navigation rather than a form post, since the CSP sets form-action 'self'.
  window.location.href = `${api.defaults.baseURL.replace(/\/$/, '')}/api/auth/google/start`
}

const handleGoogleReturn = async (outcome) => {
  router.replace({ path: '/login' })

  if (outcome !== 'ok') {
    error.value = (GOOGLE_MESSAGES[outcome] || GOOGLE_MESSAGES.error)()
    await loadGoogleConfig()
    return
  }

  loading.value = true
  try {
    // Wipe the previous user's session and caches first: the refresh cookie now belongs to the
    // Google user, and clearSession() is what clears the cached data of whoever was here before.
    await authStore.clearSession()
    if (await authStore.refreshAccessToken()) {
      $q.notify({ type: 'positive', message: t('pages.login.loginSuccess') })
      router.push('/')
    } else {
      error.value = GOOGLE_MESSAGES.error()
      await loadGoogleConfig()
    }
  } finally {
    loading.value = false
  }
}

const onSubmit = async () => {
  error.value = ''
  loading.value = true

  try {
    const result = await authStore.login(username.value, password.value)
    if (result) {
      if (result.success) {
        $q.notify({
          type: 'positive',
          message: t('pages.login.loginSuccess'),
        })
        router.push('/')
      } else {
        error.value = result.error || t('pages.login.loginFailed')
      }
    }
  } catch (err) {
    error.value = t('pages.login.loginError')
    console.error(err)
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
