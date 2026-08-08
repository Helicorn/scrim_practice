<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SummonerNotFoundModal from '@/components/SummonerNotFoundModal.vue'
import {
  mapSessionApiErrorMessage,
  refreshSessionRankStats,
  registerSessionPlayers,
} from '@/services/sessionApi'
import {
  applyDbSummonerHints,
  checkRiotNeeds,
  fetchSavedSummoners,
  formatRiotKeyRequiredMessage,
  formatSavedSummonerLabel,
  formatSavedSummonerRank,
  savedSummonerKey,
  type SavedSummoner,
} from '@/services/summonerApi'
import {
  lookupSummonersAccount,
  validatePlayerInputs,
} from '@/services/summonerLookup'
import { useSessionStore } from '@/stores/session'
import type { Player } from '@/types/session'

const DRAG_MIME = 'application/x-civilwar-summoner'

const router = useRouter()
const session = useSessionStore()

const isSearching = ref(false)
const showLookupModal = ref(false)
const modalTitle = ref('')
const modalMessages = ref<string[]>([])
const showApiKey = ref(false)

const savedSummoners = ref<SavedSummoner[]>([])
const savedListLoading = ref(false)
const savedListError = ref<string | null>(null)
const dragOverIndex = ref<number | null>(null)

const apiKeyLength = computed(() => session.riotApiKey.trim().length)
const hasApiKey = computed(() => apiKeyLength.value > 0)

const usedSummonerKeys = computed(() => {
  const keys = new Set<string>()
  for (const player of session.players) {
    const name = player.gameName.trim()
    const tag = player.tagLine.trim()
    if (name && tag) {
      keys.add(`${name.toLowerCase()}#${tag.toLowerCase()}`)
    }
  }
  return keys
})

function toggleApiKeyVisibility() {
  showApiKey.value = !showApiKey.value
}

function playerRiotKey(player: Player): string {
  return `${player.gameName.trim().toLowerCase()}#${player.tagLine.trim().toLowerCase()}`
}

function isSummonerUsed(s: SavedSummoner): boolean {
  return usedSummonerKeys.value.has(savedSummonerKey(s))
}

function isSlotEmpty(index: number): boolean {
  const p = session.players[index]
  return !p?.gameName.trim()
}

function findFirstEmptySlotIndex(): number | null {
  const idx = session.players.findIndex((p) => !p.gameName.trim())
  return idx >= 0 ? idx : null
}

function assignSummonerToSlot(s: SavedSummoner, slotIndex: number): boolean {
  if (isSummonerUsed(s) && playerRiotKey(session.players[slotIndex]) !== savedSummonerKey(s)) {
    return false
  }
  const player = session.players[slotIndex]
  player.gameName = s.gameName
  player.tagLine = s.tagLine
  player.summonerId = s.summonerId
  if (s.puuid) {
    player.puuid = s.puuid
  }
  return true
}

function onSavedSummonerClick(s: SavedSummoner) {
  if (isSummonerUsed(s)) return
  const slot = findFirstEmptySlotIndex()
  if (slot === null) {
    openModal('입력 칸 부족', ['빈 칸이 없습니다. 기존 칸을 비운 뒤 다시 선택해 주세요.'])
    return
  }
  assignSummonerToSlot(s, slot)
}

function onDragStart(event: DragEvent, s: SavedSummoner) {
  if (isSummonerUsed(s)) {
    event.preventDefault()
    return
  }
  event.dataTransfer?.setData(DRAG_MIME, JSON.stringify(s))
  event.dataTransfer!.effectAllowed = 'copy'
}

function onDragOver(event: DragEvent, index: number) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy'
  }
  dragOverIndex.value = index
}

function onDragLeave(index: number) {
  if (dragOverIndex.value === index) {
    dragOverIndex.value = null
  }
}

function parseDraggedSummoner(event: DragEvent): SavedSummoner | null {
  const raw = event.dataTransfer?.getData(DRAG_MIME)
  if (!raw) return null
  try {
    return JSON.parse(raw) as SavedSummoner
  } catch {
    return null
  }
}

