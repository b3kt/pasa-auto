<template>
    <q-page padding>
        <q-splitter v-model="splitterModel" :limits="[25, 75]" :disable="$q.screen.xs"
            style="height: calc(100vh - 100px)">
            <!-- Left: Clock-In/Out form -->
            <template v-slot:before>
                <div class="q-pa-md scroll" style="height: 100%">
                    <q-card>
                        <q-card-section class="bg-primary text-white">
                            <div class="text-h5">{{ $t('pages.absensi.header') }}</div>
                            <div class="text-subtitle2">{{ currentDate }}</div>
                        </q-card-section>

                        <q-separator />

                        <q-card-section>
                            <q-card flat bordered class="text-center q-pa-md q-mb-md">
                                <div class="text-h3 text-primary">{{ currentTime }}</div>
                                <div class="text-subtitle2 text-grey-7">{{ $t('pages.absensi.currentTime') }}</div>
                            </q-card>

                            <div v-if="!role.includes('Karyawan')">
                                <q-select v-model="selectedKaryawan"
                                    :label="$t('pages.absensi.selectEmployeeLabel')" outlined dense clearable
                                    :options="filteredKaryawanOptions"
                                    option-label="namaKaryawan" option-value="id" emit-value map-options use-input
                                    input-debounce="300" @filter="filterKaryawan" :loading="loadingKaryawan">
                                    <template v-slot:no-option>
                                        <q-item>
                                            <q-item-section class="text-grey">
                                                {{ $t('pages.absensi.noResults') }}
                                            </q-item-section>
                                        </q-item>
                                    </template>
                                </q-select>
                                <div class="text-caption text-grey-7 q-mt-xs">
                                    {{ $t('pages.absensi.allStaffHint') }}
                                </div>
                            </div>
                            <div v-else>
                                <q-input :model-value="user?.karyawanNama" :label="$t('pages.absensi.employeeNameLabel')" outlined dense readonly />
                                <q-input style="display: none;" v-model="selectedKaryawan" :label="$t('pages.absensi.employeeIdLabel')" outlined
                                    dense readonly />
                            </div>

                            <!-- Today's Status Card -->
                            <q-card v-if="todayAttendance" flat bordered class="q-mt-md">
                                <q-card-section>
                                    <div class="text-subtitle1 text-weight-bold">{{ $t('pages.absensi.todayStatusTitle') }}</div>
                                    <div class="row q-col-gutter-sm q-mt-sm">
                                        <div class="col-6">
                                            <div class="text-caption text-grey-7">{{ $t('pages.absensi.clockInLabel') }}</div>
                                            <div class="text-body1">{{ todayAttendance.jamMasuk || '-' }}</div>
                                        </div>
                                        <div class="col-6">
                                            <div class="text-caption text-grey-7">{{ $t('pages.absensi.clockOutLabel') }}</div>
                                            <div class="text-body1">{{ todayAttendance.jamKeluar || '-' }}</div>
                                        </div>
                                        <div class="col-6 q-mt-sm">
                                            <div class="text-caption text-grey-7">Status</div>
                                            <q-badge :color="getStatusColor(todayAttendance.status)">
                                                {{ todayAttendance.status }}
                                            </q-badge>
                                        </div>
                                        <div class="col-6 q-mt-sm">
                                            <div class="text-caption text-grey-7">{{ $t('pages.absensi.flagsLabel') }}</div>
                                            <div>
                                                <q-badge v-if="todayAttendance.terlambat" color="warning"
                                                    class="q-mr-xs">{{ $t('pages.absensi.late') }}</q-badge>
                                                <q-badge v-if="todayAttendance.pulangCepat" color="orange"
                                                    class="q-mr-xs">{{ $t('pages.absensi.earlyLeave') }}</q-badge>
                                                <q-badge v-if="todayAttendance.lembur > 0" color="green">{{
                                                    $t('pages.absensi.overtime', { min: todayAttendance.lembur })
                                                }}</q-badge>
                                            </div>
                                        </div>
                                    </div>
                                </q-card-section>
                            </q-card>

                            <!-- Optional note for clock-out; only relevant once clocked in and not yet clocked out -->
                            <q-input v-if="canClockOut" v-model="clockOutNote" :label="$t('pages.absensi.noteLabel')" outlined
                                dense type="textarea" rows="2" class="q-mt-md" />

                            <!-- Clock In/Out Buttons -->
                            <div class="row q-col-gutter-md q-mt-md">
                                <div class="col-6">
                                    <q-btn unelevated color="positive" icon="login" :label="$t('pages.absensi.clockInButton')" class="full-width"
                                        @click="clockIn" :loading="clocking"
                                        :disable="!selectedKaryawan || !!todayAttendance?.jamMasuk" />
                                </div>
                                <div class="col-6">
                                    <q-btn unelevated color="negative" icon="logout" :label="$t('pages.absensi.clockOutButton')"
                                        class="full-width" @click="confirmClockOut" :loading="clocking"
                                        :disable="!selectedKaryawan || !canClockOut" />
                                </div>
                            </div>
                        </q-card-section>
                    </q-card>
                </div>
            </template>

            <!-- Right: Attendance history table (hidden on mobile) -->
            <template v-slot:after >
                <div class="q-pa-md xs-hide gt-sm" style="height: 100%">
                    <GenericTable :rows="historyRows" :columns="historyColumns" row-key="id"
                        :loading="loadingHistory" :pagination="historyPagination"
                        @update:pagination="historyPagination = $event" @request="onHistoryRequest"
                        :enable-search="false">
                        <template v-slot:title>
                            <div class="text-h6 q-mb-md">{{ $t('pages.absensi.historyTitle') }}</div>
                        </template>

                        <template v-slot:toolbar-filters>
                            <div class="row items-center q-gutter-sm">
                                <q-input v-model="filters.startDate" :label="$t('pages.absensi.startDateLabel')" outlined dense type="date"
                                    style="min-width: 150px" />
                                <q-input v-model="filters.endDate" :label="$t('pages.absensi.endDateLabel')" outlined dense type="date"
                                    style="min-width: 150px" />
                                <q-select v-model="filters.status" label="Status" outlined dense clearable
                                    :options="['HADIR', 'IZIN', 'SAKIT', 'ALPHA', 'CUTI']" style="min-width: 150px" />
                                <q-btn unelevated color="primary" :label="$t('pages.absensi.searchButton')" icon="search"
                                    @click="fetchAttendanceHistory" />
                            </div>
                        </template>

                        <template v-slot:body-cell-tanggal="props">
                                {{ formatDate(props.row.tanggal) }}
                        </template>

                        <template v-slot:body-cell-status="props">
                                <q-badge :color="getStatusColor(props.row.status)">
                                    {{ props.row.status }}
                                </q-badge>
                        </template>

                        <template v-slot:body-cell-flags="props">
                                <q-badge v-if="props.row.terlambat" color="warning" class="q-mr-xs">{{ $t('pages.absensi.late') }}</q-badge>
                                <q-badge v-if="props.row.pulangCepat" color="orange" class="q-mr-xs">{{ $t('pages.absensi.earlyLeave') }}</q-badge>
                                <q-badge v-if="props.row.lembur > 0" color="green">{{ $t('pages.absensi.overtime', { min: props.row.lembur }) }}</q-badge>
                        </template>

                        <template v-slot:body-cell-actions="props" v-if="isManager">
                                <q-btn flat dense round icon="edit" color="primary"
                                    @click.stop="openMarkAbsenceDialog(props.row)">
                                    <q-tooltip>{{ $t('pages.absensi.markAbsenceTooltip') }}</q-tooltip>
                                </q-btn>
                        </template>
                    </GenericTable>
                </div>
            </template>
        </q-splitter>

        <!-- Clock-Out Confirmation Dialog -->
        <GenericDialog v-model="showClockOutConfirm" :title="$t('pages.absensi.confirmClockOutTitle')" min-width="400px">
            {{ $t('pages.absensi.confirmClockOutMessage') }}
            <template #actions>
                <q-btn flat :label="$t('cancel')" color="primary" @click="showClockOutConfirm = false" />
                <q-btn :label="$t('pages.absensi.confirmClockOutButton')" color="negative" @click="clockOutConfirmed" :loading="clocking" />
            </template>
        </GenericDialog>

        <!-- Mark Absence Dialog (Admin) -->
        <GenericDialog v-model="showMarkAbsenceDialog" :title="$t('pages.absensi.markAbsenceTitle')" min-width="400px">
            <q-form @submit="markAbsence" id="absence-form" class="q-gutter-md">
                <q-input v-model="absenceForm.tanggal" :label="$t('pages.absensi.dateLabel')" outlined dense type="date"
                    :rules="[val => !!val || $t('pages.absensi.dateRequired')]" />

                <q-select v-model="absenceForm.status" label="Status *" outlined dense
                    :options="['IZIN', 'SAKIT', 'ALPHA', 'CUTI']" :rules="[val => !!val || $t('pages.absensi.statusRequired')]" />

                <q-input v-model="absenceForm.keterangan" :label="$t('pages.absensi.notesLabel')" outlined dense type="textarea" rows="3" />
            </q-form>
            <template #actions>
                <q-btn flat :label="$t('cancel')" color="primary" @click="closeMarkAbsenceDialog" />
                <q-btn :label="$t('save')" type="submit" form="absence-form" color="primary" :loading="marking" />
            </template>
        </GenericDialog>
    </q-page>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { api } from 'boot/axios'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import browserCache from '../../utils/browserCache.js'
