// The customer portal is a standalone app served from /app/, so its paths carry no /portal prefix.
// The backend stays the source of truth for who may see what (see the /api/customer/* resources);
// routes only declare whether they need a customer session (meta.customerAuth, default true).
const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    meta: { customerAuth: true },
    children: [
      { path: '', component: () => import('pages/DashboardPage.vue') },
      { path: 'profile', component: () => import('pages/ProfilePage.vue') },
      { path: 'link-account', component: () => import('pages/LinkAccountPage.vue') },
      { path: 'vehicles', component: () => import('pages/VehiclesPage.vue') },
      { path: 'bookings', component: () => import('pages/BookingsPage.vue') },
      { path: 'spk', component: () => import('pages/SpkTrackingPage.vue') },

      // Reachable without a session; the header hides its navigation until the customer signs in
      { path: 'login', meta: { customerAuth: false }, component: () => import('pages/LoginPage.vue') },
      { path: 'register', meta: { customerAuth: false }, component: () => import('pages/RegisterPage.vue') },
      { path: 'unavailable', meta: { customerAuth: false }, component: () => import('pages/UnavailablePage.vue') },

      // Always leave this as last one
      { path: ':catchAll(.*)*', meta: { customerAuth: false }, component: () => import('pages/NotFoundPage.vue') }
    ]
  }
]

export default routes
