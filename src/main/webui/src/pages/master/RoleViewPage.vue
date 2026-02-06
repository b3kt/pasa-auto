<template>
  <q-page padding>
    <div class="q-pa-md">
      <!-- Header with Back Button -->
      <div class="row items-center q-mb-lg">
        <q-btn flat round dense icon="arrow_back" color="primary" @click="goBack">
          <q-tooltip>Back to Roles</q-tooltip>
        </q-btn>
        <div class="text-h5 q-ml-md">Role Details</div>
      </div>

      <!-- Role Details Card -->
      <q-card class="q-mb-lg">
        <q-card-section>
          <div class="text-h6">{{ roleData.name }}</div>
          <div class="text-caption text-grey-7">{{ roleData.description || 'No description' }}</div>
        </q-card-section>
        <q-separator />
        <q-card-section>
          <div class="row q-col-gutter-md">
            <div class="col-12 col-md-6">
              <div class="text-caption text-grey-7">Status</div>
              <q-badge :color="roleData.active ? 'green' : 'red'">
                {{ roleData.active ? 'Active' : 'Inactive' }}
              </q-badge>
            </div>
            <div class="col-12 col-md-6">
              <div class="text-caption text-grey-7">Role ID</div>
              <div>{{ roleData.id }}</div>
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- User Assignment Section -->
      <div class="text-h6 q-mb-md">User Assignment</div>
      <div class="row q-col-gutter-md">
        <!-- Available Users -->
        <div class="col-12 col-md-6">
          <q-card>
            <q-card-section class="bg-grey-2">
              <div class="row items-center">
                <div class="text-subtitle1 col">Available Users</div>
                <q-chip dense color="primary" text-color="white">
                  {{ availableUsers.length }}
                </q-chip>
              </div>
              <q-input v-model="searchAvailable" dense outlined placeholder="Search users..." class="q-mt-sm">
                <template v-slot:prepend>
                  <q-icon name="search" />
                </template>
              </q-input>
            </q-card-section>
            <q-separator />
            <q-card-section class="user-list-container">
              <q-list bordered separator>
                <q-item v-for="user in filteredAvailableUsers" :key="user.id" clickable v-ripple draggable="true"
                  @dragstart="onDragStart($event, user, 'available')" @dragend="onDragEnd" class="user-item"
                  :class="{ 'dragging': draggingUser?.id === user.id }">
                  <q-item-section avatar>
                    <q-avatar color="primary" text-color="white">
                      {{ user.username.charAt(0).toUpperCase() }}
                    </q-avatar>
                  </q-item-section>
                  <q-item-section>
                    <q-item-label>{{ user.username }}</q-item-label>
                    <q-item-label caption>{{ user.email }}</q-item-label>
                  </q-item-section>
                  <q-item-section side>
                    <q-btn flat dense round icon="arrow_forward" color="primary" size="sm" @click="assignUser(user)">
                      <q-tooltip>Assign to role</q-tooltip>
                    </q-btn>
                  </q-item-section>
                </q-item>
                <q-item v-if="filteredAvailableUsers.length === 0">
                  <q-item-section class="text-center text-grey-6">
                    No available users
                  </q-item-section>
                </q-item>
              </q-list>
            </q-card-section>
          </q-card>
        </div>

        <!-- Assigned Users -->
        <div class="col-12 col-md-6">
          <q-card @drop="onDrop($event, 'assigned')" @dragover.prevent @dragenter="onDragEnter('assigned')"
            @dragleave="onDragLeave('assigned')" :class="{ 'drop-zone-active': dropZone === 'assigned' }">
            <q-card-section class="bg-primary text-white">
              <div class="row items-center">
                <div class="text-subtitle1 col">Assigned Users</div>
                <q-chip dense color="white" text-color="primary">
                  {{ assignedUsers.length }}
                </q-chip>
              </div>
              <q-input v-model="searchAssigned" dense outlined dark placeholder="Search users..." class="q-mt-sm">
                <template v-slot:prepend>
                  <q-icon name="search" />
                </template>
              </q-input>
            </q-card-section>
            <q-separator />
            <q-card-section class="user-list-container">
              <q-list bordered separator>
                <q-item v-for="user in filteredAssignedUsers" :key="user.id" clickable v-ripple draggable="true"
                  @dragstart="onDragStart($event, user, 'assigned')" @dragend="onDragEnd" class="user-item"
                  :class="{ 'dragging': draggingUser?.id === user.id }">
                  <q-item-section avatar>
                    <q-avatar color="primary" text-color="white">
                      {{ user.username.charAt(0).toUpperCase() }}
                    </q-avatar>
                  </q-item-section>
                  <q-item-section>
                    <q-item-label>{{ user.username }}</q-item-label>
                    <q-item-label caption>{{ user.email }}</q-item-label>
                  </q-item-section>
                  <q-item-section side>
                    <q-btn flat dense round icon="close" color="negative" size="sm" @click="unassignUser(user)">
                      <q-tooltip>Remove from role</q-tooltip>
                    </q-btn>
                  </q-item-section>
                </q-item>
                <q-item v-if="filteredAssignedUsers.length === 0">
                  <q-item-section class="text-center text-grey-6">
                    No assigned users
                  </q-item-section>
                </q-item>
              </q-list>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="row justify-end q-mt-lg q-gutter-sm">
        <q-btn flat label="Cancel" color="primary" @click="goBack" />
        <q-btn label="Save Changes" color="primary" @click="saveChanges" :loading="saving" />
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from 'boot/axios'
import { useQuasar } from 'quasar'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()

