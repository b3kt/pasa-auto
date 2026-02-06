<template>
  <div class="q-pa-sm">
    <!-- Toolbar -->
    <q-toolbar class="shadow-1 rounded-borders q-mb-lg ">

      <div class="col" v-if="enableSearch">
        <q-input dense standout="bg-primary" v-model="internalSearch" input-class="search-field text-left"
                 :placeholder="!searchPlaceholder ? searchPlaceholder : $t('search')">
          <template v-slot:append>
            <slot name="search-append"></slot>
            <q-icon v-if="internalSearch === ''" name="search"/>
            <q-icon v-else name="clear" class="cursor-pointer" @click="internalSearch = ''"/>
          </template>
        </q-input>
      </div>

      <div class="col-auto q-pl-sm">
        <slot name="toolbar-filters"></slot>
      </div>

    </q-toolbar>

    <!-- Table -->
    <q-table class="my-sticky-header-table" :class="{ 'cursor-pointer-rows': !!onEdit }" flat bordered :rows="rows"
             :columns="filteredColumns" :row-key="rowKey" :loading="loading" v-model:pagination="internalPagination"
             @request="onRequest" @row-click="onRowClick" binary-state-sort :selected="selectedRows"
             @keydown="handleKeydown" tabindex="0" ref="tableRef"
             style="outline: none"
    >
      <!-- Pass through all slots -->
      <template v-for="(_, slot) in $slots" v-slot:[slot]="scope">
        <slot :name="slot" v-bind="scope"/>
      </template>

      <!-- Custom row slot for selection styling -->
      <template v-slot:body="props">
        <q-tr
          :props="props"
          @click="onRowClick($event, props.row, props.rowIndex)"
          :class="{ 'selected-row': isRowSelected(props.row) }"
          @keydown="handleRowKeydown($event, props.row, props.rowIndex)"
          tabindex="-1"
        >
          <q-td v-for="col in props.cols" :key="col.name" :props="props">
            <slot :name="`body-cell-${col.name}`" v-bind="props">
              <template v-if="col.name === 'actions'">
                <q-btn v-if="onDelete" flat dense round icon="delete" color="negative"
                       @click.stop="onDelete(props.row)">
                  <q-tooltip>Delete</q-tooltip>
                </q-btn>
              </template>
              <template v-else>
                {{ col.value }}
              </template>
            </slot>
          </q-td>
        </q-tr>
      </template>
    </q-table>
  </div>
</template>

<script setup>
import {ref, watch, computed, nextTick, onMounted, getCurrentInstance} from 'vue'

const instance = getCurrentInstance()

const props = defineProps({
  rows: {
    type: Array,
    required: true
  },
  columns: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  pagination: {
    type: Object,
    required: true
  },
  rowKey: {
    type: String,
    default: 'id'
  },
  createLabel: {
    type: String,
    default: 'Create'
  },
  searchPlaceholder: {
    type: String,
    default: 'Search...'
  },
  enableSearch: {
    type: Boolean,
    default: true
  },
  searchValue: {
    type: String,
    default: ''
  },
  onCreate: {
    type: Function,
    default: null
  },
  onEdit: {
    type: Function,
    default: null
  },
  onDelete: {
    type: Function,
    default: null
  }
})

const emit = defineEmits(['update:pagination', 'request', 'search', 'update:searchValue'])

// Internal state for two-way binding
const internalPagination = computed({
  get: () => props.pagination,
  set: (val) => emit('update:pagination', val)
})

const internalSearch = ref(props.searchValue)
let searchTimeout = null

// Watch prop to update internal state
watch(() => props.searchValue, (newVal) => {
  if (newVal !== internalSearch.value) {
    internalSearch.value = newVal
  }
})

// Watch search text for debouncing
watch(internalSearch, (newVal) => {
  emit('update:searchValue', newVal)
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    emit('search', newVal)
  }, 500)
})

const onRequest = (requestProp) => {
  emit('request', requestProp)
}

const onRowClick = (evt, row, index) => {
  // Update selection when row is clicked
  selectRow(index)

  if (props.onEdit) {
    console.log(index)
    props.onEdit(row)
  }
}

