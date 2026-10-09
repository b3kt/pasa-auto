import { defineBoot } from '#q-app/wrappers'
import { createI18n } from 'vue-i18n'
import messages from 'src/i18n'

const supportedLocales = Object.keys(messages)

export default defineBoot(({ app }) => {
  const savedLocale = typeof localStorage !== 'undefined'
    ? localStorage.getItem('locale')
    : null
  const locale = supportedLocales.includes(savedLocale) ? savedLocale : 'id-ID'

  const i18n = createI18n({
    locale,
    fallbackLocale: 'en-US',
    globalInjection: true,
    messages
  })

  // Set i18n instance on app
  app.use(i18n)
})
