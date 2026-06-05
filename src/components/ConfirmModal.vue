<script setup lang="ts">
defineProps<{
  open: boolean
  title: string
  messages: string[]
  confirmLabel?: string
  cancelLabel?: string
  danger?: boolean
}>()

const emit = defineEmits<{
  close: []
  confirm: []
}>()

function onBackdropClick(event: MouseEvent) {
  if (event.target === event.currentTarget) {
    emit('close')
  }
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="modal-backdrop"
      role="presentation"
      @click="onBackdropClick"
    >
      <div
        class="modal-panel"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-modal-title"
      >
        <h2 id="confirm-modal-title" class="modal-title">{{ title }}</h2>
        <ul class="modal-list">
          <li v-for="(msg, index) in messages" :key="index">{{ msg }}</li>
        </ul>
        <div class="modal-actions">
          <button type="button" class="btn-cancel" @click="emit('close')">
            {{ cancelLabel ?? '취소' }}
          </button>
          <button
            type="button"
            class="btn-confirm"
            :class="{ danger }"
            @click="emit('confirm')"
          >
            {{ confirmLabel ?? '확인' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.55);
}

.modal-panel {
  width: 100%;
  max-width: 22rem;
  padding: 1.25rem;
  border-radius: var(--radius-input);
  border: 1px solid var(--color-input-border);
  background: var(--color-input-bg);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
}

.modal-title {
  margin: 0 0 0.75rem;
  font-size: 1.05rem;
}

.modal-list {
  margin: 0 0 1rem;
  padding-left: 1.15rem;
  line-height: 1.5;
}

.modal-list li + li {
  margin-top: 0.35rem;
}

.modal-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-cancel,
.btn-confirm {
  flex: 1;
  padding: 0.55rem 0.75rem;
  border-radius: var(--radius-input);
  font: inherit;
  cursor: pointer;
}

.btn-cancel {
  border: 1px solid var(--color-input-border);
  background: var(--color-input-bg);
  color: inherit;
}

.btn-cancel:hover {
  border-color: var(--color-accent);
}

.btn-confirm {
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #fff;
}

.btn-confirm:hover {
  filter: brightness(1.08);
}

.btn-confirm.danger {
  border-color: #e84057;
  background: #e84057;
}

.btn-confirm.danger:hover {
  filter: brightness(1.08);
}
</style>