const filteredColumns = computed(() => {
  if (hasActions.value) {
    return props.columns
  }
  return props.columns.filter(col => col.name !== 'actions')
})

const hasActions = computed(() => {
  return props.columns.some(col => col.name === 'actions') && (
    !!props.onDelete ||
    !!props.onEdit ||
    Object.keys(instance.slots).some(slot => slot.startsWith('body-cell-actions'))
  )
})

// Keyboard navigation state
const selectedRowIndex = ref(-1)
const selectedRows = ref([])
const tableRef = ref(null)

// Check if a row is selected
const isRowSelected = (row) => {
  return selectedRows.value.some(selected => selected[props.rowKey] === row[props.rowKey])
}

// Handle keyboard navigation
const handleKeydown = (event) => {
  if (!props.rows || props.rows.length === 0) return

  const currentIndex = selectedRowIndex.value
  let newIndex = currentIndex

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      newIndex = Math.min(currentIndex + 1, props.rows.length - 1)
      selectRow(newIndex)
      break
    case 'ArrowUp':
      event.preventDefault()
      newIndex = Math.max(currentIndex - 1, 0)
      selectRow(newIndex)
      break
    case 'Enter':
    case ' ':
      // Only trigger if not typing in an input
      if (event.target.tagName !== 'INPUT' && event.target.tagName !== 'TEXTAREA') {
        event.preventDefault()
        if (currentIndex >= 0 && currentIndex < props.rows.length && props.onEdit) {
          props.onEdit(props.rows[currentIndex])
        }
      }
      break
  }
}

// Handle row-specific keyboard events
const handleRowKeydown = (event, row) => {
  if (event.key === 'Enter' || event.key === ' ') {
    if (event.target.tagName !== 'INPUT' && event.target.tagName !== 'TEXTAREA') {
      event.preventDefault()
      if (props.onEdit) {
        props.onEdit(row)
      }
    }
  }
}

// Watch for changes in rows data to auto-select first row
watch(() => props.rows, (newRows) => {
  if (newRows && newRows.length > 0 && selectedRowIndex.value === -1) {
    selectRow(0)
  }
}, {immediate: true})

onMounted(() => {
  if (tableRef.value) {
    tableRef.value.$el.focus()
  }
})

// Select a row by index
const selectRow = async (index) => {
  if (index < 0 || !props.rows || index >= props.rows.length) return

  selectedRowIndex.value = index
  selectedRows.value = [props.rows[index]]

  // Scroll the selected row into view
  await nextTick()
  const tableElement = tableRef.value?.$el
  if (tableElement) {
    const rows = tableElement.querySelectorAll('tbody tr')
    if (rows[index]) {
      rows[index].scrollIntoView({behavior: 'auto', block: 'nearest'})
    }
  }
}

// Select a row by item object (matching rowKey)
const selectRowByItem = (item) => {
  if (!item || !props.rows) return
  const index = props.rows.findIndex(row => row[props.rowKey] === item[props.rowKey])
  if (index !== -1) {
    selectRow(index)
  }
}

defineExpose({
  selectRow,
  selectRowByItem
})
</script>

<style lang="sass">
.my-sticky-header-table
  height: 75vh

  .q-table__top,
  .q-table__bottom,
  thead tr:first-child th
    background-color: #fafafa

  thead tr th
    position: sticky
    z-index: 1

  thead tr:first-child th
    top: 0

  &.q-table--loading thead tr:last-child th
    top: 48px

  tbody
    scroll-margin-top: 48px

.cursor-pointer-rows
  tbody tr
    cursor: pointer

.selected-row
  background-color: #e3f2fd !important
  transition: background-color 0.2s ease

  &:hover
    background-color: #bbdefb !important

.selected-indicator
  width: 32px
  text-align: center
  vertical-align: middle

  .q-icon
    animation: fadeIn 0.2s ease

@keyframes fadeIn
  from
    opacity: 0
    transform: translateX(-5px)
  to
    opacity: 1
    transform: translateX(0)
</style>
