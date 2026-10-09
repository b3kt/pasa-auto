// Configuration for the customer portal app (PASA AUTO customer self-service).
// It is a standalone Quasar project: it shares no source with the staff app in ../webui and talks
// to the same backend through the /api/customer/* endpoints.
// Deployed by the ../webui build into the admin bundle under /app/, hence publicPath/vueRouterBase.
// https://v2.quasar.dev/quasar-cli-vite/quasar-config-file

import { defineConfig } from '#q-app/wrappers'
import { fileURLToPath } from 'node:url'

export default defineConfig((ctx) => {
  return {
    // https://v2.quasar.dev/quasar-cli-vite/boot-files
    boot: [
      'stores',
      'i18n',
      'axios'
    ],

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#css
    css: [
      'app.scss'
    ],

    // https://github.com/quasarframework/quasar/tree/dev/extras
    extras: [
      'roboto-font', // optional, you are not bound to it
      'material-icons', // optional, you are not bound to it
      'mdi-v7'
    ],

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#build
    build: {
      target: {
        browser: ['es2022', 'firefox115', 'chrome115', 'safari14'],
        node: 'node20'
      },

      // Hash mode keeps every customer URL under the /app/ prefix the bundle is served from, so a
      // deep link (or a page refresh) never needs a server-side fallback to app/index.html.
      vueRouterMode: 'hash', // available values: 'hash', 'history'
      vueRouterBase: '/app/',

      // The bundle is copied into the admin SPA's dist as the app/ subdirectory, so every asset
      // URL has to be absolute and prefixed with /app/
      publicPath: '/app/',

      gzip: true,

      vitePlugins: [
        ['@intlify/unplugin-vue-i18n/vite', {
          ssr: ctx.modeName === 'ssr',

          // you need to set i18n resource including paths !
          include: [fileURLToPath(new URL('./src/i18n', import.meta.url))]
        }],

        // Only enable ESLint checker in dev mode, not during build
        ...(ctx.dev ? [
          ['vite-plugin-checker', {
            eslint: {
              lintCommand: 'eslint -c ./eslint.config.js "./src*/**/*.{js,mjs,cjs,vue}"',
              useFlatConfig: true
            }
          }, { server: false }]
        ] : [])
      ]
    },

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#devserver
    devServer: {
      // 5173 belongs to the staff app's dev server (and to the Quinoa dev proxy)
      port: 5174,
      open: false // opens browser window automatically
    },

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#framework
    framework: {
      config: {
        notify: {
          position: 'bottom-right',
          timeout: 5000
        }
      },

      // iconSet: 'material-icons', // Quasar icon set
      lang: 'id', // Quasar language pack (component chrome: pagination, date picker, etc.)

      // Quasar plugins
      plugins: ['Notify', 'Dialog']
    },

    animations: [],

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#sourcefiles
    sourceFiles: {
      rootComponent: 'src/App.vue',
      router: 'src/router/index',
      store: 'src/store/index'
    },

    // https://v2.quasar.dev/quasar-cli-vite/developing-ssr/configuring-ssr
    ssr: {
      prodPort: 3000, // The default port that the production server should use
      // (gets superseded if process.env.PORT is specified at runtime)

      middlewares: [
        'render' // keep this as last one
      ],

      pwa: false
    }
  }
})
