<template>
  <q-page class="flex flex-center" padding>
    <q-card style="width: 400px; max-width: 100%">
      <q-card-section>
        <div class="text-h6">{{ $t('changePassword') }}</div>
        <div v-if="mustChange" class="text-caption text-grey-8 q-mt-xs">
          {{ $t('pages.changePassword.temporaryNotice') }}
        </div>
      </q-card-section>

      <q-card-section>
        <q-form @submit="onSubmit" class="q-gutter-md">
          <q-input v-model="currentPassword" :label="mustChange ? $t('pages.changePassword.temporaryPasswordLabel') : $t('pages.changePassword.currentPasswordLabel')"
            type="password" autocomplete="current-password" outlined dense
            :rules="[val => !!val || $t('pages.changePassword.currentPasswordRequired')]" />

          <q-input v-model="newPassword" :label="$t('pages.changePassword.newPasswordLabel')" :type="showNew ? 'text' : 'password'"
            autocomplete="new-password" outlined dense
            :hint="$t('pages.changePassword.newPasswordHint', { min: MIN_LENGTH })"
            :rules="[
              val => !!val || $t('pages.changePassword.newPasswordRequired'),
              val => val.length >= MIN_LENGTH || $t('pages.changePassword.minLengthMessage', { min: MIN_LENGTH }),
              val => val !== currentPassword || $t('pages.changePassword.mustDifferMessage')
            ]">
            <template v-slot:append>
              <q-icon :name="showNew ? 'visibility_off' : 'visibility'" class="cursor-pointer"
                @click="showNew = !showNew" />
            </template>
          </q-input>

          <q-input v-model="confirmPassword" :label="$t('pages.changePassword.confirmPasswordLabel')" :type="showNew ? 'text' : 'password'"
            autocomplete="new-password" outlined dense
            :rules="[val => val === newPassword || $t('pages.changePassword.confirmMismatchMessage')]" />

          <q-banner v-if="error" class="bg-negative text-white" dense>{{ error }}</q-banner>

          <div class="row justify-end q-gutter-sm">
            <q-btn v-if="mustChange" flat :label="$t('logout')" color="primary" @click="logout" />
            <q-btn v-else flat :label="$t('cancel')" color="primary" @click="router.back()" />
            <q-btn type="submit" :label="$t('pages.changePassword.submitButton')" color="primary" :loading="saving" />
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
import { useI18n } from 'vue-i18n'
import { useAuthStore } from 'stores/auth-store'

// Mirrors PasswordPolicy.MIN_LENGTH on the server, which is the source of truth
const MIN_LENGTH = 8

const router = useRouter()
const $q = useQuasar()
const { t } = useI18n()
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
      $q.notify({ type: 'positive', message: t('pages.changePassword.successMessage') })
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
