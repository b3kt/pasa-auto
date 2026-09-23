<template>
  <q-page padding>
    <!-- Header & Filters -->
    <div class="row items-center q-mb-md q-gutter-sm">
      <div class="text-h6 col-auto">{{ $t('pages.summaryPage.title') }}</div>
      <q-space />

      <!-- Pembelian status filter -->
      <q-select
        v-model="filterStatusPembelian"
        :options="statusPembelianOptions"
        :label="$t('pages.summaryPage.statusPembelianLabel')"
        outlined dense clearable multiple
        style="min-width: 200px"
        emit-value map-options
      />

      <!-- Date range -->
      <q-input :model-value="dateRangeText" :label="$t('period')" outlined dense readonly style="min-width: 220px">
        <template v-slot:append>
          <q-icon name="event" class="cursor-pointer">
            <q-popup-proxy cover transition-show="scale" transition-hide="scale">
              <q-date v-model="dateRange" range>
                <div class="row items-center justify-end q-gutter-sm">
                  <q-btn :label="$t('clear')" color="primary" flat @click="clearDateRange" v-close-popup />
                  <q-btn :label="$t('close')" color="primary" flat v-close-popup />
                </div>
              </q-date>
            </q-popup-proxy>
          </q-icon>
        </template>
      </q-input>

      <q-btn icon="refresh" color="primary" dense flat :loading="loading" @click="fetchSummary">
        <q-tooltip>{{ $t('refresh') }}</q-tooltip>
      </q-btn>
    </div>

    <div v-if="loading" class="row justify-center q-pa-xl">
      <q-spinner-dots color="primary" size="60px" />
    </div>

    <template v-else-if="summary">
      <!-- Stat Cards -->
      <div class="row q-col-gutter-md q-mb-md">
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered>
            <q-card-section class="row items-center no-wrap">
              <q-icon name="people" size="40px" color="primary" class="q-mr-md" />
              <div>
                <div class="text-caption text-grey">{{ $t('pages.summaryPage.totalPelangganLabel') }}</div>
                <div class="text-h5 text-weight-bold">{{ summary.totalCustomers }}</div>
              </div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered>
            <q-card-section class="row items-center no-wrap">
              <q-icon name="trending_up" size="40px" color="green" class="q-mr-md" />
              <div>
                <div class="text-caption text-grey">{{ $t('pages.summaryPage.totalPemasukanLabel') }}</div>
                <div class="text-h5 text-weight-bold text-green">{{ formatCurrency(summary.totalIncome) }}</div>
              </div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered>
            <q-card-section class="row items-center no-wrap">
              <q-icon name="trending_down" size="40px" color="red" class="q-mr-md" />
              <div>
                <div class="text-caption text-grey">{{ $t('pages.summaryPage.totalPengeluaranLabel') }}</div>
                <div class="text-h5 text-weight-bold text-red">{{ formatCurrency(summary.totalOutcome) }}</div>
              </div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-3">
          <q-card flat bordered>
            <q-card-section class="row items-center no-wrap">
              <q-icon name="inventory_2" size="40px" color="orange" class="q-mr-md" />
              <div>
                <div class="text-caption text-grey">{{ $t('pages.summaryPage.itemTerjualLabel') }}</div>
                <div class="text-h5 text-weight-bold">{{ summary.totalItemTerjual }} {{ $t('pages.summaryPage.pcsShort') }}</div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Net Profit Banner -->
      <q-card flat bordered class="q-mb-md" :class="netProfitColor">
        <q-card-section class="row items-center">
          <q-icon :name="summary.netProfit >= 0 ? 'arrow_upward' : 'arrow_downward'" size="28px" class="q-mr-sm" />
          <span class="text-subtitle1 text-weight-bold">{{ $t('pages.summaryPage.netProfitLabel', { amount: formatCurrency(summary.netProfit) }) }}</span>
        </q-card-section>
      </q-card>

      <!-- Trend Chart -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-subtitle1 q-mb-sm">{{ $t('pages.summaryPage.trendDailyTitle') }}</div>
          <Chart type="bar" :data="trendChartData" :options="trendChartOptions" style="max-height: 300px" />
        </q-card-section>
      </q-card>

      <!-- Top Items + Donut Charts -->
      <div class="row q-col-gutter-md q-mb-md">
        <div class="col-12 col-md-4">
          <q-card flat bordered style="height: 100%">
            <q-card-section>
              <div class="text-subtitle1 q-mb-sm">{{ $t('pages.summaryPage.topItemsTitle') }}</div>
              <Bar :data="topItemsChartData" :options="topItemsChartOptions" style="max-height: 280px" />
            </q-card-section>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-4">
          <q-card flat bordered style="height: 100%">
            <q-card-section>
              <div class="text-subtitle1 q-mb-sm">{{ $t('pages.summaryPage.incomeByMethodTitle') }}</div>
              <Doughnut :data="incomeMethodChartData" :options="donutOptions" style="max-height: 280px" />
            </q-card-section>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-4">
          <q-card flat bordered style="height: 100%">
            <q-card-section>
              <div class="text-subtitle1 q-mb-sm">{{ $t('pages.summaryPage.outcomeByTypeTitle') }}</div>
              <Doughnut :data="outcomeTypeChartData" :options="donutOptions" style="max-height: 280px" />
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Mechanic Chart -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-subtitle1 q-mb-sm">{{ $t('pages.summaryPage.mechanicPerformanceTitle') }}</div>
          <Bar :data="mekanikChartData" :options="mekanikChartOptions" style="max-height: 300px" />
        </q-card-section>
      </q-card>

      <!-- Mechanic Summary Table -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-subtitle1 q-mb-sm">{{ $t('pages.summaryPage.mechanicSummaryTitle') }}</div>
          <q-table
            :rows="summary.mekanikSummary"
            :columns="mekanikSummaryColumns"
            row-key="mekanikId"
            flat dense
            :pagination="{ rowsPerPage: 0 }"
            hide-bottom
          >
            <template v-slot:body-cell-rataPerHari="props">
              <q-td :props="props">{{ props.row.rataPerHari.toFixed(1) }}</q-td>
            </template>
          </q-table>
        </q-card-section>
      </q-card>

      <!-- Daily Breakdown Table -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-subtitle1 q-mb-sm">{{ $t('pages.summaryPage.dailyBreakdownTitle') }}</div>
          <q-table
            :rows="summary.dailyBreakdown"
            :columns="dailyColumns"
            row-key="date"
            flat dense
            :pagination="{ rowsPerPage: 0 }"
            hide-bottom
          >
            <template v-slot:body-cell-income="props">
              <q-td :props="props" class="text-green">{{ formatCurrency(props.row.income) }}</q-td>
            </template>
            <template v-slot:body-cell-outcome="props">
              <q-td :props="props" class="text-red">{{ formatCurrency(props.row.outcome) }}</q-td>
            </template>
            <template v-slot:body-cell-net="props">
              <q-td :props="props" :class="props.row.net >= 0 ? 'text-green' : 'text-red'">
                {{ formatCurrency(props.row.net) }}
              </q-td>
            </template>
          </q-table>
        </q-card-section>
      </q-card>

      <!-- Mechanic Daily Table -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="row items-center q-mb-sm">
            <div class="text-subtitle1">{{ $t('pages.summaryPage.mechanicDailyTitle') }}</div>
            <q-space />
            <q-input v-model="mekanikSearch" :placeholder="$t('pages.summaryPage.mechanicSearchPlaceholder')" dense outlined clearable style="max-width: 250px">
              <template v-slot:prepend><q-icon name="search" /></template>
            </q-input>
          </div>
          <q-table
            :rows="filteredMekanikDaily"
            :columns="mekanikDailyColumns"
            row-key="rowKey"
            flat dense
            :rows-per-page-options="[5, 10, 25, 50]"
            :pagination="mekanikDailyPagination"
            @update:pagination="mekanikDailyPagination = $event"
          />
        </q-card-section>
      </q-card>

      <!-- Sold Items Table -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="row items-center q-mb-sm">
            <div class="text-subtitle1">{{ $t('pages.summaryPage.soldItemsTitle') }}</div>
            <q-space />
            <q-input v-model="itemSearch" :placeholder="$t('pages.summaryPage.itemSearchPlaceholder')" dense outlined clearable style="max-width: 250px">
              <template v-slot:prepend><q-icon name="search" /></template>
            </q-input>
          </div>
          <q-table
            :rows="filteredSoldItems"
            :columns="soldItemsColumns"
            row-key="rowKey"
            flat dense
            :rows-per-page-options="[5, 10, 25, 50]"
            :pagination="soldItemsPagination"
            @update:pagination="soldItemsPagination = $event"
          >
            <template v-slot:body-cell-totalValue="props">
              <q-td :props="props">{{ formatCurrency(props.row.totalValue) }}</q-td>
            </template>
            <template v-slot:body-cell-totalNilaiAdjustment="props">
              <q-td :props="props">{{ formatCurrency(props.row.totalNilaiAdjustment) }}</q-td>
            </template>
            <template v-slot:body-cell-totalModal="props">
              <q-td :props="props">{{ formatCurrency(props.row.totalModal) }}</q-td>
            </template>
            <template v-slot:body-cell-net="props">
              <q-td :props="props" :class="(props.row.totalNilaiAdjustment - props.row.totalModal) >= 0 ? 'text-green' : 'text-red'">
                {{ formatCurrency(props.row.totalNilaiAdjustment - props.row.totalModal) }}
              </q-td>
            </template>
          </q-table>
        </q-card-section>
      </q-card>

      <!-- Jasa Sold Table -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="row items-center q-mb-sm">
            <div class="text-subtitle1">{{ $t('pages.summaryPage.jasaSoldTitle') }}</div>
            <q-space />
            <q-input v-model="jasaSearch" :placeholder="$t('pages.summaryPage.jasaSearchPlaceholder')" dense outlined clearable style="max-width: 250px">
              <template v-slot:prepend><q-icon name="search" /></template>
            </q-input>
          </div>
          <q-table
            :rows="filteredJasaItems"
            :columns="jasaSoldColumns"
            row-key="rowKey"
            flat dense
            :rows-per-page-options="[5, 10, 25, 50]"
            :pagination="jasaSoldPagination"
            @update:pagination="jasaSoldPagination = $event"
          >
            <template v-slot:body-cell-totalNilai="props">
              <q-td :props="props">{{ formatCurrency(props.row.totalNilai) }}</q-td>
            </template>
            <template v-slot:body-cell-totalNilaiAdjustment="props">
              <q-td :props="props" class="text-green">{{ formatCurrency(props.row.totalNilaiAdjustment) }}</q-td>
            </template>
          </q-table>
        </q-card-section>
      </q-card>
    </template>

    <div v-else-if="!loading" class="text-center text-grey q-pa-xl">
      <q-icon name="bar_chart" size="60px" />
      <div class="q-mt-sm">{{ $t('pages.summaryPage.noDataLabel') }}</div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { date } from 'quasar'
