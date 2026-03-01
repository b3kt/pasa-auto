<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from './stores/auth-store'
import jsPreloader from './utils/jsPreloader.js'

// Initialize auth store when app mounts
onMounted(async () => {
  try {
    const authStore = useAuthStore()
    await authStore.initializeAuth()
    console.debug('Auth store initialized successfully')
  } catch (error) {
    console.error('Failed to initialize auth store:', error)
  }

  // Preload critical JavaScript files
  try {
    await jsPreloader.preloadCriticalJs()
    console.debug('JavaScript preloading completed')
  } catch (error) {
    console.error('JavaScript preloading failed:', error)
  }

  // Warm up JavaScript cache
  try {
    await jsPreloader.warmUpJsCache()
    console.debug('JavaScript cache warm-up completed')
  } catch (error) {
    console.error('JavaScript cache warm-up failed:', error)
  }
})
</script>