import GenericDialog from 'components/GenericDialog.vue'
import GenericTable from 'components/GenericTable.vue'
import { useAuthStore } from 'stores/auth-store'
import { computed } from 'vue'

const $q = useQuasar()
const { t } = useI18n()
const authStore = useAuthStore()
const user = computed(() => authStore.user)
// user briefly turns null while this page is still mounted during logout, right up until the
// router swaps in the login page - roles must not throw on that null instead of just being empty
const role = computed(() => user.value?.roles || [])
// Only Admin/Owner may pick other employees and edit attendance; the server enforces the same rule
const isManager = computed(() => role.value?.includes('Admin') || role.value?.includes('Owner'))

// State
const splitterModel = ref(25)
// The history table is hidden on mobile (see xs-hide below); give the form the full width there
watch(() => $q.screen.xs, (isXs) => {
    splitterModel.value = isXs ? 100 : 25
}, { immediate: true })
const currentTime = ref('')
const currentDate = ref('')
const selectedKaryawan = ref(user.value.karyawanId)
const todayAttendance = ref(null)
const clocking = ref(false)
const loadingKaryawan = ref(false)
const loadingHistory = ref(false)
const marking = ref(false)
const karyawanOptions = ref([])
const filteredKaryawanOptions = ref([])
const historyRows = ref([])
const showMarkAbsenceDialog = ref(false)
const showClockOutConfirm = ref(false)
const clockOutNote = ref('')

