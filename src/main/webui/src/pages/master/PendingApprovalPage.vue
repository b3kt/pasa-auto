<template>
  <q-page padding>
    <q-card flat bordered>
      <q-card-section>
        <div class="text-h6">{{ $t('pages.pendingApprovalPage.title') }}</div>
        <div class="text-caption text-grey-7">
          {{ $t('pages.pendingApprovalPage.description') }}
        </div>
      </q-card-section>

      <q-separator />

      <q-table :rows="rows" :columns="columns" row-key="id" :loading="loading" flat
               :rows-per-page-options="[10, 25, 50]" :no-data-label="$t('pages.pendingApprovalPage.noDataLabel')">
        <template v-slot:body-cell-createdAt="props">
          <q-td :props="props">{{ formatDateTime(props.row.createdAt) }}</q-td>
        </template>

        <template v-slot:body-cell-actions="props">
          <q-td :props="props" class="q-gutter-xs">
            <q-btn dense color="positive" icon="check" :label="$t('pages.pendingApprovalPage.approveButton')" no-caps
                   :loading="actingOn === props.row.id" @click="confirm(props.row, 'approve')" />
            <q-btn dense flat color="negative" icon="block" :label="$t('pages.pendingApprovalPage.rejectButton')" no-caps
                   :loading="actingOn === props.row.id" @click="confirm(props.row, 'reject')" />
          </q-td>
        </template>
      </q-table>
    </q-card>

    <GenericDialog v-model="showConfirm" :title="pendingAction === 'approve' ? $t('pages.pendingApprovalPage.approveTitle') : $t('pages.pendingApprovalPage.rejectTitle')"
                   min-width="400px" position="standard">
      <template v-if="pendingAction === 'approve'">
        {{ $t('pages.pendingApprovalPage.approveMessage', { username: selected?.username, email: selected?.email }) }}
      </template>
      <template v-else>
        {{ $t('pages.pendingApprovalPage.rejectMessage', { username: selected?.username, email: selected?.email }) }}
      </template>
      <template #actions>
        <q-btn flat :label="$t('cancel')" color="primary" @click="showConfirm = false" />
        <q-btn flat :label="pendingAction === 'approve' ? $t('pages.pendingApprovalPage.approveButton') : $t('pages.pendingApprovalPage.rejectButton')"
               :color="pendingAction === 'approve' ? 'positive' : 'negative'" @click="act" />
      </template>
    </GenericDialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar, date } from 'quasar'
import { useI18n } from 'vue-i18n'
import { api } from 'boot/axios'
import GenericDialog from 'components/GenericDialog.vue'

const $q = useQuasar()
const { t } = useI18n()

const rows = ref([])
const loading = ref(false)
const actingOn = ref(null)
const showConfirm = ref(false)
const selected = ref(null)
const pendingAction = ref('approve')

const columns = computed(() => [
  { name: 'username', label: t('username'), field: 'username', align: 'left', sortable: true },
  { name: 'email', label: t('email'), field: 'email', align: 'left', sortable: true },
  { name: 'createdAt', label: t('pages.pendingApprovalPage.registeredColumn'), field: 'createdAt', align: 'left', sortable: true },
  { name: 'actions', label: t('actions'), field: 'actions', align: 'right' }
])

const formatDateTime = (value) => (value ? date.formatDate(value, 'YYYY-MM-DD HH:mm') : '')

const fetchPending = async () => {
  loading.value = true
  try {
    const response = await api.get('/api/users/pending')
    rows.value = response.data?.data || []
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: t('pages.pendingApprovalPage.fetchFailed'),
      caption: error.response?.data?.message || error.message
    })
  } finally {
    loading.value = false
  }
}

const confirm = (row, action) => {
  selected.value = row
  pendingAction.value = action
  showConfirm.value = true
}

const act = async () => {
  const row = selected.value
  showConfirm.value = false
  actingOn.value = row.id

  try {
    await api.post(`/api/users/${row.id}/${pendingAction.value}`)
    $q.notify({
      type: 'positive',
      message: pendingAction.value === 'approve'
        ? t('pages.pendingApprovalPage.approvedNotify', { username: row.username })
        : t('pages.pendingApprovalPage.rejectedNotify', { username: row.username })
    })
    await fetchPending()
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: t('pages.pendingApprovalPage.actionFailed'),
      caption: error.response?.data?.message || error.message
    })
  } finally {
    actingOn.value = null
  }
}

onMounted(fetchPending)
</script>
