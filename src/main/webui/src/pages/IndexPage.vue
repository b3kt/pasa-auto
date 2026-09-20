<template>
  <q-page class="flex flex-center column">
    <!--
      A freshly approved Google account has no roles yet, so every menu entry is hidden and every
      endpoint answers 403. Say so, rather than showing an empty shell that looks broken.
    -->
    <q-banner v-if="awaitingRoles" class="bg-warning text-dark q-mb-md" style="max-width: 640px">
      <template v-slot:avatar>
        <q-icon name="hourglass_top" />
      </template>
      Akun Anda sudah aktif, tetapi administrator belum menetapkan hak akses. Hubungi administrator
      agar menu dan data bisa diakses.
    </q-banner>

    <img alt="Pasa Auto" src="~assets/pasa.svg" style="width: 100%; height: auto; max-height: 640px; ">
  </q-page>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from 'stores/auth-store'

const authStore = useAuthStore()

const awaitingRoles = computed(() => {
  if (!authStore.isLoggedIn) return false
  return (authStore.user?.roles || []).length === 0
})
</script>