// Only relevant once clocked in today and not yet clocked out
const canClockOut = computed(() => !!todayAttendance.value?.jamMasuk && !todayAttendance.value?.jamKeluar)

const _absensiDateCache = browserCache.getItem('date-filter-absensi')
const _today = new Date().toISOString().slice(0, 10)
const filters = ref({
    startDate: _absensiDateCache?.cachedDate === _today ? _absensiDateCache.startDate : null,
    endDate: _absensiDateCache?.cachedDate === _today ? _absensiDateCache.endDate : null,
    status: null
})

watch(
  () => [filters.value.startDate, filters.value.endDate],
  ([startDate, endDate]) => {
    if (startDate || endDate) {
      browserCache.setItem('date-filter-absensi', { startDate, endDate, cachedDate: _today }, 24 * 60 * 60 * 1000)
    } else {
      browserCache.removeItem('date-filter-absensi')
    }
  }
)

const historyPagination = ref({
    page: 1,
    rowsPerPage: 10,
    rowsNumber: 0,
    sortBy: 'tanggal',
    descending: true
})

const absenceForm = ref({
    tanggal: null,
    status: null,
    keterangan: ''
})

// Columns; Admin/Owner get a "Karyawan" column since they can view every employee's history at once
const historyColumns = computed(() => [
    { name: 'tanggal', label: t('pages.absensi.dateColumn'), align: 'left', field: 'tanggal', sortable: true },
    ...(isManager.value ? [{ name: 'namaKaryawan', label: t('pages.absensi.employeeColumn'), align: 'left', field: 'namaKaryawan' }] : []),
    { name: 'jamMasuk', label: t('pages.absensi.clockInLabel'), align: 'center', field: 'jamMasuk' },
    { name: 'jamKeluar', label: t('pages.absensi.clockOutLabel'), align: 'center', field: 'jamKeluar' },
    { name: 'status', label: 'Status', align: 'center', field: 'status', sortable: true },
    { name: 'flags', label: t('pages.absensi.flagsLabel'), align: 'center', field: 'flags' },
    { name: 'keterangan', label: t('pages.absensi.notesColumn'), align: 'left', field: 'keterangan' },
    { name: 'actions', label: t('pages.absensi.actionsColumn'), align: 'center', field: 'actions' }
])

