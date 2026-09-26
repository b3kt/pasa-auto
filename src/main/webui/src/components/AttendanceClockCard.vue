<template>
    <q-card style="width: 100%; max-width: 480px">
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

            <q-input :model-value="user?.karyawanNama" :label="$t('pages.absensi.employeeNameLabel')" outlined dense
                readonly />

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
                    <q-btn unelevated color="positive" icon="login" :label="$t('pages.absensi.clockInButton')"
                        class="full-width" @click="clockIn" :loading="clocking"
                        :disable="!karyawanId || !!todayAttendance?.jamMasuk" />
                </div>
                <div class="col-6">
                    <q-btn unelevated color="negative" icon="logout" :label="$t('pages.absensi.clockOutButton')"
                        class="full-width" @click="confirmClockOut" :loading="clocking"
                        :disable="!karyawanId || !canClockOut" />
                </div>
            </div>
        </q-card-section>

        <!-- Clock-Out Confirmation Dialog -->
        <GenericDialog v-model="showClockOutConfirm" :title="$t('pages.absensi.confirmClockOutTitle')" min-width="400px">
            {{ $t('pages.absensi.confirmClockOutMessage') }}
            <template #actions>
                <q-btn flat :label="$t('cancel')" color="primary" @click="showClockOutConfirm = false" />
                <q-btn :label="$t('pages.absensi.confirmClockOutButton')" color="negative" @click="clockOutConfirmed"
                    :loading="clocking" />
            </template>
        </GenericDialog>
    </q-card>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { api } from 'boot/axios'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import GenericDialog from 'components/GenericDialog.vue'
import { useAuthStore } from 'stores/auth-store'

const emit = defineEmits(['clocked'])

const $q = useQuasar()
const { t } = useI18n()
const authStore = useAuthStore()
const user = computed(() => authStore.user)
const karyawanId = computed(() => user.value?.karyawanId)

const currentTime = ref('')
const currentDate = ref('')
const todayAttendance = ref(null)
const clocking = ref(false)
const showClockOutConfirm = ref(false)
const clockOutNote = ref('')

const canClockOut = computed(() => !!todayAttendance.value?.jamMasuk && !todayAttendance.value?.jamKeluar)

let clockInterval = null

const updateClock = () => {
    const now = new Date()
    currentTime.value = now.toLocaleTimeString('id-ID')
    currentDate.value = now.toLocaleDateString('id-ID', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
}

const fetchTodayAttendance = async () => {
    if (!karyawanId.value) return
    try {
        const response = await api.get(`/api/pazaauto/absensi/today/${karyawanId.value}`)
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
            karyawanId: karyawanId.value,
            location: 'Office',
            deviceInfo: navigator.userAgent
        })

        if (response.data.success) {
            $q.notify({
                type: 'positive',
                message: t('pages.absensi.clockInSuccess'),
                icon: 'check_circle'
            })
            await fetchTodayAttendance()
            emit('clocked')
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
            karyawanId: karyawanId.value,
            location: 'Office',
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
            emit('clocked')
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

onMounted(() => {
    updateClock()
    clockInterval = setInterval(updateClock, 1000)
    fetchTodayAttendance()
})

onBeforeUnmount(() => {
    if (clockInterval) {
        clearInterval(clockInterval)
    }
})
</script>
