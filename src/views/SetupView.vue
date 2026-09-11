<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import ConfirmModal from '@/components/ConfirmModal.vue'
import {
  cancelSession,
  getSessionSnapshot,
  mapCancelSessionApiErrorMessage,
  mapSessionSnapshotApiErrorMessage,
  SessionApiError,
} from '@/services/sessionApi'
import { useSessionStore } from '@/stores/session'
import type { SeriesType } from '@/types/session'
import { SERIES_CONFIG } from '@/types/session'

const router = useRouter()
const session = useSessionStore()

const selectedSeries = ref<SeriesType>(
  session.seriesType ?? 'single',
)
const peerless = ref(session.peerless)

const seriesOptions = (
  Object.entries(SERIES_CONFIG) as [SeriesType, (typeof SERIES_CONFIG)[SeriesType]][]
).map(([value, config]) => ({ value, ...config }))

const hasActiveSession = computed(() => session.hasActiveSession)

const activeSummary = computed(() => session.formatSessionSummary())
const showDismissModal = ref(false)
const resumeCode = ref('')
const actionError = ref('')
const isContinuing = ref(false)
const isDismissing = ref(false)
const isStarting = ref(false)

function openDismissModal() {
  actionError.value = ''
  showDismissModal.value = true
}

function closeDismissModal() {
  if (isDismissing.value) return
  showDismissModal.value = false
}

async function cancelOnServerIfPossible(sessionCode: string): Promise<void> {
  try {
    await cancelSession(sessionCode)
  } catch (error) {
    if (error instanceof SessionApiError && error.status === 404) {
      return
    }
    throw error
  }
}

async function confirmDismissSession() {
  actionError.value = ''
  const code = session.sessionId
  isDismissing.value = true
  try {
    if (code) {
      await cancelOnServerIfPossible(code)
    }
    session.clearActiveSession()
    selectedSeries.value = 'single'
    peerless.value = false
    resumeCode.value = ''
    closeDismissModal()
  } catch (error) {
    actionError.value = mapCancelSessionApiErrorMessage(error)
  } finally {
    isDismissing.value = false
  }
}

async function onStart() {
  actionError.value = ''
  isStarting.value = true
  try {
    if (session.hasActiveSession && session.sessionId) {
      await cancelOnServerIfPossible(session.sessionId)
      session.clearActiveSession()
    }
    session.beginSetup(selectedSeries.value, peerless.value)
    await router.push('/players')
  } catch (error) {
    actionError.value = mapCancelSessionApiErrorMessage(error)
  } finally {
    isStarting.value = false
  }
}

async function resumeByCode(sessionCode: string) {
  const code = sessionCode.trim()
  if (!code) {
    actionError.value = '세션 코드를 입력해 주세요.'
    return
  }

  actionError.value = ''
  isContinuing.value = true
  try {
    const snapshot = await getSessionSnapshot(code)
    const route = session.hydrateFromSnapshot(snapshot)
    selectedSeries.value = snapshot.seriesType
    peerless.value = snapshot.peerless
    resumeCode.value = ''
    await router.push(`/${route}`)
  } catch (error) {
    if (error instanceof SessionApiError && error.status === 404) {
      if (session.sessionId === code) {
        session.clearActiveSession()
        selectedSeries.value = 'single'
        peerless.value = false
      }
    }
    actionError.value = mapSessionSnapshotApiErrorMessage(error)
  } finally {
    isContinuing.value = false
  }
}

async function onContinue() {
  if (!session.sessionId) return
  await resumeByCode(session.sessionId)
}

async function onResumeByCode() {
  await resumeByCode(resumeCode.value)
}
</script>

<template>
  <section class="setup">
    <h1>내전 시작</h1>
    <p class="text-hint">형식과 피어리스 여부를 선택한 뒤 소환사 입력으로 이동합니다.</p>

    <div v-if="hasActiveSession" class="active-session">
      <button
        type="button"
        class="active-dismiss"
        aria-label="진행 중인 내전 삭제"
        :disabled="isDismissing || isContinuing || isStarting"
        @click="openDismissModal"
      />
      <p class="active-label text-label">진행 중인 내전</p>
      <p class="active-summary">{{ activeSummary }}</p>
      <p v-if="session.sessionId" class="active-code text-label">
        세션 코드 · {{ session.sessionId }}
      </p>
      <button
        type="button"
        class="btn-secondary"
        :disabled="isContinuing || isDismissing || isStarting"
        @click="onContinue"
      >
        {{ isContinuing ? '불러오는 중…' : '이어서 진행' }}
      </button>
    </div>

    <div v-else class="resume-panel">
      <p class="active-label text-label">세션 코드로 이어하기</p>
      <p class="text-hint resume-hint">
        다른 브라우저·기기에서 진행 중이던 내전을 DB에서 불러옵니다.
      </p>
      <input
        v-model="resumeCode"
        class="resume-input"
        type="text"
        autocomplete="off"
        spellcheck="false"
        placeholder="세션 코드 (UUID)"
        :disabled="isContinuing || isStarting"
        @keydown.enter.prevent="onResumeByCode"
      />
      <button
        type="button"
        class="btn-secondary"
        :disabled="isContinuing || isStarting || !resumeCode.trim()"
        @click="onResumeByCode"
      >
        {{ isContinuing ? '불러오는 중…' : '코드로 이어하기' }}
      </button>
    </div>

    <p v-if="actionError" class="action-error" role="alert">{{ actionError }}</p>

    <ConfirmModal
      :open="showDismissModal"
      title="진행 중인 내전 삭제"
      :messages="[
        '서버의 진행 중 세션이 취소되고, 이 브라우저의 진행 정보도 삭제됩니다.',
        '이미 저장한 경기 결과·전적 집계는 유지됩니다.',
        '삭제 후에는 새 내전을 시작하거나 세션 코드로 다시 이어갈 수 없습니다.',
      ]"
      confirm-label="삭제"
      cancel-label="취소"
      danger
      @close="closeDismissModal"
      @confirm="confirmDismissSession"
    />

    <fieldset class="setup-fieldset">
      <legend class="fieldset-legend">경기 형식</legend>
      <div class="series-options">
        <label
          v-for="opt in seriesOptions"
          :key="opt.value"
          class="series-option"
          :class="{ selected: selectedSeries === opt.value }"
        >
          <input
            v-model="selectedSeries"
            type="radio"
            name="series"
            :value="opt.value"
          />
          <span class="series-name">{{ opt.label }}</span>
          <span class="series-desc text-label">
            {{ opt.description }}
          </span>
        </label>
      </div>
    </fieldset>

    <label class="peerless-check">
      <input v-model="peerless" type="checkbox" />
      <span>피어리스 (Fearless) — 사용한 챔피언 재사용 불가</span>
    </label>

    <div class="setup-actions">
      <button
        type="button"
        class="btn-primary"
        :disabled="isStarting || isContinuing || isDismissing"
        @click="onStart"
      >
        {{
          isStarting
            ? '시작하는 중…'
            : hasActiveSession
              ? '새 내전 시작'
              : '내전 시작'
        }}
      </button>
    </div>

    <p v-if="hasActiveSession" class="restart-note text-label">
      「새 내전 시작」을 누르면 진행 중이던 내전이 서버에서 취소됩니다.
    </p>
  </section>