// Clock interval
let clockInterval = null

// Methods
const updateClock = () => {
    const now = new Date()
    currentTime.value = now.toLocaleTimeString('id-ID')
    currentDate.value = now.toLocaleDateString('id-ID', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
}

const fetchKaryawan = async () => {
    loadingKaryawan.value = true
    try {
        const response = await api.get('/api/pazaauto/karyawan')
        if (response.data.success) {
            karyawanOptions.value = response.data.data || []
            filteredKaryawanOptions.value = karyawanOptions.value
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: t('pages.absensi.fetchEmployeesFailed'),
            caption: error.response?.data?.message || error.message
        })
    } finally {
        loadingKaryawan.value = false
    }
}

const filterKaryawan = (val, update) => {
    update(() => {
        if (val === '') {
            filteredKaryawanOptions.value = karyawanOptions.value
        } else {
            const needle = val.toLowerCase()
            filteredKaryawanOptions.value = karyawanOptions.value.filter(
                v => v.namaKaryawan.toLowerCase().indexOf(needle) > -1
            )
        }
    })
}

const fetchTodayAttendance = async () => {
    if (!selectedKaryawan.value) return

    try {
        const response = await api.get(`/api/pazaauto/absensi/today/${selectedKaryawan.value}`)
        if (response.data.success) {
            todayAttendance.value = response.data.data
        }
    } catch (error) {
        todayAttendance.value = null
        console.log(error)
    }
}

const clockIn = async () => {
    clocking.value = true
    try {
        const response = await api.post('/api/pazaauto/absensi/clock-in', {
            karyawanId: selectedKaryawan.value,
            location: 'Office', // Can be enhanced with geolocation
            deviceInfo: navigator.userAgent
        })

        if (response.data.success) {
            $q.notify({
                type: 'positive',
                message: t('pages.absensi.clockInSuccess'),
                icon: 'check_circle'
            })
            await fetchTodayAttendance()
            await fetchAttendanceHistory()
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: t('pages.absensi.clockInFailed'),
            caption: error.response?.data?.message || error.message
        })
    } finally {
        clocking.value = false
    }
}

const confirmClockOut = () => {
    showClockOutConfirm.value = true
}

const clockOutConfirmed = async () => {
    showClockOutConfirm.value = false
    await clockOut()
}

const clockOut = async () => {
    clocking.value = true
    try {
        const response = await api.post('/api/pazaauto/absensi/clock-out', {
            karyawanId: selectedKaryawan.value,
            location: 'Office', // Can be enhanced with geolocation
            keterangan: clockOutNote.value || null
        })

        if (response.data.success) {
            $q.notify({
                type: 'positive',
                message: t('pages.absensi.clockOutSuccess'),
                icon: 'check_circle'
            })
            clockOutNote.value = ''
            await fetchTodayAttendance()
            await fetchAttendanceHistory()
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: t('pages.absensi.clockOutFailed'),
            caption: error.response?.data?.message || error.message
        })
    } finally {
        clocking.value = false
    }
}