// State
const loading = ref(false)
const saving = ref(false)
const roleData = ref({
  id: null,
  name: '',
  description: '',
  active: true
})
const allUsers = ref([])
const assignedUsers = ref([])
const searchAvailable = ref('')
const searchAssigned = ref('')
const draggingUser = ref(null)
const dropZone = ref(null)

// Computed
const availableUsers = computed(() => {
  return allUsers.value.filter(user =>
    !assignedUsers.value.some(assigned => assigned.id === user.id)
  )
})

const filteredAvailableUsers = computed(() => {
  if (!searchAvailable.value) return availableUsers.value
  const search = searchAvailable.value.toLowerCase()
  return availableUsers.value.filter(user =>
    user.username.toLowerCase().includes(search) ||
    user.email.toLowerCase().includes(search)
  )
})

const filteredAssignedUsers = computed(() => {
  if (!searchAssigned.value) return assignedUsers.value
  const search = searchAssigned.value.toLowerCase()
  return assignedUsers.value.filter(user =>
    user.username.toLowerCase().includes(search) ||
    user.email.toLowerCase().includes(search)
  )
})

// Methods
const fetchRoleDetails = async () => {
  loading.value = true
  try {
    const roleId = route.params.id
    const response = await api.get(`/api/roles/${roleId}`)
    if (response.data.success) {
      roleData.value = response.data.data
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to fetch role details',
      caption: error.response?.data?.message || error.message
    })
    goBack()
  } finally {
    loading.value = false
  }
}

const fetchAllUsers = async () => {
  try {
    const response = await api.get('/api/users')
    if (response.data.success) {
      allUsers.value = response.data.data
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to fetch users',
      caption: error.response?.data?.message || error.message
    })
  }
}

const fetchAssignedUsers = async () => {
  try {
    const roleId = route.params.id
    const response = await api.get(`/api/roles/${roleId}/users`)
    if (response.data.success) {
      assignedUsers.value = response.data.data
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to fetch assigned users',
      caption: error.response?.data?.message || error.message
    })
  }
}

const assignUser = (user) => {
  if (!assignedUsers.value.some(u => u.id === user.id)) {
    assignedUsers.value.push(user)
    $q.notify({
      type: 'positive',
      message: `${user.username} assigned to role`,
      timeout: 1000
    })
  }
}

const unassignUser = (user) => {
  const index = assignedUsers.value.findIndex(u => u.id === user.id)
  if (index !== -1) {
    assignedUsers.value.splice(index, 1)
    $q.notify({
      type: 'positive',
      message: `${user.username} removed from role`,
      timeout: 1000
    })
  }
}

// Drag and Drop handlers
const onDragStart = (event, user, source) => {
  draggingUser.value = user
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', JSON.stringify({ user, source }))
}

const onDragEnd = () => {
  draggingUser.value = null
  dropZone.value = null
}

const onDragEnter = (zone) => {
  dropZone.value = zone
}

const onDragLeave = (zone) => {
  if (dropZone.value === zone) {
    dropZone.value = null
  }
}

const onDrop = (event, target) => {
  event.preventDefault()
  dropZone.value = null

  try {
    const data = JSON.parse(event.dataTransfer.getData('text/plain'))
    const { user, source } = data

    if (target === 'assigned' && source === 'available') {
      assignUser(user)
    } else if (target === 'available' && source === 'assigned') {
      unassignUser(user)
    }
  } catch (error) {
    console.error('Drop error:', error)
  }
}

const saveChanges = async () => {
  saving.value = true
  try {
    const roleId = route.params.id
    const userIds = assignedUsers.value.map(u => u.id)

    const response = await api.put(`/api/roles/${roleId}/users`, { userIds })
    if (response.data.success) {
      $q.notify({
        type: 'positive',
        message: 'Role users updated successfully'
      })
      goBack()
    }
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: 'Failed to save changes',
      caption: error.response?.data?.message || error.message
    })
  } finally {
    saving.value = false
  }
}

const goBack = () => {
  router.push('/roles')
}

// Lifecycle
onMounted(async () => {
  await fetchRoleDetails()
  await fetchAllUsers()
  await fetchAssignedUsers()
})
</script>

<style lang="sass" scoped>
.user-list-container
  max-height: 500px
  overflow-y: auto

.user-item
  cursor: move
  transition: all 0.3s ease
  
  &:hover
    background-color: rgba(0, 0, 0, 0.05)
  
  &.dragging
    opacity: 0.5

.drop-zone-active
  border: 2px dashed #1976d2
  background-color: rgba(25, 118, 210, 0.05)
</style>
