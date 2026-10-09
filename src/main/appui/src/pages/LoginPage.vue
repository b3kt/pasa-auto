<template>
  <q-page class="flex flex-center">
    <q-card class="login-card" style="min-width: 350px">
      <q-card-section>
        <div class="text-h6 text-center q-mb-lg">Sign in</div>
        <q-form @submit="onSubmit" class="q-gutter-md">
          <q-input v-model="email" label="Email" type="email" :rules="[val => !!val || 'Email is required']"
            outlined dense hide-bottom-space>
            <template v-slot:prepend>
              <q-icon name="mail" />
            </template>
          </q-input>

          <q-input v-model="password" label="Password" type="password" :rules="[val => !!val || 'Password is required']"
            outlined dense hide-bottom-space>
            <template v-slot:prepend>
              <q-icon name="lock" />
            </template>
          </q-input>

          <q-banner v-if="error" class="bg-negative text-white" dense>{{ error }}</q-banner>

          <div>
            <q-btn label="Sign in" type="submit" color="primary" class="full-width" :loading="loading" />
          </div>
          <div class="text-center">
            <router-link to="/register">Don't have an account? Register</router-link>
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
import { useAuthStore } from 'stores/auth-store'

const router = useRouter()
const route = useRoute()
const $q = useQuasar()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

onMounted(() => {
  if (route.query.expired === 'true') {
    error.value = 'Your session has expired. Please sign in again.'
  }
})

const onSubmit = async () => {
  error.value = ''
  loading.value = true
  try {
    const result = await authStore.login(email.value, password.value)
    if (result.success) {
      $q.notify({ type: 'positive', message: 'Signed in successfully' })
      router.push('/')
    } else {
      error.value = result.error || 'Login failed'
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
