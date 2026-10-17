<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from './stores/auth-store'

// Initialize auth store when app mounts
onMounted(async () => {
  try {
    const authStore = useAuthStore()
    await authStore.initializeAuth()
    console.debug('Auth store initialized successfully')
  } catch (error) {
    console.error('Failed to initialize auth store:', error)
  }
})
</script>
