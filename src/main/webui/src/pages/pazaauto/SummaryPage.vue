<template>
  <q-page padding>
    <!-- Header & Filters -->
    <div class="row items-center q-mb-md q-gutter-sm">
      <div class="text-h6 col-auto">Ringkasan Laporan</div>
      <q-space />

      <!-- Pembelian status filter -->
      <q-select
        v-model="filterStatusPembelian"
        :options="statusPembelianOptions"
        label="Status Pembelian"
        outlined dense clearable multiple
        style="min-width: 200px"
        emit-value map-options
      />

      <!-- Date range -->
      <q-input :model-value="dateRangeText" label="Periode" outlined dense readonly style="min-width: 220px">
        <template v-slot:append>
          <q-icon name="event" class="cursor-pointer">
            <q-popup-proxy cover transition-show="scale" transition-hide="scale">
              <q-date v-model="dateRange" range>
                <div class="row items-center justify-end q-gutter-sm">
                  <q-btn label="Clear" color="primary" flat @click="clearDateRange" v-close-popup />
                  <q-btn label="Close" color="primary" flat v-close-popup />
                </div>
              </q-date>
            </q-popup-proxy>
          </q-icon>
        </template>
      </q-input>

      <q-btn icon="refresh" color="primary" dense flat :loading="loading" @click="fetchSummary">
        <q-tooltip>Refresh</q-tooltip>
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
                <div class="text-caption text-grey">Total Pelanggan</div>
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
                <div class="text-caption text-grey">Total Pemasukan</div>
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
                <div class="text-caption text-grey">Total Pengeluaran</div>
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
                <div class="text-caption text-grey">Item Terjual</div>
                <div class="text-h5 text-weight-bold">{{ summary.totalItemTerjual }} pcs</div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Net Profit Banner -->
      <q-card flat bordered class="q-mb-md" :class="netProfitColor">
        <q-card-section class="row items-center">
          <q-icon :name="summary.netProfit >= 0 ? 'arrow_upward' : 'arrow_downward'" size="28px" class="q-mr-sm" />
          <span class="text-subtitle1 text-weight-bold">Pemasukan Bruto: {{ formatCurrency(summary.netProfit) }}</span>
        </q-card-section>
      </q-card>

      <!-- Trend Chart -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-subtitle1 q-mb-sm">Tren Harian</div>
          <Bar :data="trendChartData" :options="trendChartOptions" style="max-height: 300px" />
        </q-card-section>
      </q-card>

      <!-- Top Items + Donut Charts -->
      <div class="row q-col-gutter-md q-mb-md">
        <div class="col-12 col-md-4">
          <q-card flat bordered style="height: 100%">
            <q-card-section>
              <div class="text-subtitle1 q-mb-sm">Top 10 Barang Terjual</div>
              <Bar :data="topItemsChartData" :options="topItemsChartOptions" style="max-height: 280px" />
            </q-card-section>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-4">
          <q-card flat bordered style="height: 100%">
            <q-card-section>
              <div class="text-subtitle1 q-mb-sm">Pemasukan per Metode</div>
              <Doughnut :data="incomeMethodChartData" :options="donutOptions" style="max-height: 280px" />
            </q-card-section>
          </q-card>
        </div>
        <div class="col-12 col-sm-6 col-md-4">
          <q-card flat bordered style="height: 100%">
            <q-card-section>
              <div class="text-subtitle1 q-mb-sm">Pengeluaran per Jenis</div>
              <Doughnut :data="outcomeTypeChartData" :options="donutOptions" style="max-height: 280px" />
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Mechanic Chart -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-subtitle1 q-mb-sm">Performa Mekanik per Hari</div>
          <Bar :data="mekanikChartData" :options="mekanikChartOptions" style="max-height: 300px" />
        </q-card-section>
      </q-card>

      <!-- Mechanic Summary Table -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-subtitle1 q-mb-sm">Ringkasan Mekanik</div>
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
          <div class="text-subtitle1 q-mb-sm">Detail Per Hari</div>
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
            <div class="text-subtitle1">Detail Mekanik per Hari</div>
            <q-space />
            <q-input v-model="mekanikSearch" placeholder="Cari mekanik..." dense outlined clearable style="max-width: 250px">
              <template v-slot:prepend><q-icon name="search" /></template>
            </q-input>
          </div>
          <q-table
            :rows="filteredMekanikDaily"
            :columns="mekanikDailyColumns"
            row-key="rowKey"
            flat dense
            :pagination="mekanikDailyPagination"
            @update:pagination="mekanikDailyPagination = $event"
          />
        </q-card-section>
      </q-card>

      <!-- Sold Items Table -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="row items-center q-mb-sm">
            <div class="text-subtitle1">Detail Item Terjual per Hari</div>
            <q-space />
            <q-input v-model="itemSearch" placeholder="Cari barang..." dense outlined clearable style="max-width: 250px">
              <template v-slot:prepend><q-icon name="search" /></template>
            </q-input>
          </div>
          <q-table
            :rows="filteredSoldItems"
            :columns="soldItemsColumns"
            row-key="rowKey"
            flat dense
            :pagination="soldItemsPagination"
            @update:pagination="soldItemsPagination = $event"
          >
            <template v-slot:body-cell-totalValue="props">
              <q-td :props="props">{{ formatCurrency(props.row.totalValue) }}</q-td>
            </template>
          </q-table>
        </q-card-section>
      </q-card>
    </template>

    <div v-else-if="!loading" class="text-center text-grey q-pa-xl">
      <q-icon name="bar_chart" size="60px" />
      <div class="q-mt-sm">Pilih periode dan klik Refresh untuk melihat ringkasan.</div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { date } from 'quasar'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, BarElement, LineElement, BarController, LineController,
  PointElement, ArcElement, Title, Tooltip, Legend
} from 'chart.js'
import { Bar, Doughnut, Line } from 'vue-chartjs'
import { api } from 'boot/axios'
import { useDateFilter } from 'src/composables/useDateFilter'

ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, BarController, LineController, PointElement, ArcElement, Title, Tooltip, Legend)