function onDrop(event: DragEvent, index: number) {
  event.preventDefault()
  dragOverIndex.value = null
  const s = parseDraggedSummoner(event)
  if (!s) return
  if (isSummonerUsed(s) && playerRiotKey(session.players[index]) !== savedSummonerKey(s)) {
    openModal('중복', [`${formatSavedSummonerLabel(s)} 은(는) 이미 입력 목록에 있습니다.`])
    return
  }
  assignSummonerToSlot(s, index)
}

async function loadSavedSummoners() {
  savedListLoading.value = true
  savedListError.value = null
  try {
    savedSummoners.value = await fetchSavedSummoners()
  } catch (error) {
    savedSummoners.value = []
    savedListError.value = mapSessionApiErrorMessage(error)
  } finally {
    savedListLoading.value = false
  }
}

onMounted(() => {
  session.ensurePlayers()
  loadSavedSummoners()
})

function openModal(title: string, messages: string[]) {
  modalTitle.value = title
  modalMessages.value = messages
  showLookupModal.value = true
}

function closeModal() {
  showLookupModal.value = false
}

async function onNext() {
  session.ensurePlayers()

  if (!session.sessionId || !session.seriesType) {
    openModal('세션 없음', ['진입 페이지에서 내전을 먼저 시작해 주세요.'])
    return
  }

  const inputErrors = validatePlayerInputs(session.players)
  if (inputErrors.length > 0) {
    openModal('입력 확인', inputErrors)
    return
  }

  isSearching.value = true
  try {
    let riotCheck
    try {
      riotCheck = await checkRiotNeeds(session.players)
    } catch (error) {
      openModal('확인 실패', [mapSessionApiErrorMessage(error)])
      return
    }

    applyDbSummonerHints(session.players, riotCheck.players)

    if (riotCheck.needsRiotKey && !hasApiKey.value) {
      openModal('Riot API Key 필요', [formatRiotKeyRequiredMessage(riotCheck.players)])
      return
    }

    const accountSlots = riotCheck.players
      .filter((s) => s.needsAccountLookup)
      .map((s) => s.slotIndex)

    if (accountSlots.length > 0) {
      const result = await lookupSummonersAccount(
        session.players,
        session.riotApiKey,
        accountSlots,
      )

      if (result.validationErrors.length > 0) {
        openModal('입력 확인', result.validationErrors)
        return
      }

      if (result.notFound.length > 0) {
        openModal('전적을 찾을 수 없는 소환사', [
          '아래 소환사는 Riot API에서 조회되지 않았습니다. 이름·태그를 확인해 주세요.',
          ...result.notFound,
        ])
        return
      }
    }

    const needsRankRefresh = riotCheck.players.some((s) => s.needsRankRefresh)

    try {
      const saved = await registerSessionPlayers(
        session.sessionId,
        session.seriesType,
        session.peerless,
        session.players,
      )
      const idByRiotId = new Map(
        saved.summoners.map((s) => [
          `${s.gameName.trim().toLowerCase()}#${s.tagLine.trim().toLowerCase()}`,
          s.summonerId,
        ]),
      )
      for (const player of session.players) {
        const key = `${player.gameName.trim().toLowerCase()}#${player.tagLine.trim().toLowerCase()}`
        const summonerId = idByRiotId.get(key)
        if (summonerId !== undefined) {
          player.summonerId = summonerId
        }
      }

      if (needsRankRefresh && hasApiKey.value) {
        await refreshSessionRankStats(session.sessionId, session.riotApiKey)
      }
      await loadSavedSummoners()
      if (!session.commitActiveSession()) {
        openModal('세션 저장 실패', [
          '로스터가 확정되지 않아 진행 중 내전으로 저장하지 못했습니다.',
        ])
        return
      }
    } catch (error) {
      openModal('DB 저장 실패', [mapSessionApiErrorMessage(error)])
      return
    }

    router.push('/teams')
  } finally {
    isSearching.value = false
  }
}
</script>

