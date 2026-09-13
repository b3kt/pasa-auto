<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated>
      <q-toolbar>
        <q-toolbar-title>
          {{ $t('app.constant.app_name') }}
        </q-toolbar-title>

        <div class="q-gutter-sm row items-center no-wrap">
          <span v-if="appVersion" class="text-caption q-mr-sm">v{{ appVersion }}</span>
          <span v-else class="text-caption q-mr-sm">v{{ $q.version }}</span>
          <q-btn v-if="authStore.isLoggedIn" flat dense icon="logout" :label="$t('logout')" @click="handleLogout" />
        </div>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'stores/auth-store'
import { api } from 'boot/axios'

const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()
const appVersion = ref('')

onMounted(async () => {
  try {
    const response = await api.get('/health')
    if (response.data && response.data.data) {
      appVersion.value = response.data.data.version || ''
    }
  } catch (e) {
    console.error('Failed to fetch version:', e)
  }
})

async function handleLogout() {
  await authStore.logout()
  $q.notify({
    type: 'info',
    message: 'Logged out successfully',
    position: 'top'
  })
  router.push('/login')
}
</script>
