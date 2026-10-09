import { defineBoot } from '#q-app/wrappers'
import { createPinia } from 'pinia'

export default defineBoot(({ app }) => {
  // Use Quasar's store system - this will automatically initialize Pinia
  // The stores/index.js file handles the Pinia setup
  console.debug('Stores boot file initialized')
  const pinia = createPinia()
  app.use(pinia)
})
