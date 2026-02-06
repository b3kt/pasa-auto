import { defineBoot } from '#q-app/wrappers'
import { useAuthStore } from 'stores/auth-store'

export default defineBoot(({ app, router }) => {
  // Initialize auth store on app startup
  console.debug('app', app)
  console.debug('router', router)
  
  const authStore = useAuthStore()
  authStore.initializeAuth()
})
