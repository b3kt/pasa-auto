<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated>
      <q-toolbar>
        <q-toolbar-title>
          {{ $t('app.constant.app_name') }}
        </q-toolbar-title>

        <div class="q-gutter-sm">
          <q-btn v-if="authStore.isLoggedIn" flat dense icon="logout" :label="$t('logout')" @click="handleLogout" />
          <span v-else>Quasar v{{ $q.version }}</span>
        </div>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'stores/auth-store'

const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()

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
