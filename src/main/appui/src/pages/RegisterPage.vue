<template>
  <q-page class="flex flex-center">
    <q-card class="register-card" style="min-width: 350px">
      <q-card-section>
        <div class="text-h6 text-center q-mb-lg">Create your account</div>
        <q-form @submit="onSubmit" class="q-gutter-md">
          <q-input v-model="fullName" label="Full name" :rules="[val => !!val || 'Full name is required']"
            outlined dense hide-bottom-space />

          <q-input v-model="email" label="Email" type="email" :rules="[val => !!val || 'Email is required']"
            outlined dense hide-bottom-space />

          <q-input v-model="phone" label="Phone (optional)" outlined dense hide-bottom-space />

          <q-input v-model="password" label="Password" type="password"
            :rules="[val => !!val && val.length >= 8 || 'At least 8 characters']"
            outlined dense hide-bottom-space />

          <q-banner v-if="error" class="bg-negative text-white" dense>{{ error }}</q-banner>

          <div>
            <q-btn label="Register" type="submit" color="primary" class="full-width" :loading="loading" />
          </div>
          <div class="text-center">
            <router-link to="/login">Already have an account? Sign in</router-link>
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
import { useAuthStore } from 'stores/auth-store'

const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()

const fullName = ref('')
const email = ref('')
const phone = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

const onSubmit = async () => {
  error.value = ''
  loading.value = true
  try {
    const result = await authStore.register({
      fullName: fullName.value,
      email: email.value,
      phone: phone.value || null,
      password: password.value
    })
    if (result.success) {
      $q.notify({ type: 'positive', message: 'Account created' })
      router.push('/link-account')
    } else {
      error.value = result.error || 'Registration failed'
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