<template>
  <section class="players">
    <h1>소환사 입력</h1>
    <p v-if="session.seriesType" class="session-chip text-label">
      {{ session.formatSessionSummary() }}
    </p>
    <p class="text-hint">
      내전을 진행할 10인의 소환사명과 태그를 입력하세요. 오른쪽 저장 목록에서
      <strong>드래그</strong>하거나 <strong>클릭</strong>해 빈 칸에 넣을 수 있습니다.
    </p>

    <div class="api-key-row">
      <label class="api-key-label" for="riot-api-key">Riot API Key</label>
      <div class="api-key-controls">
        <div class="api-key-input-wrap">
          <input
            id="riot-api-key"
            v-model="session.riotApiKey"
            :type="showApiKey ? 'text' : 'password'"
            class="input-field api-key-input"
            placeholder="RGAPI-..."
            autocomplete="off"
            spellcheck="false"
          />
          <button
            type="button"
            class="api-key-toggle"
            :aria-label="showApiKey ? 'API Key 숨기기' : 'API Key 보기'"
            @click="toggleApiKeyVisibility"
          >
            {{ showApiKey ? '숨기기' : '보기' }}
          </button>
        </div>
        <a
          href="https://developer.riotgames.com/"
          target="_blank"
          rel="noopener noreferrer"
          class="api-key-link"
        >
          키 발급 ↗
        </a>
      </div>
      <p class="api-key-status text-label" :class="{ filled: hasApiKey }">
        <template v-if="hasApiKey">
          키 입력됨 · {{ apiKeyLength }}자
          <span v-if="!showApiKey"> (「보기」로 내용 확인)</span>
        </template>
        <template v-else>키 미입력</template>
      </p>
      <p class="api-key-note text-label">
        <strong>신규 소환사</strong> 또는 <strong>내전 0판</strong>일 때만 Key가 필요합니다.
        저장된 내전 단골만 채우면 생략 가능.
        <a
          href="https://developer.riotgames.com/"
          target="_blank"
          rel="noopener noreferrer"
          class="api-key-note-link"
        >developer.riotgames.com</a>
        · 개발용 키는 24시간마다 갱신
      </p>
    </div>

    <div class="players-layout">
      <div class="player-table">
        <div class="player-header text-label" aria-hidden="true">
          <span class="col-index" />
          <span class="col-name">소환사명</span>
          <span class="col-sep" />
          <span class="col-tag">태그</span>
        </div>

        <ol class="player-list">
          <li
            v-for="(player, index) in session.players"
            :key="index"
            class="player-row"
            :class="{
              'player-row--drop-target': dragOverIndex === index,
              'player-row--empty': isSlotEmpty(index),
            }"
            @dragover="onDragOver($event, index)"
            @dragleave="onDragLeave(index)"
            @drop="onDrop($event, index)"
          >
            <span class="player-index">{{ index + 1 }}</span>
            <input
              v-model="player.gameName"
              type="text"
              class="input-field"
              autocomplete="off"
              aria-label="소환사명"
              placeholder="소환사명"
            />
            <span class="separator" aria-hidden="true">#</span>
            <input
              v-model="player.tagLine"
              type="text"
              class="input-field"
              autocomplete="off"
              aria-label="태그"
              placeholder="KR1"
            />
          </li>
        </ol>
      </div>

      <aside class="saved-panel" aria-label="저장된 소환사 목록">
        <div class="saved-panel-header">
          <h2 class="saved-panel-title">저장된 소환사</h2>
          <button
            type="button"
            class="saved-refresh-btn"
            :disabled="savedListLoading"
            title="목록 새로고침"
            @click="loadSavedSummoners"
          >
            ↻
          </button>
        </div>
        <p class="saved-panel-hint text-label">
          DB에 등록된 소환사 · 클릭 시 다음 빈 칸 · 드래그로 원하는 칸에 놓기
        </p>

        <p v-if="savedListLoading" class="saved-panel-status text-label">불러오는 중…</p>
        <p v-else-if="savedListError" class="saved-panel-status saved-panel-status--error">
          {{ savedListError }}
        </p>
        <p v-else-if="savedSummoners.length === 0" class="saved-panel-status text-label">
          아직 저장된 소환사가 없습니다.
        </p>

        <ul v-else class="saved-list">
          <li
            v-for="s in savedSummoners"
            :key="s.summonerId"
            class="saved-item"
            :class="{
              'saved-item--used': isSummonerUsed(s),
              'saved-item--draggable': !isSummonerUsed(s),
            }"
            :draggable="!isSummonerUsed(s)"
            :title="
              isSummonerUsed(s)
                ? '이미 입력 목록에 있음'
                : '클릭: 빈 칸에 추가 · 드래그: 원하는 칸에 놓기'
            "
            @click="onSavedSummonerClick(s)"
            @dragstart="onDragStart($event, s)"
          >
            <span class="saved-item-name">{{ formatSavedSummonerLabel(s) }}</span>
            <span v-if="formatSavedSummonerRank(s)" class="saved-item-rank text-label">
              {{ formatSavedSummonerRank(s) }}
            </span>
            <span v-if="isSummonerUsed(s)" class="saved-item-badge">입력됨</span>
          </li>
        </ul>
      </aside>
    </div>

    <div class="players-actions">
      <button
        type="button"
        class="btn-next"
        :disabled="isSearching"
        @click="onNext"
      >
        {{ isSearching ? '저장 중…' : '다음 →' }}
      </button>
    </div>

    <SummonerNotFoundModal
      :open="showLookupModal"
      :title="modalTitle"
      :messages="modalMessages"
      @close="closeModal"
    />
  </section>
