import { defineRouter } from '#q-app/wrappers'
import { createRouter, createMemoryHistory, createWebHistory, createWebHashHistory } from 'vue-router'
import routes from './routes'

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
  Router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('auth_token')
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth)

    if (requiresAuth && !token) {
      // Redirect to login if route requires auth and user is not authenticated
      next('/login')
    } else if (to.path === '/login' && token) {
      // Redirect to home if user is already logged in
      next('/')
    } else if (requiresAuth && token) {
      // Check role-based access
      const userJson = localStorage.getItem('auth_user')
      if (userJson) {
        try {
          const user = JSON.parse(userJson)
          const userRoles = user.roles || []
          
          // Check if route has role restrictions
          
          if (to.path.startsWith('/pazaauto/summary') && !userRoles.includes('owner')) {
            next('/')
            return
          }
        } catch (e) {
          console.error('Failed to parse user data', e)
        }
      }
      next()
    } else {
      next()
    }
  })

  return Router
})
