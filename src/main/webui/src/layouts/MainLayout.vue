<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated>
      <q-toolbar>
        <q-toolbar-title class="text-uppercase text-bold" shrink>
          {{ $t('app.constant.app_name') }}
        </q-toolbar-title>

        <q-separator vertical inset dark class="q-mx-md" />

        <div class="row no-wrap">
          <template v-for="menu in linksList" :key="menu.title">
            <q-btn-dropdown
              v-if="menu.visible"
              flat
              stretch
              no-caps
              :label="menu.title"
              :icon="menu.icon"
            >
              <q-list dense>
                <q-item
                  v-for="child in menu.children"
                  :key="child.title"
                  v-bind="child"
                  clickable
                  v-close-popup
                  :to="child.link"
                  v-show="child.visible"
                >
                  <q-item-section>
                    <q-item-label class="q-pa-sm">{{ child.title }}</q-item-label>
                  </q-item-section>
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </template>
        </div>

        <q-space />

        <div class="q-gutter-sm row items-center no-wrap">
          <q-btn flat no-caps v-if="authStore.isLoggedIn">
            <div class="row items-center no-wrap">
              <q-icon name="person" class="q-mr-xs" />
              <div class="text-center">
                {{ user.username }}
              </div>
            </div>
            <q-menu>
              <q-list dense style="min-width: 150px">
                <q-item-label header>User Info</q-item-label>
                <q-item clickable v-close-popup>
                  <q-item-section>
                    <q-item-label>{{ user.username }}</q-item-label>
                    <q-item-label caption>{{ user?.roles?.join(', ') }}</q-item-label>
                  </q-item-section>
                </q-item>
                <q-separator />
                <q-item clickable v-close-popup @click="handleLogout">
                  <q-item-section avatar>
                    <q-icon name="logout" />
                  </q-item-section>
                  <q-item-section>{{ $t('logout') }}</q-item-section>
                </q-item>
              </q-list>
            </q-menu>
          </q-btn>
          <span v-else>Quasar v{{ $q.version }}</span>
        </div>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'stores/auth-store'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const $q = useQuasar()
const { t } = useI18n()
const authStore = useAuthStore()

const user = computed(() => authStore.user)

const hasRole = (role) => {
  return user.value?.roles?.includes(role) || false
}

const linksList = computed(() => [
  {
    title: t('app.menu.master.title'),
    caption: t('app.menu.master.caption'),
    icon: 'warehouse',
    visible: hasRole('Admin') || hasRole('Owner'),
    children: [

      {
        title: t('app.menu.master.employee.title'),
        caption: t('app.menu.master.employee.caption'),
        icon: 'warehouse',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/pazaauto/karyawan'
      },
      {
        title: t('app.menu.master.service.title'),
        caption: t('app.menu.master.service.caption'),
        icon: 'warehouse',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/pazaauto/jasa'
      },
      {
        title: t('app.menu.master.product.title'),
        caption: t('app.menu.master.product.caption'),
        icon: 'warehouse',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/pazaauto/barang'
      },
      {
        title: t('app.menu.master.customer.title'),
        caption: t('app.menu.master.customer.caption'),
        icon: 'warehouse',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/pazaauto/pelanggan'
      },
      {
        title: t('app.menu.master.vehicle.title'),
        caption: t('app.menu.master.vehicle.caption'),
        icon: 'warehouse',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/pazaauto/kendaraan'
      },
      {
        title: t('app.menu.master.supplier.title'),
        caption: t('app.menu.master.supplier.caption'),
        icon: 'warehouse',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/pazaauto/supplier'
      },
    ]
  },
  {
    title: t('app.menu.process.title'),
    caption: t('app.menu.process.caption'),
    icon: 'conveyor_belt',
    link: 'https://quasar.dev',
    visible: hasRole('Admin') || hasRole('Karyawan'),
    children: [
      {
        title: t('app.menu.process.order.title'),
        caption: t('app.menu.process.order.caption'),
        icon: 'warehouse',
        link: '/pazaauto/spk',
        visible: hasRole('Admin') || hasRole('Owner'),
      }
    ]
  },
  {
    title: t('app.menu.transaction.title'),
    caption: t('app.menu.transaction.caption'),
    icon: 'swap_horiz',
    visible: hasRole('Admin') || hasRole('Owner'),
    children: [
      {
        title: t('app.menu.sales.buy.title'),
        caption: t('app.menu.sales.buy.caption'),
        icon: 'warehouse',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/pazaauto/pembelian'
      },
      {
        title: t('app.menu.sales.sell.title'),
        caption: t('app.menu.sales.sell.caption'),
        icon: 'warehouse',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/pazaauto/penjualan-barang'
      },
    ]
  },
  {
    title: t('app.menu.report.title'),
    caption: t('app.menu.report.caption'),
    icon: 'analytics',
    visible: hasRole('Admin') || hasRole('Owner'),
    children: [
      {
        title: t('app.menu.recap.buy.title'),
        caption: t('app.menu.recap.buy.caption'),
        icon: 'warehouse',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/pazaauto/pembelian'
      },
      {
        title: t('app.menu.recap.sell.title'),
        caption: t('app.menu.recap.sell.caption'),
        icon: 'warehouse',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/pazaauto/penjualan-barang'
      },
    ]
  },
  {
    title: t('app.menu.admin.title'),
    caption: t('app.menu.admin.caption'),
    icon: 'settings',
    visible: hasRole('Admin') || hasRole('Owner'),
    children: [
      {
        title: t('app.menu.admin.user.title'),
        caption: t('app.menu.admin.user.caption'),
        icon: 'user',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/users'
      },
      {
        title: t('app.menu.admin.role.title'),
        caption: t('app.menu.admin.role.caption'),
        icon: 'group',
        visible: hasRole('Admin') || hasRole('Owner'),
        link: '/roles'
      },
    ]
  }
])

async function handleLogout() {
  await authStore.logout()
  $q.notify({
    type: 'info',
    message: 'Logged out successfully',
    position: 'top'
  })
  router.push('/login')
}

</script>
