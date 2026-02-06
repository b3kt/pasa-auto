<template>
    <q-page padding>
        <div class="q-pa-md">
            <!-- Clock-In/Out Section -->
            <q-card class="q-mb-lg">
                <q-card-section class="bg-primary text-white">
                    <div class="text-h5">Employee Attendance</div>
                    <div class="text-subtitle2">{{ currentDate }}</div>
                </q-card-section>

                <q-separator />

                <q-card-section>
                    <div class="row q-col-gutter-md">
                        <!-- Current Time Display -->
                        <div class="col-12 col-md-4">
                            <q-card flat bordered class="text-center q-pa-md">
                                <div class="text-h3 text-primary">{{ currentTime }}</div>
                                <div class="text-subtitle2 text-grey-7">Current Time</div>
                            </q-card>
                        </div>

                        <!-- Employee Selector -->
                        <div class="col-12 col-md-8">

                            <q-select v-if="!role.includes('Karyawan')" v-model="selectedKaryawan"
                                label="Select Employee *" outlined dense :options="filteredKaryawanOptions"
                                option-label="namaKaryawan" option-value="id" emit-value map-options use-input
                                input-debounce="300" @filter="filterKaryawan" :loading="loadingKaryawan">
                                <template v-slot:no-option>
                                    <q-item>
                                        <q-item-section class="text-grey">
                                            No results
                                        </q-item-section>
                                    </q-item>
                                </template>
                            </q-select>
                            <div v-else>
                                <q-input v-model="user.karyawanNama" label="Employee Name" outlined dense readonly />
                                <q-input style="display: none;" v-model="selectedKaryawan" label="Employee ID" outlined
                                    dense readonly />
                            </div>


                            <!-- Today's Status Card -->
                            <q-card v-if="todayAttendance" flat bordered class="q-mt-md">
                                <q-card-section>
                                    <div class="text-subtitle1 text-weight-bold">Today's Status</div>
                                    <div class="row q-col-gutter-sm q-mt-sm">
                                        <div class="col-6">
                                            <div class="text-caption text-grey-7">Clock In</div>
                                            <div class="text-body1">{{ todayAttendance.jamMasuk || '-' }}</div>
                                        </div>
                                        <div class="col-6">
                                            <div class="text-caption text-grey-7">Clock Out</div>
                                            <div class="text-body1">{{ todayAttendance.jamKeluar || '-' }}</div>
                                        </div>
                                        <div class="col-6 q-mt-sm">
                                            <div class="text-caption text-grey-7">Status</div>
                                            <q-badge :color="getStatusColor(todayAttendance.status)">
                                                {{ todayAttendance.status }}
                                            </q-badge>
                                        </div>
                                        <div class="col-6 q-mt-sm">
                                            <div class="text-caption text-grey-7">Flags</div>
                                            <div>
                                                <q-badge v-if="todayAttendance.terlambat" color="warning"
                                                    class="q-mr-xs">Late</q-badge>
                                                <q-badge v-if="todayAttendance.pulangCepat" color="orange"
                                                    class="q-mr-xs">Early</q-badge>
                                                <q-badge v-if="todayAttendance.lembur > 0" color="green">OT: {{
                                                    todayAttendance.lembur }}m
                                                </q-badge>
                                            </div>
                                        </div>
                                    </div>
                                </q-card-section>
                            </q-card>

                            <!-- Clock In/Out Buttons -->
                            <div class="row q-col-gutter-md q-mt-md">
                                <div class="col-6">
                                    <q-btn unelevated color="positive" icon="login" label="Clock In" class="full-width"
                                        @click="clockIn" :loading="clocking"
                                        :disable="!selectedKaryawan || (todayAttendance && todayAttendance.jamMasuk)" />
                                </div>
                                <div class="col-6">
                                    <q-btn unelevated color="negative" icon="logout" label="Clock Out"
                                        class="full-width" @click="clockOut" :loading="clocking"
                                        :disable="!selectedKaryawan || !todayAttendance || !todayAttendance.jamMasuk || todayAttendance.jamKeluar || !user.roles.includes('Karyawan')" />
                                </div>
                            </div>
                        </div>
                    </div>
                </q-card-section>
            </q-card>

            <!-- Attendance History Section -->
            <q-card>
                <q-card-section class="bg-grey-2">
                    <div class="text-h6">Attendance History</div>
                </q-card-section>

                <q-separator />

                <q-card-section>
                    <!-- Filters -->
                    <div class="row q-col-gutter-md q-mb-md">
                        <div class="col-12 col-md-3">
                            <q-input v-model="filters.startDate" label="Start Date" outlined dense type="date" />
                        </div>
                        <div class="col-12 col-md-3">
                            <q-input v-model="filters.endDate" label="End Date" outlined dense type="date" />
                        </div>
                        <div class="col-12 col-md-3">
                            <q-select v-model="filters.status" label="Status" outlined dense clearable
                                :options="['HADIR', 'IZIN', 'SAKIT', 'ALPHA', 'CUTI']" />
                        </div>
                        <div class="col-12 col-md-3">
                            <q-btn unelevated color="primary" label="Search" icon="search" class="full-width"
                                @click="fetchAttendanceHistory" />
                        </div>
                    </div>

                    <!-- Data Table -->
                    <q-table flat bordered :rows="historyRows" :columns="historyColumns" row-key="id"
                        :loading="loadingHistory" v-model:pagination="historyPagination" @request="onHistoryRequest"
                        binary-state-sort>
                        <template v-slot:body-cell-tanggal="props">
                                {{ formatDate(props.row.tanggal) }}
                        </template>

                        <template v-slot:body-cell-status="props">
                                <q-badge :color="getStatusColor(props.row.status)">
                                    {{ props.row.status }}
                                </q-badge>
                        </template>

                        <template v-slot:body-cell-flags="props">
                                <q-badge v-if="props.row.terlambat" color="warning" class="q-mr-xs">Late</q-badge>
                                <q-badge v-if="props.row.pulangCepat" color="orange" class="q-mr-xs">Early</q-badge>
                                <q-badge v-if="props.row.lembur > 0" color="green">OT: {{ props.row.lembur }}m</q-badge>
                        </template>

                        <template v-slot:body-cell-actions="props">
                                <q-btn flat dense round icon="edit" color="primary"
                                    @click="openMarkAbsenceDialog(props.row)">
                                    <q-tooltip>Mark Absence</q-tooltip>
                                </q-btn>
                        </template>
                    </q-table>
                </q-card-section>
            </q-card>

            <!-- Mark Absence Dialog (Admin) -->
            <GenericDialog v-model="showMarkAbsenceDialog" title="Mark Absence" min-width="400px">
                <q-form @submit="markAbsence" id="absence-form" class="q-gutter-md">
                    <q-input v-model="absenceForm.tanggal" label="Date *" outlined dense type="date"
                        :rules="[val => !!val || 'Date harus diisi']" />

                    <q-select v-model="absenceForm.status" label="Status *" outlined dense
                        :options="['IZIN', 'SAKIT', 'ALPHA', 'CUTI']" :rules="[val => !!val || 'Status harus diisi']" />

                    <q-input v-model="absenceForm.keterangan" label="Notes" outlined dense type="textarea" rows="3" />
                </q-form>
                <template #actions>
                    <q-btn flat label="Batalkan" color="primary" @click="closeMarkAbsenceDialog" />
                    <q-btn label="Simpan" type="submit" form="absence-form" color="primary" :loading="marking" />
                </template>
            </GenericDialog>
        </div>
    </q-page>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { api } from 'boot/axios'