import { useI18n } from 'vue-i18n'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, BarElement, LineElement, BarController, LineController,
  PointElement, ArcElement, Title, Tooltip, Legend
} from 'chart.js'
import { Bar, Doughnut, Chart } from 'vue-chartjs'
import { api } from 'boot/axios'
import { useDateFilter } from 'src/composables/useDateFilter'

ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, BarController, LineController, PointElement, ArcElement, Title, Tooltip, Legend)

const { t } = useI18n()

// ── Date filter — default: first day of current month → today ─────────────
const todayVal = new Date()
const firstOfMonth = new Date(todayVal.getFullYear(), todayVal.getMonth(), 1)
const { dateRange, dateRangeText, clearDateRange } = useDateFilter('summary', {
  from: date.formatDate(firstOfMonth, 'YYYY/MM/DD'),
  to: date.formatDate(todayVal, 'YYYY/MM/DD')
})

// ── Pembelian status filter ───────────────────────────────────────────────
const filterStatusPembelian = ref(null)
const statusPembelianOptions = computed(() => [
  { label: t('pages.summaryPage.lunasOption'), value: 'LUNAS' },
  { label: t('pages.summaryPage.belumLunasOption'), value: 'BELUM_LUNAS' },
  { label: t('pages.summaryPage.dpOption'), value: 'DP' }
])

