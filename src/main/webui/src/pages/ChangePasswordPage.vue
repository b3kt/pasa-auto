<template>
  <q-page class="flex flex-center" padding>
    <q-card style="width: 400px; max-width: 100%">
      <q-card-section>
        <div class="text-h6">Change Password</div>
        <div v-if="mustChange" class="text-caption text-grey-8 q-mt-xs">
          You are signed in with a temporary password. Choose a new password to continue.
        </div>
      </q-card-section>

      <q-card-section>
        <q-form @submit="onSubmit" class="q-gutter-md">
          <q-input v-model="currentPassword" :label="mustChange ? 'Temporary password' : 'Current password'"
            type="password" autocomplete="current-password" outlined dense
            :rules="[val => !!val || 'Password lama harus diisi']" />

          <q-input v-model="newPassword" label="New password" :type="showNew ? 'text' : 'password'"
            autocomplete="new-password" outlined dense
            :hint="`At least ${MIN_LENGTH} characters`"
            :rules="[
              val => !!val || 'Password baru harus diisi',
              val => val.length >= MIN_LENGTH || `Minimal ${MIN_LENGTH} karakter`,
              val => val !== currentPassword || 'Password baru harus berbeda'
            ]">
            <template v-slot:append>
              <q-icon :name="showNew ? 'visibility_off' : 'visibility'" class="cursor-pointer"
                @click="showNew = !showNew" />
            </template>
          </q-input>

          <q-input v-model="confirmPassword" label="Confirm new password" :type="showNew ? 'text' : 'password'"
            autocomplete="new-password" outlined dense
            :rules="[val => val === newPassword || 'Konfirmasi password tidak sama']" />

          <q-banner v-if="error" class="bg-negative text-white" dense>{{ error }}</q-banner>

          <div class="row justify-end q-gutter-sm">
            <q-btn v-if="mustChange" flat label="Logout" color="primary" @click="logout" />
            <q-btn v-else flat label="Cancel" color="primary" @click="router.back()" />
            <q-btn type="submit" label="Change password" color="primary" :loading="saving" />
          </div>
        </q-form>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'stores/auth-store'

// Mirrors PasswordPolicy.MIN_LENGTH on the server, which is the source of truth
const MIN_LENGTH = 8

const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()

const mustChange = computed(() => authStore.user?.mustChangePassword === true)
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showNew = ref(false)
const saving = ref(false)
const error = ref('')

const onSubmit = async () => {
  error.value = ''
  saving.value = true
  try {
    const result = await authStore.changePassword(currentPassword.value, newPassword.value)
    if (result.success) {
      $q.notify({ type: 'positive', message: 'Password changed' })
      router.replace('/')
    } else {
      error.value = result.error
    }
  } finally {
    saving.value = false
  }
}

const logout = async () => {
  await authStore.logout()
  router.replace('/login')
}
</script>