// ── Date filter — default: first day of current month → today ─────────────
const todayVal = new Date()
const firstOfMonth = new Date(todayVal.getFullYear(), todayVal.getMonth(), 1)
const { dateRange, dateRangeText, clearDateRange } = useDateFilter('summary', {
  from: date.formatDate(firstOfMonth, 'YYYY/MM/DD'),
  to: date.formatDate(todayVal, 'YYYY/MM/DD')
})

// ── Pembelian status filter ───────────────────────────────────────────────
const filterStatusPembelian = ref(null)
const statusPembelianOptions = [
  { label: 'Lunas', value: 'LUNAS' },
  { label: 'Belum Lunas', value: 'BELUM_LUNAS' },
  { label: 'DP', value: 'DP' }
]

// ── Data ─────────────────────────────────────────────────────────────────
const loading = ref(false)
const summary = ref(null)

const mekanikSearch = ref('')
const itemSearch = ref('')
const mekanikDailyPagination = ref({ rowsPerPage: 10, page: 1 })
const soldItemsPagination = ref({ rowsPerPage: 10, page: 1 })

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
        label: 'Pemasukan',
        data: rows.map(r => Number(r.income)),
        backgroundColor: 'rgba(66,165,245,0.7)',
        borderColor: '#42A5F5',
        type: 'bar'
      },
      {
        label: 'Pengeluaran',
        data: rows.map(r => Number(r.outcome)),
        backgroundColor: 'rgba(239,83,80,0.7)',
        borderColor: '#EF5350',
        type: 'bar'
      },
      {
        label: 'Item Terjual',
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
    y: { beginAtZero: true, title: { display: true, text: 'Rupiah' } },
    y2: { beginAtZero: true, position: 'right', title: { display: true, text: 'Item' }, grid: { drawOnChartArea: false } }
  }
}

// ── Top Items Chart ───────────────────────────────────────────────────────
const topItemsChartData = computed(() => {
  const rows = summary.value?.topItems ?? []
  return {
    labels: rows.map(r => r.namaBarang),
    datasets: [{
      label: 'Qty Terjual',
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
  scales: { x: { stacked: false }, y: { beginAtZero: true, title: { display: true, text: 'Pelanggan' } } }
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

// ── Table columns ─────────────────────────────────────────────────────────
const dailyColumns = [
  { name: 'date', label: 'Tanggal', field: 'date', align: 'left', sortable: true },
  { name: 'customers', label: 'Pelanggan', field: 'customers', align: 'center', sortable: true },
  { name: 'income', label: 'Pemasukan', field: 'income', align: 'right', sortable: true },
  { name: 'outcome', label: 'Pengeluaran', field: 'outcome', align: 'right', sortable: true },
  { name: 'itemsTerjual', label: 'Item', field: 'itemsTerjual', align: 'center', sortable: true },
  { name: 'net', label: 'Net', field: 'net', align: 'right', sortable: true }
]

const mekanikSummaryColumns = [
  { name: 'namaMekanik', label: 'Mekanik', field: 'namaMekanik', align: 'left', sortable: true },
  { name: 'totalCustomers', label: 'Total Pelanggan', field: 'totalCustomers', align: 'center', sortable: true },
  { name: 'totalHari', label: 'Hari Kerja', field: 'totalHari', align: 'center', sortable: true },
  { name: 'rataPerHari', label: 'Rata/Hari', field: 'rataPerHari', align: 'center', sortable: true }
]

const mekanikDailyColumns = [
  { name: 'date', label: 'Tanggal', field: 'date', align: 'left', sortable: true },
  { name: 'namaMekanik', label: 'Mekanik', field: 'namaMekanik', align: 'left', sortable: true },
  { name: 'totalCustomers', label: 'Jumlah Pelanggan', field: 'totalCustomers', align: 'center', sortable: true }
]

const soldItemsColumns = [
  { name: 'date', label: 'Tanggal', field: 'date', align: 'left', sortable: true },
  { name: 'namaBarang', label: 'Nama Barang', field: 'namaBarang', align: 'left', sortable: true },
  { name: 'totalQty', label: 'Qty', field: 'totalQty', align: 'center', sortable: true },
  { name: 'totalValue', label: 'Total Nilai', field: 'totalValue', align: 'right', sortable: true }
]
</script>
