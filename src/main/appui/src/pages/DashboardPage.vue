<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">Welcome, {{ profile?.fullName }}</div>

    <q-banner v-if="profile && !profile.linked" class="bg-warning text-white q-mb-md">
      Your account isn't linked to your service history yet.
      <template v-slot:action>
        <q-btn flat color="white" label="Link now" to="/link-account" />
      </template>
    </q-banner>

    <div v-if="profile?.linked" class="row q-col-gutter-md">
      <div class="col-12 col-md-4">
        <q-card class="cursor-pointer" @click="$router.push('/profile')">
          <q-card-section>
            <div class="text-subtitle2">Customer record</div>
            <div class="text-body1">{{ profile.namaPelanggan }}</div>
            <div class="text-caption text-grey-7">{{ profile.alamat }}</div>
            <div class="text-caption text-primary q-mt-xs">Edit profile</div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-12 col-md-4">
        <q-card class="cursor-pointer" @click="$router.push('/vehicles')">
          <q-card-section>
            <div class="text-subtitle2">My vehicles</div>
            <div class="text-caption text-grey-7">View your registered vehicles</div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-12 col-md-4">
        <q-card class="cursor-pointer" @click="$router.push('/bookings')">
          <q-card-section>
            <div class="text-subtitle2">Book a service</div>
            <div class="text-caption text-grey-7">Request an appointment</div>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from 'boot/axios'

const profile = ref(null)

onMounted(async () => {
  try {
    const response = await api.get('/api/customer/profile')
    profile.value = response.data?.data
  } catch (e) {
    console.error('Failed to load profile', e)
  }
})
</script>
