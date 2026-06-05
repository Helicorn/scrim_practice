<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SummonerNotFoundModal from '@/components/SummonerNotFoundModal.vue'
import {
  mapSessionApiErrorMessage,
  refreshSessionRankStats,
  registerSessionPlayers,
} from '@/services/sessionApi'
import { lookupAllSummoners } from '@/services/summonerLookup'
import { useSessionStore } from '@/stores/session'

const router = useRouter()
const session = useSessionStore()

const isSearching = ref(false)
const showLookupModal = ref(false)
const modalTitle = ref('')
const modalMessages = ref<string[]>([])
const showApiKey = ref(false)

const apiKeyLength = computed(() => session.riotApiKey.trim().length)
const hasApiKey = computed(() => apiKeyLength.value > 0)

function toggleApiKeyVisibility() {
  showApiKey.value = !showApiKey.value
}

onMounted(() => {
  session.ensurePlayers()
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

  isSearching.value = true
  try {
    const result = await lookupAllSummoners(
      session.players,
      session.riotApiKey,
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

      await refreshSessionRankStats(session.sessionId, session.riotApiKey)
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
    <p v-if="session.hasActiveSession" class="session-chip text-label">
      {{ session.formatSessionSummary() }}
    </p>
    <p class="text-hint">내전을 진행할 10인의 소환사명과 태그를 입력하세요. (예: Hide on bush # KR1)</p>

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
        <a
          href="https://developer.riotgames.com/"
          target="_blank"
          rel="noopener noreferrer"
          class="api-key-note-link"
        >developer.riotgames.com</a>
        에서 발급 · 개발용 키는 24시간마다 갱신
      </p>
    </div>

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

    <div class="players-actions">
      <button
        type="button"
        class="btn-next"
        :disabled="isSearching"
        @click="onNext"
      >
        {{ isSearching ? '전적·랭크 저장 중…' : '다음 →' }}
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

.player-table {
  --col-index: 2rem;
  --col-sep: 1.25rem;
  --col-tag: 8rem;
  margin-bottom: 1.25rem;
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