</template>

<style scoped>
.setup {
  text-align: left;
  max-width: 28rem;
  margin: 0 auto;
}

.active-session,
.resume-panel {
  position: relative;
  margin-bottom: 1.25rem;
  padding: 0.75rem 2rem 0.85rem 0.85rem;
  border: 1px solid var(--color-accent);
  border-radius: var(--radius-input);
  background: rgba(66, 184, 131, 0.08);
}

.resume-panel {
  padding-right: 0.85rem;
  border-color: var(--color-input-border);
  background: var(--color-input-bg);
}

.active-dismiss {
  position: absolute;
  top: 0.4rem;
  right: 0.4rem;
  width: 1.65rem;
  height: 1.65rem;
  padding: 0;
  border: 1px solid var(--color-input-border);
  border-radius: 50%;
  background: var(--color-input-bg);
  color: var(--color-input-text);
  font-size: 0;
  line-height: 0;
  cursor: pointer;
  transition: border-color 0.15s, background-color 0.15s, color 0.15s;
}

.active-dismiss:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.active-dismiss::before,
.active-dismiss::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0.75rem;
  height: 2px;
  background-color: currentColor;
  border-radius: 1px;
}

.active-dismiss::before {
  transform: translate(-50%, -50%) rotate(45deg);
}

.active-dismiss::after {
  transform: translate(-50%, -50%) rotate(-45deg);
}

.active-dismiss:hover:not(:disabled) {
  border-color: #e84057;
  color: #e84057;
  background: rgba(232, 64, 87, 0.15);
}

.active-label {
  margin: 0 0 0.25rem;
}

.active-summary {
  margin: 0 0 0.35rem;
  font-size: 1rem;
}

.active-code {
  margin: 0 0 0.65rem;
  word-break: break-all;
}

.resume-hint {
  margin: 0 0 0.65rem;
}

.resume-input {
  display: block;
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 0.65rem;
  padding: 0.55rem 0.65rem;
  border: 1px solid var(--color-input-border);
  border-radius: var(--radius-input);
  background: var(--color-input-bg);
  color: inherit;
  font: inherit;
  font-size: 0.9rem;
}

.action-error {
  margin: 0 0 1rem;
  color: #c62828;
  font-size: 0.9rem;
  line-height: 1.4;
}

.setup-fieldset {
  margin: 0 0 1rem;
  padding: 0;
  border: none;
}

.fieldset-legend {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.85rem;
  color: var(--color-text-subtle);
}

.series-options {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.series-option {
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto;
  column-gap: 0.5rem;
  row-gap: 0.1rem;
  align-items: center;
  padding: 0.65rem 0.75rem;
  border: 2px solid var(--color-input-border);
  border-radius: var(--radius-input);
  background: var(--color-input-bg);
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s;
}

.series-option.selected {
  border-color: var(--color-accent);
  background: rgba(66, 184, 131, 0.1);
}

.series-option input {
  grid-row: 1 / 3;
  margin: 0;
  accent-color: var(--color-accent);
}

.series-name {
  font-size: 1rem;
}

.series-desc {
  grid-column: 2;
}

.peerless-check {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
  cursor: pointer;
  line-height: 1.4;
}

.peerless-check input {
  margin-top: 0.2rem;
  accent-color: var(--color-accent);
}

.setup-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.btn-primary,
.btn-secondary {
  width: 100%;
  padding: 0.6rem 1rem;
  border-radius: var(--radius-input);
  font: inherit;
  font-size: 0.95rem;
  cursor: pointer;
  transition: filter 0.2s, background-color 0.2s;
}

.btn-primary {
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  filter: brightness(1.08);
}

.btn-primary:disabled,
.btn-secondary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-secondary {
  border: 1px solid var(--color-input-border);
  background: var(--color-input-bg);
  color: inherit;
}

.btn-secondary:hover:not(:disabled) {
  border-color: var(--color-accent);
}

.restart-note {
  margin: 0.75rem 0 0;
  line-height: 1.4;
}
</style>
