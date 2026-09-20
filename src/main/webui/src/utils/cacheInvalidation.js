import masterDataCache from './masterDataCache.js'
import browserCache from './browserCache.js'
import syncService from '../services/syncService.js'

// Cache Storage bucket the service worker keeps API responses in (see src-pwa/custom-service-worker.js)
const SW_API_CACHE_NAME = 'api-cache'

// Paths that only group endpoints together: never safe to invalidate as a whole
const NON_COLLECTION_PATHS = new Set(['/api', '/api/pazaauto'])

// A successful write under `prefix` makes the cached responses of `invalidate` stale.
// Each entry lists its own endpoint plus every related endpoint whose data embeds or is derived from it.
export const WRITE_INVALIDATION_MAP = [
  { prefix: '/api/pazaauto/jasa', invalidate: ['/api/pazaauto/jasa'] },
  { prefix: '/api/pazaauto/barang', invalidate: ['/api/pazaauto/barang', '/api/pazaauto/sparepart'] },
  { prefix: '/api/pazaauto/sparepart', invalidate: ['/api/pazaauto/sparepart', '/api/pazaauto/barang'] },
  { prefix: '/api/pazaauto/supplier', invalidate: ['/api/pazaauto/supplier'] },
  // Saving a pelanggan may create a new merk / kendaraan master (typed-in merk or jenis)
  { prefix: '/api/pazaauto/pelanggan', invalidate: ['/api/pazaauto/pelanggan', '/api/pazaauto/kendaraan', '/api/pazaauto/merk-kendaraan'] },
  // KaryawanDto carries the posisi name, the role names and the login username
  { prefix: '/api/pazaauto/karyawan', invalidate: ['/api/pazaauto/karyawan', '/api/pazaauto/karyawan-posisi', '/api/pazaauto/absensi', '/api/users'] },
  { prefix: '/api/pazaauto/karyawan-posisi', invalidate: ['/api/pazaauto/karyawan-posisi', '/api/pazaauto/karyawan'] },
  { prefix: '/api/pazaauto/kendaraan', invalidate: ['/api/pazaauto/kendaraan', '/api/pazaauto/merk-kendaraan', '/api/pazaauto/pelanggan'] },
  { prefix: '/api/pazaauto/vehicles', invalidate: ['/api/pazaauto/vehicles', '/api/pazaauto/pelanggan', '/api/pazaauto/kendaraan'] },
  { prefix: '/api/pazaauto/merk-kendaraan', invalidate: ['/api/pazaauto/merk-kendaraan', '/api/pazaauto/kendaraan'] },
  // An SPK leaves the "unprocessed" list once it is turned into a penjualan / rekap penjualan
  { prefix: '/api/pazaauto/spk-detail', invalidate: ['/api/pazaauto/spk-detail', '/api/pazaauto/spk'] },
  { prefix: '/api/pazaauto/spk', invalidate: ['/api/pazaauto/spk', '/api/pazaauto/spk-detail', '/api/pazaauto/penjualan', '/api/pazaauto/rekap-penjualan'] },
  { prefix: '/api/pazaauto/penjualan-detail', invalidate: ['/api/pazaauto/penjualan-detail', '/api/pazaauto/penjualan', '/api/pazaauto/summary'] },
  { prefix: '/api/pazaauto/penjualan', invalidate: ['/api/pazaauto/penjualan', '/api/pazaauto/penjualan-detail', '/api/pazaauto/spk', '/api/pazaauto/summary'] },
  { prefix: '/api/pazaauto/rekap-penjualan', invalidate: ['/api/pazaauto/rekap-penjualan', '/api/pazaauto/spk', '/api/pazaauto/summary'] },
  // TbPembelianService moves sparepart stock on every purchase write
  { prefix: '/api/pazaauto/pembelian-detail', invalidate: ['/api/pazaauto/pembelian-detail', '/api/pazaauto/pembelian', '/api/pazaauto/sparepart', '/api/pazaauto/summary'] },
  { prefix: '/api/pazaauto/pembelian', invalidate: ['/api/pazaauto/pembelian', '/api/pazaauto/pembelian-detail', '/api/pazaauto/sparepart', '/api/pazaauto/barang', '/api/pazaauto/summary'] },
  { prefix: '/api/pazaauto/absensi', invalidate: ['/api/pazaauto/absensi'] },
  { prefix: '/api/roles', invalidate: ['/api/roles', '/api/permissions', '/api/users', '/api/pazaauto/karyawan'] },
  // Linking a user to a karyawan changes the karyawan list and its "unregistered" lookup
  { prefix: '/api/users', invalidate: ['/api/users', '/api/pazaauto/karyawan'] },
  { prefix: '/api/system-parameters', invalidate: ['/api/system-parameters'] }
]

// Path of a request URL, without its query string
function endpointPath(url) {
  return String(url || '').split('?')[0]
}

// True when `url` is the endpoint `prefix` itself or a path below it
// ('/api/users' matches '/api/users/5' but not '/api/users-archive')
function isUnderEndpoint(url, prefix) {
  const path = endpointPath(url)
  return path === prefix || path.startsWith(prefix + '/')
}

// Endpoints whose cached responses a successful write on `writeUrl` makes stale
export function getRelatedEndpoints(writeUrl) {
  const path = endpointPath(writeUrl)
  const entry = WRITE_INVALIDATION_MAP.find(({ prefix }) => isUnderEndpoint(path, prefix))
  if (entry) return entry.invalidate

  // Unmapped endpoint: fall back to the written path and the collection it belongs to
  const parent = path.slice(0, path.lastIndexOf('/'))
  return [path, parent].filter(p => p.startsWith('/api') && !NON_COLLECTION_PATHS.has(p))
}

// Drop the service worker's copies of the matching API responses
async function clearServiceWorkerApiCache(matches) {
  if (typeof caches === 'undefined') return
  if (!(await caches.has(SW_API_CACHE_NAME))) return

  const cache = await caches.open(SW_API_CACHE_NAME)
  const requests = await cache.keys()
  await Promise.all(
    requests
      .filter(request => {
        try {
          return matches(new URL(request.url).pathname)
        } catch {
          return false
        }
      })
      .map(request => cache.delete(request))
  )
}

// Remove every browser-side copy (IndexedDB lookup cache, offline store, localStorage, service worker)
// of the given endpoints, so the next read goes to the server.
export async function invalidateEndpoints(endpoints) {
  if (!endpoints || endpoints.length === 0) return

  const matches = (url) => endpoints.some(prefix => isUnderEndpoint(url, prefix))

  const results = await Promise.allSettled([
    ...endpoints.map(prefix => masterDataCache.invalidatePrefix(prefix)),
    Promise.resolve().then(() => browserCache.removeApiResponsesMatching(matches)),
    syncService.storage.clearDataMatching(matches),
    clearServiceWorkerApiCache(matches)
  ])

  results
    .filter(r => r.status === 'rejected')
    .forEach(r => console.warn('[cacheInvalidation] failed to clear a cache:', r.reason))
}

// Called after a successful write: resets the cache of the written endpoint and of its related entities
export function invalidateRelatedCaches(writeUrl) {
  return invalidateEndpoints(getRelatedEndpoints(writeUrl))
}
