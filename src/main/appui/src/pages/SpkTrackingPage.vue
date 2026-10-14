<template>
  <q-page padding>
    <div class="text-h6 q-mb-md">{{ $t('pages.spk.title') }}</div>

    <q-banner v-if="notLinked" class="bg-warning text-white">
      {{ $t('pages.spk.linkFirst') }}
      <template v-slot:action>
        <q-btn flat color="white" :label="$t('common.linkNow')" to="/link-account" />
      </template>
    </q-banner>

    <q-list v-else bordered separator>
      <q-item
        v-for="s in spks"
        :key="s.id"
        clickable
        v-ripple
        @click="openInvoice(s)"
      >
        <q-item-section>
          <q-item-label>{{ s.noSpk }} &mdash; {{ s.nopol }}</q-item-label>
          <q-item-label caption>{{ s.tanggalJamSpk }}</q-item-label>
          <q-item-label caption v-if="s.keterangan">{{ s.keterangan }}</q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-badge :color="statusColor(s.statusSpk)">{{ statusLabel(s.statusSpk) }}</q-badge>
        </q-item-section>
        <q-item-section side>
          <q-icon name="receipt_long" color="primary" />
        </q-item-section>
      </q-item>
      <q-item v-if="!spks.length">
        <q-item-section class="text-grey-7">{{ $t('pages.spk.none') }}</q-item-section>
      </q-item>
    </q-list>

    <q-dialog v-model="invoiceDialog" maximized>
      <q-card class="column no-wrap full-height">
        <q-card-section class="row items-center justify-between">
          <div class="text-h6">{{ $t('pages.spk.invoiceTitle') }}</div>
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-separator />
        <q-card-section class="col q-pa-none relative-position">
          <iframe
            ref="invoiceFrame"
            class="invoice-frame absolute-full"
            scrolling="no"
            :title="$t('pages.spk.invoiceTitle')"
          />
        </q-card-section>
        <q-separator />
        <q-card-actions align="right">
          <q-btn flat :label="$t('common.close')" v-close-popup />
          <q-btn
            color="primary"
            icon="picture_as_pdf"
            :label="$t('pages.spk.downloadPdf')"
            @click="downloadPdf"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useQuasar } from 'quasar'
import { useI18n } from 'vue-i18n'
import { api } from 'boot/axios'
import renderFaktur from 'assets/template/faktur.template?compiled'

const $q = useQuasar()
const { t, te } = useI18n()
const spks = ref([])
const notLinked = ref(false)
const invoiceDialog = ref(false)
const invoiceFrame = ref(null)

const statusColor = (status) => ({
  OPEN: 'grey',
  PROSES: 'orange',
  SELESAI: 'positive',
  BATAL: 'negative'
})[status] || 'grey'

const statusLabel = (status) => te(`pages.spk.status.${status}`)
  ? t(`pages.spk.status.${status}`)
  : status

const formatCurrency = (value) => {
  if (!value) return 'Rp 0'
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    minimumFractionDigits: 0
  }).format(value)
}

const formatNumber = (value) => {
  if (!value) return '0'
  return new Intl.NumberFormat('id-ID', {
    minimumFractionDigits: 0
  }).format(value)
}

const invoiceData = ref(null)

// Writes the rendered faktur into the dialog's iframe. The template is a full HTML document, so an
// iframe keeps its <style> scoped away from the page and doubles as the print source.
const writeInvoice = () => {
  const frame = invoiceFrame.value
  if (!frame) return
  const doc = frame.contentWindow.document
  doc.open()
  doc.write(renderFaktur(invoiceData.value, formatCurrency, formatNumber))
  doc.close()
}

// Scales the whole faktur so it fits the preview frame with no scrollbars. The transform is applied
// through the template's screen-only `fit` class, so printing resets it to the natural page layout.
const fitInvoice = () => {
  const frame = invoiceFrame.value
  const doc = frame?.contentDocument
  const body = doc?.body
  const root = doc?.documentElement
  if (!frame || !body || !root) return
  const availWidth = frame.clientWidth
  const availHeight = frame.clientHeight
  if (!availWidth || !availHeight) return
  // Measure at the natural size: clear any previous fit first (overflow hidden would clamp the box).
  root.classList.remove('fit')
  body.classList.remove('fit')
  body.style.setProperty('--fit-transform', 'none')
  const naturalWidth = Math.max(body.scrollWidth, root.scrollWidth)
  const naturalHeight = Math.max(body.scrollHeight, root.scrollHeight)
  if (!naturalWidth || !naturalHeight) return
  const scale = Math.min(availWidth / naturalWidth, availHeight / naturalHeight, 1)
  const offsetX = Math.max(0, (availWidth - naturalWidth * scale) / 2)
  const offsetY = Math.max(0, (availHeight - naturalHeight * scale) / 2)
  body.style.setProperty('--fit-transform', `translate(${offsetX}px, ${offsetY}px) scale(${scale})`)
  root.classList.add('fit')
  body.classList.add('fit')
}

// Re-fits whenever the preview frame's box changes (dialog animating in, resize, rotation).
let frameObserver = null
let observedFrame = null
const observeFrame = () => {
  const frame = invoiceFrame.value
  if (!frame || observedFrame === frame) return
  frameObserver?.disconnect()
  frameObserver = new ResizeObserver(() => fitInvoice())
  frameObserver.observe(frame)
  observedFrame = frame
}

const openInvoice = async (spk) => {
  try {
    const response = await api.get(`/api/customer/spk/${spk.id}/invoice`)
    invoiceData.value = response.data?.data
    invoiceDialog.value = true
    await nextTick()
    writeInvoice()
    observeFrame()
    // Fit immediately, then again once the maximized dialog finishes animating in. ResizeObserver
    // keeps it correct afterwards (window resize, rotation, ...).
    requestAnimationFrame(fitInvoice)
    setTimeout(fitInvoice, 300)
  } catch (e) {
    if (e.response?.status === 404) {
      $q.notify({ type: 'info', message: t('pages.spk.noInvoice') })
    } else {
      $q.notify({ type: 'negative', message: t('pages.spk.invoiceFailed') })
    }
  }
}

// The browser print dialog is the "download as PDF" path: the customer picks "Save as PDF" there.
const downloadPdf = () => {
  const frame = invoiceFrame.value
  if (!frame) return
  setTimeout(() => {
    frame.contentWindow.focus()
    frame.contentWindow.print()
  }, 250)
}

const onResize = () => {
  if (invoiceDialog.value) fitInvoice()
}

onMounted(async () => {
  window.addEventListener('resize', onResize)
  try {
    const response = await api.get('/api/customer/spk')
    spks.value = response.data?.data || []
  } catch (e) {
    if (e.response?.status === 400) {
      notLinked.value = true
    } else {
      console.error('Failed to load service records', e)
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.invoice-frame {
  display: block;
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
  overflow: hidden;
}
</style>
