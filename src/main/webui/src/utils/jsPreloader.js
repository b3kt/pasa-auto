import browserCache from './browserCache.js'

class JsPreloader {
  constructor() {
    this.criticalJsFiles = [
      // Vite client and development files
      '/@vite/client',
      '/quasar/dev-spa/client-entry.js',
      '/@vite-plugin-checker-runtime',
      '/node_modules/vite/dist/client/env.mjs',
      '/@id/_x00_plugin-vue:export-helper',

      // Core Vue and Quasar dependencies
      '/node_modules/.q-cache/dev-spa/vite-spa/deps/vue.js',
      '/node_modules/.q-cache/dev-spa/vite-spa/deps/vue-router.js',
      '/node_modules/.q-cache/dev-spa/vite-spa/deps/pinia.js',
      '/node_modules/.q-cache/dev-spa/vite-spa/deps/jwt-decode.js',

      // Quasar framework files
      '/node_modules/.q-cache/dev-spa/vite-spa/deps/quasar_lang_en-US_js.js',
      '/node_modules/.q-cache/dev-spa/vite-spa/deps/quasar_dist_quasar_client_js.js',
      '/node_modules/.q-cache/dev-spa/vite-spa/deps/@quasar_app-vite_wrappers.js',
      '/node_modules/quasar/dist/quasar.sass',

      // Quasar extras
      '/node_modules/@quasar/extras/roboto-font/roboto-font.css',
      '/node_modules/@quasar/extras/material-icons/material-icons.css',
      '/node_modules/@quasar/extras/mdi-v7/mdi-v7.css',

      // App-specific files
      '/.quasar/dev-spa/app.js',
      '/.quasar/dev-spa/quasar-user-options.js',
      '/src/App.vue',
      '/src/css/app.scss',

      // Router files
      '/src/router/index.js',
      '/src/router/routes.js',

      // Store files
      '/src/stores/auth-store.js',

      // Boot files
      '/src/boot/stores.js',
      '/src/boot/i18n.js',
      '/src/boot/axios.js',
      '/src/boot/auth.js',
      '/src/boot/offline.js',

      // Utility files
      '/src/utils/jsPreloader.js',
      '/src/utils/browserCache.js',

      // Vite chunks (common patterns)
      '/node_modules/.q-cache/dev-spa/vite-spa/deps/chunk-WFPNB3EW.js',
      '/node_modules/.q-cache/dev-spa/vite-spa/deps/chunk-PZ5AY32C.js'
    ]

    this.preloadPromises = new Map()
  }

  // Preload critical JavaScript files
  async preloadCriticalJs() {
    console.log('Starting JavaScript preloading...')

    const preloadPromises = this.criticalJsFiles.map(url => this.preloadJsFile(url))

    try {
      const results = await Promise.allSettled(preloadPromises)
      const successful = results.filter(r => r.status === 'fulfilled').length
      const failed = results.filter(r => r.status === 'rejected').length

      console.log(`JavaScript preloading completed: ${successful} successful, ${failed} failed`)
      return { successful, failed }
    } catch (error) {
      console.error('JavaScript preloading failed:', error)
      return { successful: 0, failed: this.criticalJsFiles.length }
    }
  }

