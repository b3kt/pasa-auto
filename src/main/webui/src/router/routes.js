import { Roles } from 'src/constants/roles'

// Pages each role may open; mirrors the backend @RolesAllowed rules (the backend stays the source of truth)
const ROLES_OWNER_ONLY = [Roles.OWNER]
const ROLES_ADMIN_OWNER = [Roles.ADMIN, Roles.OWNER]
const ROLES_ALL_STAFF = [Roles.ADMIN, Roles.OWNER, Roles.KARYAWAN]

const routes = [
  {
    path: '/login',
    component: () => import('layouts/PublicLayout.vue'),
    meta: { requiresAuth: false },
    children: [
      { path: '', component: () => import('pages/LoginPage.vue') }
    ]
  },
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      { path: 'users', meta: { roles: ROLES_OWNER_ONLY }, component: () => import('pages/master/UserPage.vue') },
      { path: 'users/pending', meta: { roles: ROLES_OWNER_ONLY }, component: () => import('pages/master/PendingApprovalPage.vue') },
      { path: 'roles', meta: { roles: ROLES_OWNER_ONLY }, component: () => import('pages/master/RolePage.vue') },
      { path: 'roles/:id', meta: { roles: ROLES_OWNER_ONLY }, component: () => import('pages/master/RoleViewPage.vue') },
      { path: 'system-parameters', meta: { roles: ROLES_OWNER_ONLY }, component: () => import('pages/master/SystemParameterPage.vue') },
      { path: 'pazaauto/barang', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/BarangPage.vue') },
      { path: 'pazaauto/jasa', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/JasaPage.vue') },
      { path: 'pazaauto/karyawan', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/KaryawanPage.vue') },
      { path: 'pazaauto/karyawan-posisi', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/KaryawanPosisiPage.vue') },
      { path: 'pazaauto/kendaraan', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/KendaraanPage.vue') },
      { path: 'pazaauto/pelanggan', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/PelangganPage.vue') },
      { path: 'pazaauto/supplier', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/SupplierPage.vue') },
      { path: 'pazaauto/sparepart', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/SparepartPage.vue') },
      { path: 'pazaauto/spk', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/SPKPage.vue') },
      { path: 'pazaauto/pembelian', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/PembelianPage.vue') },
      { path: 'pazaauto/penjualan', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/PenjualanPage.vue') },
      { path: 'pazaauto/rekap-pembelian', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/RekapPembelianPage.vue') },
      { path: 'pazaauto/rekap-penjualan', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/pazaauto/RekapPenjualanPage.vue') },
      { path: 'pazaauto/summary', meta: { roles: ROLES_OWNER_ONLY }, component: () => import('pages/pazaauto/SummaryPage.vue') },
      { path: 'pazaauto/absensi', meta: { roles: ROLES_ALL_STAFF }, component: () => import('pages/pazaauto/AbsensiPage.vue') },
      { path: 'offline-status', component: () => import('pages/OfflineStatusPage.vue') },
      { path: 'profile', component: () => import('pages/ProfilePage.vue') },
      { path: 'change-password', component: () => import('pages/ChangePasswordPage.vue') },
      { path: 'admin/clear-cache', meta: { roles: ROLES_ALL_STAFF }, component: () => import('pages/admin/ClearCachePage.vue') },
      { path: 'admin/audit-trail', meta: { roles: ROLES_ADMIN_OWNER }, component: () => import('pages/admin/AuditTrailPage.vue') }

    ]
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue')
  }
]

export default routes