</template>

<style scoped>
.players {
  text-align: left;
}

.session-chip {
  margin: 0 0 0.5rem;
  padding: 0.35rem 0.6rem;
  display: inline-block;
  border-radius: var(--radius-input);
  border: 1px solid var(--color-input-border);
  background: rgba(66, 184, 131, 0.08);
}

.api-key-row {
  margin-bottom: 1.25rem;
  padding: 0.75rem 0.85rem;
  border: 1px solid var(--color-input-border);
  border-radius: var(--radius-input);
  background: var(--color-input-bg);
}

.api-key-label {
  display: block;
  margin-bottom: 0.4rem;
  font-size: 0.85rem;
}

.api-key-controls {
  display: flex;
  align-items: stretch;
  gap: 0.5rem;
}

.api-key-input-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: stretch;
  gap: 0.35rem;
}

.api-key-input {
  flex: 1;
  min-width: 0;
  font-family: ui-monospace, 'Cascadia Code', Consolas, monospace;
  font-size: 0.85rem;
  letter-spacing: 0.02em;
}

.api-key-toggle {
  flex-shrink: 0;
  padding: 0 0.65rem;
  border-radius: var(--radius-input);
  border: 1px solid var(--color-input-border);
  background: var(--color-input-bg);
  color: var(--color-input-text);
  font: inherit;
  font-size: 0.8rem;
  cursor: pointer;
  white-space: nowrap;
}

.api-key-toggle:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.api-key-status {
  margin: 0.4rem 0 0;
  line-height: 1.4;
}

.api-key-status.filled {
  color: var(--color-accent);
}

.api-key-link {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  padding: 0 0.85rem;
  border-radius: var(--radius-input);
  border: 1px solid var(--color-accent);
  color: var(--color-accent);
  font-size: 0.85rem;
  text-decoration: none;
  white-space: nowrap;
  transition: background-color 0.2s, color 0.2s;
}

.api-key-link:hover {
  background: var(--color-accent);
  color: #fff;
}

.api-key-note {
  margin: 0.45rem 0 0;
  line-height: 1.4;
}

