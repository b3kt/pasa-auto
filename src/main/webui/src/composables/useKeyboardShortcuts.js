import { onMounted, onUnmounted } from 'vue'

export function useKeyboardShortcuts(shortcuts) {
  const handleKeyDown = (event) => {
    const isCtrl = event.ctrlKey || event.metaKey
    const key = event.key.toLowerCase()

    if (isCtrl && key === 's') {
      if (shortcuts.onSave) {
        event.preventDefault()
        shortcuts.onSave()
      }
    } else if (isCtrl && key === 'n') {
      if (shortcuts.onNew) {
        event.preventDefault()
        shortcuts.onNew()
      }
    } else if (key === 'delete') {
      // Avoid triggering delete if we are in an input field
      if (event.target.tagName !== 'INPUT' && event.target.tagName !== 'TEXTAREA') {
        if (shortcuts.onDelete) {
          event.preventDefault()
          shortcuts.onDelete()
        }
      }
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', handleKeyDown)
  })

  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeyDown)
  })
}
