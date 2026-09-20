<template>
  <div class="q-pa-sm">

    <div v-if="$slots.title" class="col-auto text-subtitle1 text-weight-medium">
      <slot name="title"></slot>
    </div>

    <!-- Table -->
    <q-table class="my-sticky-header-table" :class="{ 'cursor-pointer-rows': !!onEdit }" flat bordered :rows="rows"
             :columns="filteredColumns" :row-key="rowKey" :loading="loading" v-model:pagination="internalPagination"
             @request="onRequest" @row-click="onRowClick" binary-state-sort :selected="selectedRows"
             @keydown="handleKeydown" tabindex="0" ref="tableRef"
             :rows-per-page-options="[5, 10, 25, 50]"
             :style="{ outline: 'none', ...(effectiveHeight ? { height: effectiveHeight } : {}) }"
    >
      <!-- Pass through all slots except the toolbar-only ones -->
      <template v-for="slot in passThroughSlots" v-slot:[slot]="scope">
        <slot :name="slot" v-bind="scope"/>
      </template>

      <!-- Toolbar (title + search + filters) inside the table's top bar -->
      <template v-slot:top>
        <div class="row items-center no-wrap full-width q-gutter-x-sm">
          <div class="col" v-if="enableSearch">
            <q-input dense outlined bg-color="white" v-model="internalSearch" input-class="search-field text-left"
                     :placeholder="searchPlaceholder || $t('search')">
              <template v-slot:append>
                <slot name="search-append"></slot>
                <q-icon v-if="internalSearch === ''" name="search"/>
                <q-icon v-else name="clear" class="cursor-pointer" @click="internalSearch = ''"/>
              </template>
            </q-input>
          </div>
          <q-space v-else/>
          <slot name="toolbar-filters"></slot>
          <q-btn v-if="footerButtonLabel && footerButtonAction" :label="footerButtonLabel" icon="print"
                 color="primary" @click="footerButtonAction"/>
        </div>
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
import {ref, watch, computed, nextTick, onMounted, onBeforeUnmount, getCurrentInstance} from 'vue'

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
  },
  footerButtonLabel: {
    type: String,
    default: ''
  },
  footerButtonAction: {
    type: Function,
    default: () => {}
  },
  // Fixed table height (e.g. when stacking tables); by default the table fills the space down to the viewport bottom
  tableHeight: {
    type: String,
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

// Slots rendered by the toolbar above; everything else is forwarded to q-table
const TOOLBAR_SLOTS = ['top', 'title', 'search-append', 'toolbar-filters']
const passThroughSlots = computed(() => Object.keys(instance.slots).filter(slot => !TOOLBAR_SLOTS.includes(slot)))

const filteredColumns = computed(() => {
  if (!props.columns || !Array.isArray(props.columns)) {
    return []
  }
  if (hasActions.value) {
    return props.columns
  }
  return props.columns.filter(col => col.name !== 'actions')
})

const hasActions = computed(() => {
  if (!props.columns || !Array.isArray(props.columns)) {
    return false
  }
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

// Select a row by index (must be defined before watch and handleKeydown)
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

// ── Auto height: fill the space from the table's top down to the viewport bottom, without page scroll ──
const MIN_AUTO_HEIGHT = 240
const autoHeight = ref(null)
const effectiveHeight = computed(() => props.tableHeight || autoHeight.value)

const px = (value) => parseFloat(value) || 0

const isInFlow = (style) => style.display !== 'none' && style.position !== 'absolute' && style.position !== 'fixed'

// Vertical space the layout needs below the table: bottom margins, paddings and borders of every ancestor,
// plus content stacked after it (side-by-side siblings in a row flex/grid container don't count)
const spaceBelow = (el) => {
  let space = 0
  for (let node = el; node && node !== document.body; node = node.parentElement) {
    space += px(getComputedStyle(node).marginBottom)
    const parent = node.parentElement
    if (!parent) break
    const parentStyle = getComputedStyle(parent)
    const stacksVertically = parentStyle.display.includes('grid')
      ? false
      : !parentStyle.display.includes('flex') || parentStyle.flexDirection.startsWith('column')
    if (stacksVertically) {
      for (let sib = node.nextElementSibling; sib; sib = sib.nextElementSibling) {
        const sibStyle = getComputedStyle(sib)
        if (isInFlow(sibStyle)) space += sib.offsetHeight + px(sibStyle.marginTop) + px(sibStyle.marginBottom)
      }
    }
    space += px(parentStyle.paddingBottom) + px(parentStyle.borderBottomWidth)
  }
  return space
}

const updateAutoHeight = () => {
  const el = tableRef.value?.$el
  if (props.tableHeight || !el) return
  const top = el.getBoundingClientRect().top + window.scrollY
  const available = Math.floor(window.innerHeight - top - spaceBelow(el))
  const height = `${Math.max(MIN_AUTO_HEIGHT, available)}px`
  if (height !== autoHeight.value) autoHeight.value = height
}

// Content above the table (titles, filters, banners) can change size after mount
let layoutObserver = null

onMounted(() => {
  if (tableRef.value) {
    tableRef.value.$el.focus()
  }

  nextTick(updateAutoHeight)
  window.addEventListener('resize', updateAutoHeight)
  if (typeof ResizeObserver !== 'undefined') {
    layoutObserver = new ResizeObserver(updateAutoHeight)
    layoutObserver.observe(document.body)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateAutoHeight)
  layoutObserver?.disconnect()
  clearTimeout(searchTimeout)
})

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