// ── Data ─────────────────────────────────────────────────────────────────
const loading = ref(false)
const summary = ref(null)

const mekanikSearch = ref('')
const itemSearch = ref('')
const jasaSearch = ref('')
const mekanikDailyPagination = ref({ rowsPerPage: 10, page: 1 })
const soldItemsPagination = ref({ rowsPerPage: 10, page: 1 })
const jasaSoldPagination = ref({ rowsPerPage: 10, page: 1 })

// ── Fetch ─────────────────────────────────────────────────────────────────
async function fetchSummary() {
  if (!dateRange.value?.from) return
  loading.value = true
  try {
    const params = {
      startDate: dateRange.value.from.replace(/\//g, '-'),
      endDate: (dateRange.value.to || dateRange.value.from).replace(/\//g, '-')
    }
    if (filterStatusPembelian.value?.length) {
      params.statusPembelianFilter = filterStatusPembelian.value.join(',')
    }
    const res = await api.get('/api/pazaauto/summary', { params })
    summary.value = res.data?.data ?? res.data
  } finally {
    loading.value = false
  }
}

watch(dateRange, fetchSummary, { deep: true })
watch(filterStatusPembelian, fetchSummary)
onMounted(fetchSummary)

// ── Helpers ───────────────────────────────────────────────────────────────
function formatCurrency(val) {
  const num = Number(val) || 0
  return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(num)
}

const netProfitColor = computed(() => {
  if (!summary.value) return ''
  return Number(summary.value.netProfit) >= 0 ? 'bg-green-1' : 'bg-red-1'
})

const CHART_COLORS = ['#42A5F5','#66BB6A','#FFA726','#EF5350','#AB47BC','#26C6DA','#D4E157','#FF7043','#8D6E63','#78909C']

// ── Trend Chart ───────────────────────────────────────────────────────────
const trendChartData = computed(() => {
  const rows = [...(summary.value?.dailyBreakdown ?? [])].reverse()
  return {
    labels: rows.map(r => r.date),
    datasets: [
      {
        label: t('pages.summaryPage.pemasukanColumn'),
        data: rows.map(r => Number(r.income)),
        backgroundColor: 'rgba(66,165,245,0.7)',
        borderColor: '#42A5F5',
        type: 'bar'
      },
      {
        label: t('pages.summaryPage.pengeluaranColumn'),
        data: rows.map(r => Number(r.outcome)),
        backgroundColor: 'rgba(239,83,80,0.7)',
        borderColor: '#EF5350',
        type: 'bar'
      },
      {
        label: t('pages.summaryPage.itemTerjualLabel'),
        data: rows.map(r => r.itemsTerjual),
        borderColor: '#FFA726',
        backgroundColor: 'transparent',
        type: 'line',
        yAxisID: 'y2',
        tension: 0.3,
        pointRadius: 4
      }
    ]
  }
})

const trendChartOptions = {
  responsive: true,
  plugins: { legend: { position: 'top' } },
  scales: {
    y: { beginAtZero: true, title: { display: true, text: t('pages.summaryPage.rupiahAxisTitle') } },
    y2: { beginAtZero: true, position: 'right', title: { display: true, text: t('pages.summaryPage.itemColumn') }, grid: { drawOnChartArea: false } }
  }
}

// ── Top Items Chart ───────────────────────────────────────────────────────
const topItemsChartData = computed(() => {
  const rows = summary.value?.topItems ?? []
  return {
    labels: rows.map(r => r.namaBarang),
    datasets: [{
      label: t('pages.summaryPage.qtyTerjualLabel'),
      data: rows.map(r => r.totalQty),
      backgroundColor: CHART_COLORS.slice(0, rows.length)
    }]
  }
})

const topItemsChartOptions = {
  indexAxis: 'y',
  responsive: true,
  plugins: { legend: { display: false } },
  scales: { x: { beginAtZero: true } }
}

// ── Donut Charts ──────────────────────────────────────────────────────────
const incomeMethodChartData = computed(() => {
  const rows = summary.value?.incomeByMethod ?? []
  return {
    labels: rows.map(r => r.label),
    datasets: [{ data: rows.map(r => Number(r.amount)), backgroundColor: CHART_COLORS }]
  }
})

const outcomeTypeChartData = computed(() => {
  const rows = summary.value?.outcomeByType ?? []
  return {
    labels: rows.map(r => r.label),
    datasets: [{ data: rows.map(r => Number(r.amount)), backgroundColor: CHART_COLORS }]
  }
})

const donutOptions = {
  responsive: true,
  plugins: {
    legend: { position: 'bottom' },
    tooltip: {
      callbacks: {
        label: ctx => `${ctx.label}: ${new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(ctx.parsed)}`
      }
    }
  }
}

// ── Mechanic Chart ────────────────────────────────────────────────────────
const mekanikChartData = computed(() => {
  const breakdown = summary.value?.mekanikBreakdown ?? []
  const dates = [...new Set(breakdown.map(r => r.date))].sort()
  const mechanics = [...new Set(breakdown.map(r => r.namaMekanik))]

  return {
    labels: dates,
    datasets: mechanics.map((mek, i) => ({
      label: mek,
      data: dates.map(d => {
        const found = breakdown.find(r => r.date === d && r.namaMekanik === mek)
        return found ? found.totalCustomers : 0
      }),
      backgroundColor: CHART_COLORS[i % CHART_COLORS.length]
    }))
  }
})

const mekanikChartOptions = {
  responsive: true,
  plugins: { legend: { position: 'top' } },
  scales: { x: { stacked: false }, y: { beginAtZero: true, title: { display: true, text: t('pages.summaryPage.pelangganColumn') } } }
}

// ── Filtered tables ───────────────────────────────────────────────────────
const filteredMekanikDaily = computed(() => {
  const rows = (summary.value?.mekanikBreakdown ?? []).map((r, i) => ({ ...r, rowKey: i }))
  if (!mekanikSearch.value) return rows
  const q = mekanikSearch.value.toLowerCase()
  return rows.filter(r => r.namaMekanik?.toLowerCase().includes(q) || r.date?.includes(q))
})

const filteredSoldItems = computed(() => {
  const rows = (summary.value?.soldItemsBreakdown ?? []).map((r, i) => ({ ...r, rowKey: i }))
  if (!itemSearch.value) return rows
  const q = itemSearch.value.toLowerCase()
  return rows.filter(r => r.namaBarang?.toLowerCase().includes(q) || r.date?.includes(q))
})

const filteredJasaItems = computed(() => {
  const rows = (summary.value?.jasaSummaryBreakdown ?? []).map((r, i) => ({ ...r, rowKey: i }))
  if (!jasaSearch.value) return rows
  const q = jasaSearch.value.toLowerCase()
  return rows.filter(r => r.namaJasa?.toLowerCase().includes(q) || r.date?.includes(q))
})

// ── Table columns ─────────────────────────────────────────────────────────
const dailyColumns = computed(() => [
  { name: 'date', label: t('pages.summaryPage.tanggalColumn'), field: 'date', align: 'left', sortable: true },
  { name: 'customers', label: t('pages.summaryPage.pelangganColumn'), field: 'customers', align: 'center', sortable: true },
  { name: 'income', label: t('pages.summaryPage.pemasukanColumn'), field: 'income', align: 'right', sortable: true },
  { name: 'outcome', label: t('pages.summaryPage.pengeluaranColumn'), field: 'outcome', align: 'right', sortable: true },
  { name: 'itemsTerjual', label: t('pages.summaryPage.itemColumn'), field: 'itemsTerjual', align: 'center', sortable: true },
  { name: 'net', label: t('pages.summaryPage.grossColumn'), field: 'net', align: 'right', sortable: true }
])

const mekanikSummaryColumns = computed(() => [
  { name: 'namaMekanik', label: t('pages.summaryPage.mekanikColumn'), field: 'namaMekanik', align: 'left', sortable: true },
  { name: 'totalCustomers', label: t('pages.summaryPage.totalPelangganLabel'), field: 'totalCustomers', align: 'center', sortable: true },
  { name: 'totalHari', label: t('pages.summaryPage.hariKerjaColumn'), field: 'totalHari', align: 'center', sortable: true },
  { name: 'rataPerHari', label: t('pages.summaryPage.rataHariColumn'), field: 'rataPerHari', align: 'center', sortable: true }
])

const mekanikDailyColumns = computed(() => [
  { name: 'date', label: t('pages.summaryPage.tanggalColumn'), field: 'date', align: 'left', sortable: true },
  { name: 'namaMekanik', label: t('pages.summaryPage.mekanikColumn'), field: 'namaMekanik', align: 'left', sortable: true },
  { name: 'totalCustomers', label: t('pages.summaryPage.jumlahPelangganColumn'), field: 'totalCustomers', align: 'center', sortable: true }
])

const soldItemsColumns = computed(() => [
  { name: 'date', label: t('pages.summaryPage.tanggalColumn'), field: 'date', align: 'left', sortable: true },
  { name: 'namaBarang', label: t('pages.summaryPage.namaBarangColumn'), field: 'namaBarang', align: 'left', sortable: true },
  { name: 'totalQty', label: t('pages.summaryPage.qtyColumn'), field: 'totalQty', align: 'center', sortable: true },
  { name: 'totalValue', label: t('pages.summaryPage.totalNilaiColumn'), field: 'totalValue', align: 'right', sortable: true },
  { name: 'totalNilaiAdjustment', label: t('pages.summaryPage.totalNilaiAdjustmentColumn'), field: 'totalNilaiAdjustment', align: 'right', sortable: true },
  { name: 'totalModal', label: t('pages.summaryPage.totalModalColumn'), field: 'totalModal', align: 'right', sortable: true },
  { name: 'net', label: t('pages.summaryPage.netColumn'), field: r => r.totalNilaiAdjustment - r.totalModal, align: 'right', sortable: true }
])

const jasaSoldColumns = computed(() => [
  { name: 'date', label: t('pages.summaryPage.tanggalColumn'), field: 'date', align: 'left', sortable: true },
  { name: 'namaJasa', label: t('pages.summaryPage.namaJasaColumn'), field: 'namaJasa', align: 'left', sortable: true },
  { name: 'totalQty', label: t('pages.summaryPage.qtyColumn'), field: 'totalQty', align: 'center', sortable: true },
  { name: 'totalNilai', label: t('pages.summaryPage.totalNilaiColumn'), field: 'totalNilai', align: 'right', sortable: true },
  { name: 'totalNilaiAdjustment', label: t('pages.summaryPage.netColumn'), field: 'totalNilaiAdjustment', align: 'right', sortable: true },
])
</script>
