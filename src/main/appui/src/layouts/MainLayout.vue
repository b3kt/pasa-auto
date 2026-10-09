<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated class="bg-secondary">
      <q-toolbar>
        <q-toolbar-title class="text-uppercase text-bold" shrink>
          Pasa Auto | Customer
        </q-toolbar-title>
        <q-space />
        <div v-if="authStore.isLoggedIn" class="q-gutter-sm row items-center no-wrap">
          <q-btn flat dense to="/" label="Dashboard" />
          <q-btn flat dense to="/profile" label="Profile" />
          <q-btn flat dense to="/vehicles" label="Vehicles" />
          <q-btn flat dense to="/bookings" label="Bookings" />
          <q-btn flat dense to="/spk" label="Service Status" />
          <q-btn flat dense icon="logout" label="Logout" @click="handleLogout" />
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
  $q.notify({ type: 'info', message: 'Logged out successfully' })
  router.push('/login')
}
</script>
