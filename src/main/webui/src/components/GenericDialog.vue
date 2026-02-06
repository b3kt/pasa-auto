<template>
    <q-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" persistent
        :maximized="$q.screen.lt.sm"
        :position="position === 'drawer' ? 'right' : 'standard'"
        :full-height="position === 'drawer'"
        :transition-show="position === 'drawer' ? 'slide-left' : ($q.screen.lt.sm ? 'slide-up' : 'scale')"
        :transition-hide="position === 'drawer' ? 'slide-right' : ($q.screen.lt.sm ? 'slide-down' : 'scale')"
        @keydown="handleKeydown">
        <q-card :style="cardStyle"
            :class="['column', { 'full-height no-border-radius': position === 'drawer' || $q.screen.lt.sm }]">
            <slot name="header">
                <q-card-section class="row items-center q-pb-none">
                    <slot name="title">
                        <div class="text-h6">{{ title }}</div>
                    </slot>
                    <q-space />
                    <q-btn icon="close" flat round dense v-close-popup />
                </q-card-section>
            </slot>

            <q-card-section :class="[contentClass, { 'col scroll': $q.screen.lt.sm }]">
                <slot></slot>
            </q-card-section>

            <q-card-actions align="right" v-if="$slots.actions" class="bg-grey-1">
                <slot name="actions"></slot>
            </q-card-actions>
        </q-card>
    </q-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { useQuasar } from 'quasar'

const $q = useQuasar()

const props = defineProps({
    modelValue: {
        type: Boolean,
        required: true
    },
    title: {
        type: String,
        default: ''
    },
    minWidth: {
        type: String,
        default: '500px'
    },
    maxWidth: {
        type: String,
        default: '80vw'
    },
    contentClass: {
        type: String,
        default: ''
    },
    position: {
        type: String,
        default: 'drawer',
        validator: (val) => ['standard', 'drawer'].includes(val)
    }
})

const emit = defineEmits(['update:modelValue'])

const cardStyle = computed(() => {
    if ($q.screen.lt.sm) return ''
    if (props.position === 'drawer') {
        return `width: ${props.minWidth}; max-width: ${props.maxWidth}; height: 100%`
    }
    return `min-width: ${props.minWidth}; max-width: ${props.maxWidth}`
})

// Handle Escape key to close dialog
const handleKeydown = (event) => {
    if (event.key === 'Escape') {
        emit('update:modelValue', false)
    } else if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
        // Find submit button inside this dialog and click it
        const dialogEl = event.currentTarget
        const submitBtn = dialogEl.querySelector('button[type="submit"]')
        if (submitBtn) submitBtn.click()
    }
}
</script>
