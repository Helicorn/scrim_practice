<script setup lang="ts">
defineProps<{
  open: boolean
  title?: string
  messages: string[]
}>()

const emit = defineEmits<{
  close: []
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
        :aria-labelledby="title ? 'modal-title' : undefined"
      >
        <h2 id="modal-title" class="modal-title">
          {{ title ?? '전적 검색 안내' }}
        </h2>
        <ul class="modal-list">
          <li v-for="(msg, index) in messages" :key="index">{{ msg }}</li>
        </ul>
        <button type="button" class="modal-close" @click="emit('close')">
          확인
        </button>
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

.modal-close {
  width: 100%;
  padding: 0.55rem 1rem;
  border: 1px solid var(--color-accent);
  border-radius: var(--radius-input);
  background: var(--color-accent);
  color: #fff;
  font: inherit;
  cursor: pointer;
}

.modal-close:hover {
  filter: brightness(1.08);
}
</style>
