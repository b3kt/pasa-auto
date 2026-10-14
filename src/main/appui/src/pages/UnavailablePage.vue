<template>
  <q-page class="flex flex-center">
    <q-card style="min-width: 350px" flat bordered>
      <q-card-section class="text-center">
        <q-icon name="build" size="48px" color="grey-6" />
        <div class="text-h6 q-mt-md">{{ $t('pages.unavailable.title') }}</div>
        <div class="text-body2 text-grey-7 q-mt-sm">
          {{ $t('pages.unavailable.message') }}
        </div>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
</script>
