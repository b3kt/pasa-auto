import { defineBoot } from '#q-app/wrappers'
import { useAuthStore } from 'stores/auth-store'

export default defineBoot(async ({ app, router }) => {
  // Initialize auth store on app startup (renews or drops an expired stored session before the first navigation)
  console.debug('app', app)
  console.debug('router', router)

  const authStore = useAuthStore()
  await authStore.initializeAuth()
})
