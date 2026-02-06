<template>
  <q-page class="flex flex-center">
    <q-card class="login-card" style="min-width: 350px">
      <q-card-section>
        <div class="text-h6 text-center q-mb-md">Login</div>
        <q-form @submit="onSubmit" class="q-gutter-md">
          <q-input v-model="username" label="Username" :rules="[val => !!val || 'Username harus diisi']" outlined dense>
            <template v-slot:prepend>
              <q-icon name="person" />
            </template>
          </q-input>

          <q-input v-model="password" label="Password" type="password" :rules="[val => !!val || 'Password harus diisi']"
            outlined dense>
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
import { ref } from 'vue'
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
