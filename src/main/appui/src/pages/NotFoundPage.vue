<template>
  <q-page class="flex flex-center">
    <q-card style="min-width: 350px" flat bordered>
      <q-card-section class="text-center">
        <q-icon name="help_outline" size="48px" color="grey-6" />
        <div class="text-h6 q-mt-md">Page not found</div>
        <div class="text-body2 text-grey-7 q-mt-sm">
          The page you are looking for does not exist.
        </div>
        <q-btn class="q-mt-md" color="primary" label="Back to dashboard" :to="authStore.isLoggedIn ? '/' : '/login'" />
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { useAuthStore } from 'stores/auth-store'

const authStore = useAuthStore()
</script>
