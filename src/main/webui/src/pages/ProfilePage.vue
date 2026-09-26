<template>
  <q-page class="flex flex-center" padding>
    <q-card style="width: 450px; max-width: 100%">
      <q-card-section>
        <div class="text-h6">{{ $t('pages.profile.title') }}</div>
      </q-card-section>

      <q-card-section>
        <q-form @submit="onSubmit" class="q-gutter-md">
          <q-input :model-value="user?.username" :label="$t('username')" outlined dense readonly />

          <q-input :model-value="user?.roles?.join(', ')" :label="$t('pages.profile.rolesLabel')" outlined dense readonly />

          <q-input :model-value="user?.karyawanNama || $t('pages.profile.noEmployeeLinked')"
            :label="$t('pages.profile.employeeLabel')" outlined dense readonly />

          <q-input v-model="email" :label="$t('email')" type="email" outlined dense
            :rules="[
              val => !!val || $t('pages.profile.emailRequired'),
              val => /.+@.+\..+/.test(val) || $t('pages.profile.emailInvalid')
            ]" />

          <q-banner v-if="error" class="bg-negative text-white" dense>{{ error }}</q-banner>

          <div class="row justify-end q-gutter-sm">
            <q-btn flat :label="$t('changePassword')" color="primary" @click="router.push('/change-password')" />
            <q-btn type="submit" :label="$t('save')" color="primary" :loading="saving" :disable="!isDirty" />
          </div>
        </q-form>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from 'stores/auth-store'

const router = useRouter()
const $q = useQuasar()
const { t } = useI18n()
const authStore = useAuthStore()

const user = computed(() => authStore.user)
const email = ref(user.value?.email || '')
const saving = ref(false)
const error = ref('')

const isDirty = computed(() => email.value !== (user.value?.email || ''))

// The store's user object can change after a background token refresh; keep the field in sync
// as long as the person hasn't started editing it themselves.
watch(() => user.value?.email, (newEmail) => {
  if (!isDirty.value) {
    email.value = newEmail || ''
  }
})

const onSubmit = async () => {
  error.value = ''
  saving.value = true
  try {
    const result = await authStore.updateProfile(email.value)
    if (result.success) {
      $q.notify({ type: 'positive', message: t('pages.profile.updateSuccess') })
    } else {
      error.value = result.error || t('pages.profile.updateFailed')
    }
  } finally {
    saving.value = false
  }
}
</script>