const fetchAttendanceHistory = async () => {
    loadingHistory.value = true
    try {
        const params = {
            karyawanId: selectedKaryawan.value,
            startDate: filters.value.startDate,
            endDate: filters.value.endDate,
            status: filters.value.status,
            page: historyPagination.value.page,
            rowsPerPage: historyPagination.value.rowsPerPage,
            sortBy: historyPagination.value.sortBy,
            descending: historyPagination.value.descending
        }

        const response = await api.get('/api/pazaauto/absensi/history', { params })
        if (response.data.success) {
            const pageData = response.data.data
            historyRows.value = pageData.rows || []
            historyPagination.value.rowsNumber = pageData.rowsNumber || 0
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: t('pages.absensi.fetchHistoryFailed'),
            caption: error.response?.data?.message || error.message
        })
    } finally {
        loadingHistory.value = false
    }
}

const onHistoryRequest = (props) => {
    historyPagination.value = props.pagination
    fetchAttendanceHistory()
}

const openMarkAbsenceDialog = (row) => {
    absenceForm.value = {
        tanggal: row.tanggal,
        status: row.status,
        keterangan: row.keterangan || ''
    }
    showMarkAbsenceDialog.value = true
}

const closeMarkAbsenceDialog = () => {
    showMarkAbsenceDialog.value = false
    absenceForm.value = {
        tanggal: null,
        status: null,
        keterangan: ''
    }
}

const markAbsence = async () => {
    marking.value = true
    try {
        const response = await api.post('/api/pazaauto/absensi/mark-absence', {
            karyawanId: selectedKaryawan.value,
            tanggal: absenceForm.value.tanggal,
            status: absenceForm.value.status,
            keterangan: absenceForm.value.keterangan
        })

        if (response.data.success) {
            $q.notify({
                type: 'positive',
                message: t('pages.absensi.markAbsenceSuccess')
            })
            closeMarkAbsenceDialog()
            await fetchTodayAttendance()
            await fetchAttendanceHistory()
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: t('pages.absensi.markAbsenceFailed'),
            caption: error.response?.data?.message || error.message
        })
    } finally {
        marking.value = false
    }
}

const getStatusColor = (status) => {
    const colors = {
        'HADIR': 'positive',
        'IZIN': 'info',
        'SAKIT': 'warning',
        'ALPHA': 'negative',
        'CUTI': 'purple'
    }
    return colors[status] || 'grey'
}

const formatDate = (date) => {
    if (!date) return '-'
    return new Date(date).toLocaleDateString('id-ID')
}

// Watchers
const watchSelectedKaryawan = () => {
    clockOutNote.value = ''
    if (selectedKaryawan.value) {
        fetchTodayAttendance()
    } else {
        // Cleared back to "all staff" (Admin/Owner only) - there's no single employee to show a status for
        todayAttendance.value = null
    }
    // Karyawan omitted means "all staff" for Admin/Owner; refetch either way so a cleared selection
    // actually broadens the history back out instead of leaving it stuck on the last employee.
    historyPagination.value.page = 1
    fetchAttendanceHistory()
}

// Lifecycle
onMounted(() => {
    updateClock()
    clockInterval = setInterval(updateClock, 1000)
    if (isManager.value) {
        fetchKaryawan()
    } else {
        fetchTodayAttendance();
    }
    // Load the history table right away, using the default pagination/filters, instead of
    // waiting for the employee selector to change.
    fetchAttendanceHistory()
})

onBeforeUnmount(() => {
    if (clockInterval) {
        clearInterval(clockInterval)
    }
})

// Watch selectedKaryawan
watch(selectedKaryawan, watchSelectedKaryawan)
</script>

<style lang="sass" scoped>
</style>
