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
      { path: 'users', component: () => import('pages/master/UserPage.vue') },
      { path: 'roles', component: () => import('pages/master/RolePage.vue') },
      { path: 'roles/:id', component: () => import('pages/master/RoleViewPage.vue') },
      { path: 'system-parameters', component: () => import('pages/master/SystemParameterPage.vue') },
      { path: 'pazaauto/barang', component: () => import('pages/pazaauto/BarangPage.vue') },
      { path: 'pazaauto/jasa', component: () => import('pages/pazaauto/JasaPage.vue') },
      { path: 'pazaauto/karyawan', component: () => import('pages/pazaauto/KaryawanPage.vue') },
      { path: 'pazaauto/karyawan-posisi', component: () => import('pages/pazaauto/KaryawanPosisiPage.vue') },
      { path: 'pazaauto/kendaraan', component: () => import('pages/pazaauto/KendaraanPage.vue') },
      { path: 'pazaauto/pelanggan', component: () => import('pages/pazaauto/PelangganPage.vue') },
      { path: 'pazaauto/supplier', component: () => import('pages/pazaauto/SupplierPage.vue') },
      { path: 'pazaauto/sparepart', component: () => import('pages/pazaauto/SparepartPage.vue') },
      { path: 'pazaauto/spk', component: () => import('pages/pazaauto/SPKPage.vue') },
      { path: 'pazaauto/pembelian', component: () => import('pages/pazaauto/PembelianPage.vue') },
      { path: 'pazaauto/penjualan-barang', component: () => import('pages/pazaauto/PenjualanBarangPage.vue') },
      { path: 'pazaauto/absensi', component: () => import('pages/pazaauto/AbsensiPage.vue') }

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
