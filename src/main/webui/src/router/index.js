import { defineRouter } from '#q-app/wrappers'
import { createRouter, createMemoryHistory, createWebHistory, createWebHashHistory } from 'vue-router'
import routes from './routes'
import { useAuthStore } from 'stores/auth-store'

/*
 * If not building with SSR mode, you can
 * directly export the Router instantiation;
 *
 * The function below can be async too; either use
 * async/await or return a Promise which resolves
 * with the Router instance.
 */


export default defineRouter(function (/* { store, ssrContext } */) {
  const createHistory = process.env.SERVER
    ? createMemoryHistory
    : (process.env.VUE_ROUTER_MODE === 'history' ? createWebHistory : createWebHashHistory)

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,

    // Leave this as is and make changes in quasar.conf.js instead!
    // quasar.conf.js -> build -> vueRouterMode
    // quasar.conf.js -> build -> publicPath
    history: createHistory(process.env.VUE_ROUTER_BASE)
  })

  // Route guard for authentication
  Router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore()
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth)

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
      // Redirect to login if route requires auth and user is not authenticated
      next('/login')
    } else if (to.path === '/login' && token) {
      // Redirect to home if user is already logged in
      next('/')
    } else if (requiresAuth && token && authStore.user?.mustChangePassword && to.path !== '/change-password') {
      // A temporary password must be replaced before anything else (the server enforces this too)
      next('/change-password')
    } else if (requiresAuth && token) {
      // Role-based access: routes declare meta.roles (see routes.js); the backend enforces the same rules
      const allowedRoles = to.meta.roles
      const userRoles = authStore.user?.roles || []
      if (allowedRoles && !allowedRoles.some(role => userRoles.includes(role))) {
        next(from.matched.length ? false : '/')
        return
      }
      next()
    } else {
      next()
    }
  })

  return Router
})
