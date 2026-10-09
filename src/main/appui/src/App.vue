<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from './stores/auth-store'

// Renew or drop a stored session before the first navigation, so the router guard starts from a
// known state instead of a stale token in localStorage
onMounted(async () => {
  try {
    const authStore = useAuthStore()
    await authStore.initializeAuth()
  } catch (error) {
    console.error('Failed to initialize customer auth store:', error)
  }
})
</script>
