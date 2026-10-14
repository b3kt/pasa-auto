import { defineBoot } from '#q-app/wrappers'
import { createI18n } from 'vue-i18n'
import { Lang } from 'quasar'
import langEnUS from 'quasar/lang/en-US'
import langId from 'quasar/lang/id'
import messages from 'src/i18n'

export const SUPPORTED_LOCALES = Object.keys(messages)
export const DEFAULT_LOCALE = 'id-ID'

// Keep Quasar's own component chrome (date picker, pagination, ...) in step with the app locale.
const quasarLangByLocale = {
  'id-ID': langId,
  'en-US': langEnUS
}

// Switch the app + Quasar language and remember the choice across reloads.
export function applyLocale(locale) {
  Lang.set(quasarLangByLocale[locale] || quasarLangByLocale[DEFAULT_LOCALE])
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('locale', locale)
  }
}

export default defineBoot(({ app }) => {
  const savedLocale =
    typeof localStorage !== 'undefined' ? localStorage.getItem('locale') : null
  const locale = SUPPORTED_LOCALES.includes(savedLocale) ? savedLocale : DEFAULT_LOCALE

  const i18n = createI18n({
    legacy: false,
    locale,
    fallbackLocale: 'en-US',
    globalInjection: true,
    messages
  })

  // quasar.config.js loads the Indonesian pack by default; honour a saved non-default locale.
  Lang.set(quasarLangByLocale[locale] || quasarLangByLocale[DEFAULT_LOCALE])

  // Set i18n instance on app
  app.use(i18n)
})