.api-key-note-link {
  color: inherit;
}

.players-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(12rem, 16rem);
  gap: 1rem 1.25rem;
  align-items: start;
  margin-bottom: 1.25rem;
}

@media (max-width: 720px) {
  .players-layout {
    grid-template-columns: 1fr;
  }
}

.player-table {
  --col-index: 2rem;
  --col-sep: 1.25rem;
  --col-tag: 8rem;
  min-width: 0;
}

.player-header,
.player-row {
  display: grid;
  grid-template-columns: var(--col-index) 1fr var(--col-sep) var(--col-tag);
  align-items: center;
  gap: 0 0.75rem;
}

.player-header {
  margin-bottom: 0.35rem;
}

.player-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.player-row {
  border-radius: var(--radius-input);
  padding: 0.15rem 0.25rem;
  transition: background-color 0.15s, box-shadow 0.15s;
}

.player-row--empty {
  border: 1px dashed transparent;
}

.player-row--drop-target {
  background: rgba(66, 184, 131, 0.12);
  box-shadow: inset 0 0 0 2px var(--color-accent);
}

.player-index {
  font-weight: 600;
  color: var(--color-accent);
  text-align: center;
  line-height: 1;
}

.separator {
  text-align: center;
  font-weight: 600;
  color: var(--color-text-faint);
  line-height: 1;
}

.saved-panel {
  border: 1px solid var(--color-input-border);
  border-radius: var(--radius-input);
  background: var(--color-input-bg);
  padding: 0.75rem 0.65rem;
  max-height: min(28rem, 70vh);
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.saved-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.saved-panel-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
}

.saved-refresh-btn {
  flex-shrink: 0;
  width: 1.75rem;
  height: 1.75rem;
  padding: 0;
  border-radius: var(--radius-input);
  border: 1px solid var(--color-input-border);
  background: transparent;
  color: var(--color-input-text);
  font-size: 1rem;
  line-height: 1;
  cursor: pointer;
}

.saved-refresh-btn:hover:not(:disabled) {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.saved-refresh-btn:disabled {
  opacity: 0.5;
  cursor: wait;
}

.saved-panel-hint {
  margin: 0 0 0.5rem;
  line-height: 1.35;
  font-size: 0.75rem;
}

.saved-panel-status {
  margin: 0.5rem 0;
  font-size: 0.85rem;
}

.saved-panel-status--error {
  color: #c0392b;
}

.saved-list {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.saved-item {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.25rem 0.4rem;
  padding: 0.45rem 0.5rem;
  border-radius: calc(var(--radius-input) - 2px);
  border: 1px solid var(--color-input-border);
  background: var(--color-bg, #fff);
  font-size: 0.85rem;
  line-height: 1.3;
}

.saved-item--draggable {
  cursor: grab;
  user-select: none;
}

.saved-item--draggable:hover {
  border-color: var(--color-accent);
  background: rgba(66, 184, 131, 0.06);
}

.saved-item--draggable:active {
  cursor: grabbing;
}

.saved-item--used {
  opacity: 0.55;
  cursor: default;
  background: transparent;
}

.saved-item-name {
  font-weight: 500;
  word-break: break-all;
}

.saved-item-rank {
  font-size: 0.75rem;
  color: var(--color-text-faint);
}

.saved-item-badge {
  margin-left: auto;
  font-size: 0.7rem;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  background: rgba(66, 184, 131, 0.15);
  color: var(--color-accent);
}

.players-actions {
  display: flex;
  justify-content: flex-end;
}

.btn-next {
  padding: 0.55rem 1.35rem;
  border-radius: var(--radius-input);
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #fff;
  font: inherit;
  font-size: 0.95rem;
  cursor: pointer;
  transition: filter 0.2s;
}

.btn-next:hover:not(:disabled) {
  filter: brightness(1.08);
}

.btn-next:disabled {
  opacity: 0.65;
  cursor: wait;
}
</style>
