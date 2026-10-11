import { defineBoot } from '#q-app/wrappers'
import { useAuthStore } from 'stores/auth-store'

export default defineBoot(async () => {
  // Restore the customer session before the first navigation by exchanging the HttpOnly refresh
  // cookie for an access token; the token itself is never persisted.
  const authStore = useAuthStore()
  await authStore.initializeAuth()
})
