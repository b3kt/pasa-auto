<template>
  <q-page class="flex flex-center">
    <q-card class="login-card" style="min-width: 350px">
      <q-card-section>
<div class="text-h6 text-center q-mb-md">Login</div>
         <q-form @submit="onSubmit" class="q-gutter-md">
          <q-input v-model="username" label="Username" :rules="[val => !!val || 'Username harus diisi']" outlined dense
          hide-bottom-space>
            <template v-slot:prepend>
              <q-icon name="person" />
            </template>
          </q-input>

          <q-input v-model="password" label="Password" type="password" :rules="[val => !!val || 'Password harus diisi']"
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
            <q-btn label="Login" type="submit" color="primary" class="full-width" :loading="loading" />
          </div>
        </q-form>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'stores/auth-store'
import { useQuasar } from 'quasar'

const router = useRouter()
const authStore = useAuthStore()
const $q = useQuasar()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

// Clear all authentication data and cookies on mount
const clearAuthData = () => {
  // Clear localStorage
  localStorage.removeItem('auth_token')
  localStorage.removeItem('refresh_token')
  localStorage.removeItem('auth_user')

  // Clear all cookies
  document.cookie.split(';').forEach(cookie => {
    const eqPos = cookie.indexOf('=')
    const name = eqPos > -1 ? cookie.substr(0, eqPos).trim() : cookie.trim()
    if (name) {
      document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/;`
    }
  })

  // Reset auth store
  authStore.$reset()

  // Clear axios default headers
  delete authStore.api.defaults.headers.common['Authorization']
}

// Check for expired token on mount
onMounted(async () => {
  const urlParams = new URLSearchParams(window.location.search)
  const expired = urlParams.get('expired')
  const logoutMessage = urlParams.get('message')

  if (expired === 'true') {
    clearAuthData()

    if (logoutMessage) {
      error.value = decodeURIComponent(logoutMessage)
    } else {
      error.value = 'Session has expired. Please login again.'
    }

    $q.notify({
      type: 'warning',
      message: 'Session expired, please login again',
      position: 'top'
    })

    // Clean URL
    window.history.replaceState({}, document.title, window.location.pathname)
  }
})

const onSubmit = async () => {
  error.value = ''
  loading.value = true

  try {
    const result = await authStore.login(username.value, password.value)
    if (result) {
      if (result.success) {
        $q.notify({
          type: 'positive',
          message: 'Login successful!',
          position: 'top'
        })
        router.push('/')
      } else {
        error.value = result.error || 'Login failed'
      }
    }
  } catch (err) {
    error.value = 'An error occurred during login'
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
