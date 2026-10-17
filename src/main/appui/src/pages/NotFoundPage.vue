<template>
  <q-page class="flex flex-center">
    <q-card style="min-width: 350px" flat bordered>
      <q-card-section class="text-center">
        <q-icon name="help_outline" size="48px" color="grey-6" />
        <div class="text-h6 q-mt-md">{{ $t('pages.notFound.title') }}</div>
        <div class="text-body2 text-grey-7 q-mt-sm">
          {{ $t('pages.notFound.message') }}
        </div>
        <q-btn class="q-mt-md" color="primary" :label="$t('common.backToDashboard')" :to="authStore.canBrowse ? '/' : '/login'" />
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { useAuthStore } from 'stores/auth-store'

const authStore = useAuthStore()
</script>
