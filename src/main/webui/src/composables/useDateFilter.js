import { ref, computed, watch } from 'vue'
import { date } from 'quasar'
import browserCache from '../utils/browserCache.js'

const CACHE_TTL = 24 * 60 * 60 * 1000 // 24 hours

/**
 * Composable for persisting date range filter selection in browser cache.
 * The cache resets automatically when the calendar date changes (a new day begins).
 *
 * @param {string} pageKey - Unique key identifying the page (e.g. 'spk', 'penjualan')
 */
export function useDateFilter(pageKey) {
  const cacheKey = `date-filter-${pageKey}`
  const today = date.formatDate(new Date(), 'YYYY-MM-DD')

  const todayVal = new Date()
  const yesterdayVal = date.subtractFromDate(todayVal, { days: 1 })
  const defaultRange = {
    from: date.formatDate(yesterdayVal, 'YYYY/MM/DD'),
    to: date.formatDate(todayVal, 'YYYY/MM/DD')
  }

  // Restore from cache only if it was saved on today's calendar date
  const cached = browserCache.getItem(cacheKey)
  const initialRange = cached && cached.cachedDate === today ? cached.dateRange : defaultRange

  const dateRange = ref(initialRange)

  const dateRangeText = computed(() => {
    if (!dateRange.value || (!dateRange.value.from && !dateRange.value.to)) return ''
    if (dateRange.value.from && dateRange.value.to) return `${dateRange.value.from} - ${dateRange.value.to}`
    if (dateRange.value.from) return `From: ${dateRange.value.from}`
    if (dateRange.value.to) return `To: ${dateRange.value.to}`
    return ''
  })

  const clearDateRange = () => {
    dateRange.value = { from: '', to: '' }
    browserCache.removeItem(cacheKey)
  }

  watch(
    dateRange,
    (val) => {
      if (val?.from || val?.to) {
        browserCache.setItem(cacheKey, { dateRange: val, cachedDate: today }, CACHE_TTL)
      } else {
        browserCache.removeItem(cacheKey)
      }
    },
    { deep: true }
  )

  return { dateRange, dateRangeText, clearDateRange }
}
