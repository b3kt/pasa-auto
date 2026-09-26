<template>
  <q-page class="flex flex-center column">
    <!--
      A freshly approved Google account has no roles yet, so every menu entry is hidden and every
      endpoint answers 403. Say so, rather than showing an empty shell that looks broken.
    -->
    <q-banner v-if="awaitingRoles" class="bg-warning text-dark q-mb-md" style="max-width: 640px">
      <template v-slot:avatar>
        <q-icon name="hourglass_top" />
      </template>
      {{ $t('pages.indexPage.noRolesBanner') }}
    </q-banner>

    <div class="q-pa-md">
      <AttendanceClockCard v-if="isKaryawan" class="lt-sm" />
    </div>
    <img alt="Pasa Auto" class="gt-sm" src="~assets/pasa.svg" style="width: 100%; height: auto; max-height: 640px; ">
  </q-page>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from 'stores/auth-store'
import { Roles } from 'src/constants/roles'
import AttendanceClockCard from 'components/AttendanceClockCard.vue'

const authStore = useAuthStore()

const awaitingRoles = computed(() => {
  if (!authStore.isLoggedIn) return false
  return (authStore.user?.roles || []).length === 0
})

const isKaryawan = computed(() => (authStore.user?.roles || []).includes(Roles.KARYAWAN))
</script>