  // Preload individual JavaScript file
  async preloadJsFile(url) {
    try {
      // Check if already cached
      const cached = browserCache.getCachedJsFile(url)
      if (cached) {
        console.log(`JS file already cached: ${url}`)
        return { url, status: 'cached' }
      }

      // Fetch the file
      const response = await fetch(url, {
        cache: 'force-cache',
        headers: {
          'Accept': 'application/javascript, text/javascript, */*'
        }
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const content = await response.text()

      // Cache the content
      await browserCache.cacheJsFile(url, content, 7 * 24 * 60 * 60 * 1000) // 7 days

      console.log(`JS file cached: ${url}`)
      return { url, status: 'cached' }

    } catch (error) {
      console.warn(`Failed to preload JS file ${url}:`, error)
      return { url, status: 'failed', error: error.message }
    }
  }

  // Preload JavaScript file on demand
  async preloadJsOnDemand(url) {
    if (this.preloadPromises.has(url)) {
      return this.preloadPromises.get(url)
    }

    const promise = this.preloadJsFile(url)
    this.preloadPromises.set(url, promise)

    try {
      const result = await promise
      return result
    } finally {
      this.preloadPromises.delete(url)
    }
  }

  // Get cached JavaScript file
  getCachedJs(url) {
    return browserCache.getCachedJsFile(url)
  }

  // Check if JavaScript file is cached
  isJsCached(url) {
    return this.getCachedJs(url) !== null
  }

  // Clear JavaScript cache
  clearJsCache() {
    try {
      const keys = Object.keys(localStorage)
      let clearedCount = 0

      keys.forEach(key => {
        if (key.startsWith('pasa-auto-js-')) {
          localStorage.removeItem(key)
          clearedCount++
        }
      })

      console.log(`Cleared ${clearedCount} JavaScript cache items`)
      return clearedCount
    } catch (error) {
      console.error('Failed to clear JavaScript cache:', error)
      return 0
    }
  }

  // Get JavaScript cache statistics
  getJsCacheStats() {
    try {
      const keys = Object.keys(localStorage)
      let jsItems = 0
      let totalSize = 0

      keys.forEach(key => {
        if (key.startsWith('pasa-auto-js-')) {
          jsItems++
          const item = localStorage.getItem(key)
          totalSize += item.length
        }
      })

      return {
        jsItems,
        totalSize: (totalSize / 1024).toFixed(2) + ' KB',
        averageSize: jsItems > 0 ? (totalSize / jsItems / 1024).toFixed(2) + ' KB' : '0 KB'
      }
    } catch (error) {
      console.error('Failed to get JavaScript cache stats:', error)
      return null
    }
  }

  // Preload JavaScript files based on current route
  async preloadRouteJs(routePath) {
    const routeJsFiles = this.getRouteJsFiles(routePath)

    if (routeJsFiles.length === 0) {
      return { successful: 0, failed: 0 }
    }

    console.log(`Preloading JavaScript for route: ${routePath}`)

    const preloadPromises = routeJsFiles.map(url => this.preloadJsFile(url))

    try {
      const results = await Promise.allSettled(preloadPromises)
      const successful = results.filter(r => r.status === 'fulfilled').length
      const failed = results.filter(r => r.status === 'rejected').length

      console.log(`Route JavaScript preloading completed: ${successful} successful, ${failed} failed`)
      return { successful, failed }
    } catch (error) {
      console.error('Route JavaScript preloading failed:', error)
      return { successful: 0, failed: routeJsFiles.length }
    }
  }

  // Get JavaScript files for specific route
  getRouteJsFiles(routePath) {
    const routeJsMap = {
      '/': ['/src/pages/IndexPage.vue'],
      '/login': ['/src/pages/LoginPage.vue'],
      '/users': ['/src/pages/master/UserPage.vue'],
      '/roles': ['/src/pages/master/RolePage.vue'],
      '/system-parameters': ['/src/pages/master/SystemParameterPage.vue'],
      '/pazaauto/barang': ['/src/pages/pazaauto/BarangPage.vue'],
      '/pazaauto/jasa': ['/src/pages/pazaauto/JasaPage.vue'],
      '/pazaauto/karyawan': ['/src/pages/pazaauto/KaryawanPage.vue'],
      '/pazaauto/karyawan-posisi': ['/src/pages/pazaauto/KaryawanPosisiPage.vue'],
      '/pazaauto/kendaraan': ['/src/pages/pazaauto/KendaraanPage.vue'],
      '/pazaauto/pelanggan': ['/src/pages/pazaauto/PelangganPage.vue'],
      '/pazaauto/supplier': ['/src/pages/pazaauto/SupplierPage.vue'],
      '/pazaauto/sparepart': ['/src/pages/pazaauto/SparepartPage.vue'],
      '/pazaauto/spk': ['/src/pages/pazaauto/SPKPage.vue'],
      '/pazaauto/pembelian': ['/src/pages/pazaauto/PembelianPage.vue'],
      '/pazaauto/penjualan': ['/src/pages/pazaauto/PenjualanPage.vue'],
      '/pazaauto/rekap-pembelian': ['/src/pages/pazaauto/RekapPembelianPage.vue'],
      '/pazaauto/rekap-penjualan': ['/src/pages/pazaauto/RekapPenjualanPage.vue'],
      '/pazaauto/absensi': ['/src/pages/pazaauto/AbsensiPage.vue'],
      '/offline-status': ['/src/pages/OfflineStatusPage.vue']
    }

    return routeJsMap[routePath] || []
  }

  // Warm up JavaScript cache for common patterns
  async warmUpJsCache() {
    console.log('Warming up JavaScript cache...')

    // Try to detect and preload common JS files
    const scripts = document.querySelectorAll('script[src]')
    const jsUrls = Array.from(scripts)
      .map(script => script.src)
      .filter(src => src.includes('.js') || src.includes('.vue') || src.includes('.mjs'))

    const preloadPromises = jsUrls.map(url => this.preloadJsOnDemand(url))

    try {
      const results = await Promise.allSettled(preloadPromises)
      const successful = results.filter(r => r.status === 'fulfilled').length

      console.log(`JavaScript cache warm-up completed: ${successful} files processed`)
      return successful
    } catch (error) {
      console.error('JavaScript cache warm-up failed:', error)
      return 0
    }
  }
}

// Create singleton instance
const jsPreloader = new JsPreloader()

export default jsPreloader
