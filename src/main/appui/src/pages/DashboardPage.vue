<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">{{ $t('pages.dashboard.welcome', { name: profile?.fullName }) }}</div>

    <q-banner v-if="profile && !profile.linked" class="bg-warning text-white q-mb-md">
      {{ $t('pages.dashboard.notLinked') }}
      <template v-slot:action>
        <q-btn flat color="white" :label="$t('common.linkNow')" to="/link-account" />
      </template>
    </q-banner>

    <div v-if="profile?.linked" class="row q-col-gutter-md">
      <div class="col-12 col-md-4">
        <q-card class="cursor-pointer" @click="$router.push('/profile')">
          <q-card-section>
            <div class="text-subtitle2">{{ $t('pages.dashboard.customerRecord') }}</div>
            <div class="text-body1">{{ profile.namaPelanggan }}</div>
            <div class="text-caption text-grey-7">{{ profile.alamat }}</div>
            <div class="text-caption text-primary q-mt-xs">{{ $t('pages.dashboard.editProfile') }}</div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-12 col-md-4">
        <q-card class="cursor-pointer" @click="$router.push('/vehicles')">
          <q-card-section>
            <div class="text-subtitle2">{{ $t('pages.dashboard.myVehicles') }}</div>
            <div v-if="vehicles.length > 0" class="q-mt-sm">
              <div v-for="v in vehicles.slice(0, 3)" :key="v.id" class="text-body2 q-mb-xs">
                <div class="text-weight-medium">{{ v.nopol }}</div>
                <div class="text-caption text-grey-7">{{ v.merk }} {{ v.jenis }}</div>
              </div>
              <div v-if="vehicles.length > 3" class="text-caption text-primary q-mt-xs">
                +{{ vehicles.length - 3 }} more
              </div>
            </div>
            <div v-else class="text-caption text-grey-7 q-mt-sm">
              {{ $t('pages.dashboard.noVehicles') }}
            </div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-12 col-md-4">
        <q-card class="cursor-pointer" @click="$router.push('/bookings')">
          <q-card-section>
            <div class="text-subtitle2">{{ $t('pages.dashboard.bookService') }}</div>
            <div v-if="upcomingBookings.length > 0" class="q-mt-sm">
              <div v-for="b in upcomingBookings.slice(0, 2)" :key="b.id" class="text-body2 q-mb-xs">
                <div class="text-weight-medium">{{ b.serviceType || $t('pages.bookings.generalService') }}</div>
                <div class="text-caption text-grey-7">{{ formatDate(b.requestedDateTime) }}</div>
                <div class="text-caption">{{ b.nopol || $t('pages.bookings.noPlate') }}</div>
              </div>
              <div v-if="upcomingBookings.length > 2" class="text-caption text-primary q-mt-xs">
                +{{ upcomingBookings.length - 2 }} more
              </div>
            </div>
            <div v-else class="text-caption text-grey-7 q-mt-sm">
              {{ $t('pages.dashboard.noUpcomingBookings') }}
            </div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-12">
        <q-card class="cursor-pointer" @click="$router.push('/spk')">
          <q-card-section>
            <div class="row items-center justify-between">
              <div class="text-subtitle2">{{ $t('pages.dashboard.serviceHistory') }}</div>
              <div class="text-caption text-primary">{{ $t('pages.dashboard.viewAll') }}</div>
            </div>
            <div v-if="latestService" class="row items-center justify-between q-mt-sm">
              <div>
                <div class="text-weight-medium">{{ latestService.noSpk }} &mdash; {{ latestService.nopol }}</div>
                <div class="text-caption text-grey-7">{{ latestService.tanggalJamSpk }}</div>
                <div v-if="latestService.keterangan" class="text-caption">{{ latestService.keterangan }}</div>
              </div>
              <q-badge :color="statusColor(latestService.statusSpk)">
                {{ statusLabel(latestService.statusSpk) }}
              </q-badge>
            </div>
            <div v-else class="text-caption text-grey-7 q-mt-sm">
              {{ $t('pages.dashboard.noServiceHistory') }}
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { api } from 'boot/axios'

const { t, te, locale } = useI18n()
const profile = ref(null)
const vehicles = ref([])
const bookings = ref([])
const serviceHistory = ref([])

const upcomingBookings = ref([])
const latestService = computed(() => serviceHistory.value[0] || null)

const statusColor = (status) => ({
  OPEN: 'grey',
  PROSES: 'orange',
  SELESAI: 'positive',
  BATAL: 'negative'
})[status] || 'grey'

const statusLabel = (status) => te(`pages.spk.status.${status}`)
  ? t(`pages.spk.status.${status}`)
  : status

const formatDate = (value) => {
  if (!value) return ''
  const date = new Date(value)
  return date.toLocaleString(locale.value, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(async () => {
  try {
    const profileResponse = await api.get('/api/customer/profile')
    profile.value = profileResponse.data?.data

    if (profile.value?.linked) {
      const vehiclesResponse = await api.get('/api/customer/vehicles')
      vehicles.value = vehiclesResponse.data?.data || []

      const bookingsResponse = await api.get('/api/customer/bookings')
      bookings.value = bookingsResponse.data?.data || []

      upcomingBookings.value = bookings.value.filter(b =>
        b.status === 'REQUESTED' || b.status === 'CONFIRMED'
      ).sort((a, b) => new Date(a.requestedDateTime) - new Date(b.requestedDateTime))

      const serviceResponse = await api.get('/api/customer/spk')
      serviceHistory.value = serviceResponse.data?.data || []
    }
  } catch (e) {
    console.error('Failed to load dashboard data', e)
  }
})
</script>
