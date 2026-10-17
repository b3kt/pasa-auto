import { defineBoot } from '#q-app/wrappers'
import { useAuthStore } from 'stores/auth-store'

export default defineBoot(async ({ router }) => {
  // Restore the customer session before the first navigation by exchanging the HttpOnly refresh
  // cookie for an access token; the token itself is never persisted.
  const authStore = useAuthStore()
  await authStore.initializeAuth()

  window.addEventListener('offline', () => authStore.setOnline(false))

  // Back online: an offline session (cached pages only) has to be renewed into a real one. If the
  // refresh cookie is gone or rejected the session is over, exactly as on startup.
  window.addEventListener('online', async () => {
    authStore.setOnline(true)
    if (authStore.token || !authStore.user) return

    await authStore.resumeSession()
    if (!authStore.user && router.currentRoute.value.meta.customerAuth !== false) {
      router.replace({ path: '/login', query: { expired: 'true' } })
    }
  })
})
