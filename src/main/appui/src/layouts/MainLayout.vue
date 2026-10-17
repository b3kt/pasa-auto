<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated class="bg-secondary">
      <q-toolbar>
        <q-btn
          v-if="authStore.canBrowse"
          flat
          dense
          round
          icon="menu"
          :aria-label="$t('common.menu')"
          class="lt-md q-mr-sm"
          @click="drawer = !drawer"
        />
        <q-toolbar-title class="text-uppercase text-bold" shrink>
          {{ $t('app.name') }}
        </q-toolbar-title>
        <q-space />

        <!-- Horizontal menu: desktop/tablet only (mobile uses the drawer below) -->
        <div v-if="authStore.canBrowse" class="q-gutter-sm row items-center no-wrap gt-sm">
          <q-btn v-for="link in links" :key="link.to" flat dense :to="link.to" :label="link.label" />
          <q-btn flat dense icon="logout" :label="$t('common.logout')" @click="handleLogout" />
        </div>

        <!-- Language switcher: always available, signed in or not -->
        <q-btn flat dense round icon="translate" class="q-ml-sm" :aria-label="$t('language.label')">
          <q-menu>
            <q-list style="min-width: 180px">
              <q-item
                v-for="lng in languages"
                :key="lng.code"
                v-close-popup
                clickable
                @click="setLocale(lng.code)"
              >
                <q-item-section avatar>
                  <q-icon name="check" :class="{ invisible: locale !== lng.code }" />
                </q-item-section>
                <q-item-section>{{ lng.label }}</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </q-toolbar>

      <q-banner v-if="!authStore.online" dense class="bg-warning text-dark">
        <template #avatar>
          <q-icon name="cloud_off" />
        </template>
        {{ $t('common.offline') }}
      </q-banner>
    </q-header>

    <q-drawer
      v-if="authStore.canBrowse"
      v-model="drawer"
      side="left"
      bordered
      class=" lt-md"
    >
      <q-list padding>
        <q-item
          v-for="link in links"
          :key="link.to"
          v-ripple
          clickable
          exact
          :to="link.to"
          @click="drawer = false"
        >
          <q-item-section avatar>
            <q-icon :name="link.icon" />
          </q-item-section>
          <q-item-section>{{ link.label }}</q-item-section>
        </q-item>

        <q-separator />

        <q-item v-ripple clickable @click="handleLogout">
          <q-item-section avatar>
            <q-icon name="logout" />
          </q-item-section>
          <q-item-section>{{ $t('common.logout') }}</q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from 'stores/auth-store'
import { applyLocale } from 'boot/i18n'

const router = useRouter()
const $q = useQuasar()
const { t, locale } = useI18n({ useScope: 'global' })
const authStore = useAuthStore()

const drawer = ref(false)

const links = computed(() => [
  { to: '/', label: t('nav.dashboard'), icon: 'dashboard' },
  { to: '/profile', label: t('nav.profile'), icon: 'person' },
  { to: '/vehicles', label: t('nav.vehicles'), icon: 'directions_car' },
  { to: '/bookings', label: t('nav.bookings'), icon: 'event' },
  { to: '/spk', label: t('nav.serviceStatus'), icon: 'build' }
])

const languages = computed(() => [
  { code: 'id-ID', label: t('language.id') },
  { code: 'en-US', label: t('language.en') }
])

function setLocale(code) {
  locale.value = code
  applyLocale(code)
}

async function handleLogout() {
  drawer.value = false
  await authStore.logout()
  $q.notify({ type: 'info', message: t('common.loggedOut') })
  router.push('/login')
}
</script>