import { useQuasar } from 'quasar'
import GenericDialog from 'components/GenericDialog.vue'
import { useAuthStore } from 'stores/auth-store'
import { computed } from 'vue'

const $q = useQuasar()
const authStore = useAuthStore()
const user = computed(() => authStore.user)
const role = computed(() => user.value.roles)

// State
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

const filters = ref({
    startDate: null,
    endDate: null,
    status: null
})

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

// Columns
const historyColumns = [
    { name: 'tanggal', label: 'Date', align: 'left', field: 'tanggal', sortable: true },
    { name: 'jamMasuk', label: 'Clock In', align: 'center', field: 'jamMasuk' },
    { name: 'jamKeluar', label: 'Clock Out', align: 'center', field: 'jamKeluar' },
    { name: 'status', label: 'Status', align: 'center', field: 'status', sortable: true },
    { name: 'flags', label: 'Flags', align: 'center', field: 'flags' },
    { name: 'keterangan', label: 'Notes', align: 'left', field: 'keterangan' },
    { name: 'actions', label: 'Actions', align: 'center', field: 'actions' }
]

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
            message: 'Failed to fetch employees',
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
                message: 'Clock-in successful',
                icon: 'check_circle'
            })
            await fetchTodayAttendance()
            await fetchAttendanceHistory()
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: 'Clock-in failed',
            caption: error.response?.data?.message || error.message
        })
    } finally {
        clocking.value = false
    }
}

const clockOut = async () => {
    clocking.value = true
    try {
        const response = await api.post('/api/pazaauto/absensi/clock-out', {
            karyawanId: selectedKaryawan.value,
            location: 'Office' // Can be enhanced with geolocation
        })

        if (response.data.success) {
            $q.notify({
                type: 'positive',
                message: 'Clock-out successful',
                icon: 'check_circle'
            })
            await fetchTodayAttendance()
            await fetchAttendanceHistory()
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: 'Clock-out failed',
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
            historyPagination.value.rowsNumber = pageData.totalCount || 0
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: 'Failed to fetch attendance history',
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
                message: 'Absence marked successfully'
            })
            closeMarkAbsenceDialog()
            await fetchTodayAttendance()
            await fetchAttendanceHistory()
        }
    } catch (error) {
        $q.notify({
            type: 'negative',
            message: 'Failed to mark absence',
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
    if (selectedKaryawan.value) {
        fetchTodayAttendance()
        fetchAttendanceHistory()
    }
}

// Lifecycle
onMounted(() => {
    updateClock()
    clockInterval = setInterval(updateClock, 1000)
    if (user.value.roles.includes('Admin')) {
        fetchKaryawan()
    } else {
        fetchTodayAttendance();
    }
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
