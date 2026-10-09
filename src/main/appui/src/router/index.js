import { defineRouter } from '#q-app/wrappers'
import { createRouter, createMemoryHistory, createWebHashHistory } from 'vue-router'
import routes from './routes'
import { useAuthStore } from 'stores/auth-store'

export default defineRouter(function (/* { store, ssrContext } */) {
  const createHistory = process.env.SERVER
    ? createMemoryHistory
    : createWebHashHistory

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,

    // Leave this as is and make changes in quasar.config.js instead!
    // quasar.config.js -> build -> vueRouterMode / vueRouterBase
    history: createHistory(process.env.VUE_ROUTER_BASE)
  })

  // The Owner can switch the portal off without a redeploy (see the staff app's
  // admin/customer-access page), so the feature flag is checked before anything else: a customer
  // already using the portal must see it become unavailable. Then the customer's own session is
  // applied.
  Router.beforeEach(async (to, from, next) => {
    if (to.path !== '/unavailable' && !(await portalEnabled())) {
      next({ path: '/unavailable' })
      return
    }

    const authStore = useAuthStore()
    const requiresAuth = to.meta.customerAuth !== false

    // Renew an expired access token; if that fails the session is over
    let token = authStore.token
    if (token && (requiresAuth || to.path === '/login') && !(await authStore.ensureValidToken())) {
      authStore.clearSession()
      token = null
      if (requiresAuth) {
        next({ path: '/login', query: { expired: 'true' } })
        return
      }
    }

    if (requiresAuth && !token) {
      next('/login')
    } else if (to.path === '/login' && token) {
      next('/')
    } else {
      next()
    }
  })

  return Router
})

// Fail closed: if the backend cannot be reached to confirm the flag, treat the portal as switched
// off rather than show a broken app. The 503 interceptor in boot/axios.js covers the same case
// while the customer is already on a page.
async function portalEnabled() {
  try {
    const { api } = await import('boot/axios')
    const response = await api.get('/api/public/customer-access-status')
    return response.data?.data?.customerAccessEnabled === true
  } catch {
    return false
  }
}
