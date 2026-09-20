<template>
  <div class="row q-col-gutter-sm q-mb-sm">
    <div v-for="stat in statCards" :key="stat.label" class="col-12 col-sm-4">
      <q-card flat bordered>
        <q-card-section class="row items-center no-wrap q-py-sm">
          <q-icon :name="stat.icon" :color="stat.color" size="28px" class="q-mr-md"/>
          <div class="col">
            <div class="text-caption text-grey-7">{{ stat.label }}</div>
            <div class="text-h6 text-weight-medium">
              <q-skeleton v-if="loading" type="text" width="80px"/>
              <template v-else>{{ stat.value }}</template>
            </div>
            <div class="text-caption text-grey-6">{{ stat.caption }}</div>
          </div>
        </q-card-section>
      </q-card>
    </div>
  </div>
</template>

<script setup>
import {computed} from 'vue'

const props = defineProps({
  // Totals from /api/pazaauto/rekap-penjualan/summary, null until the first response
  stats: {type: Object, default: null},
  loading: {type: Boolean, default: false}
})

const formatCurrency = (value) => {
  if (!value) return 'Rp 0'
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    minimumFractionDigits: 0
  }).format(value)
}

// Seconds as the two most significant units, e.g. 699 -> "11 mnt 39 dtk"
const formatDuration = (seconds) => {
  if (seconds === null || seconds === undefined) return '-'
  const total = Math.round(seconds)
  const parts = [
    {value: Math.floor(total / 3600), unit: 'jam'},
    {value: Math.floor((total % 3600) / 60), unit: 'mnt'},
    {value: total % 60, unit: 'dtk'}
  ]
  const shown = parts.slice(parts.findIndex(p => p.value > 0)).slice(0, 2).filter(p => p.value > 0)
  return shown.length ? shown.map(p => `${p.value} ${p.unit}`).join(' ') : '0 dtk'
}

const statCards = computed(() => {
  const s = props.stats
  return [
    {
      label: 'Pelanggan / Kendaraan',
      icon: 'groups',
      color: 'primary',
      value: `${s?.totalPelanggan ?? 0} / ${s?.totalKendaraan ?? 0}`,
      caption: `dari ${s?.totalSpk ?? 0} SPK`
    },
    {
      label: 'Total dibayar',
      icon: 'payments',
      color: 'green',
      value: formatCurrency(s?.totalDibayar),
      caption: 'sesuai filter yang dipilih'
    },
    {
      label: 'Rata-rata pengerjaan',
      icon: 'schedule',
      color: 'orange',
      value: formatDuration(s?.avgCompletionSeconds),
      caption: `dari ${s?.completedCount ?? 0} SPK yang tercatat waktunya`
    }
  ]
})
</script>
