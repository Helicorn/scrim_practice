<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const TEAM_SIZE = 5
const BAN_COUNT = 5
const CHAMPION_COLUMNS = 10
const DRAFT_TIMER_SECONDS = 30
const ddragonDataVersionUrl = 'https://ddragon.leagueoflegends.com/api/versions.json'
const pickBanOrder = [
  { team: 'RED', type: 'BAN' },
  { team: 'BLUE', type: 'BAN' },
  { team: 'RED', type: 'BAN' },
  { team: 'BLUE', type: 'BAN' },
  { team: 'RED', type: 'BAN' },
  { team: 'BLUE', type: 'BAN' },

  { team: 'RED', type: 'PICK' },
  { team: 'BLUE', type: 'PICK' },
  { team: 'BLUE', type: 'PICK' },
  { team: 'RED', type: 'PICK' },
  { team: 'RED', type: 'PICK' },
  { team: 'BLUE', type: 'PICK' },

  { team: 'RED', type: 'BAN' },
  { team: 'BLUE', type: 'BAN' },
  { team: 'RED', type: 'BAN' },
  { team: 'BLUE', type: 'BAN' },

  { team: 'RED', type: 'PICK' },
  { team: 'BLUE', type: 'PICK' },
  { team: 'BLUE', type: 'PICK' },
  { team: 'RED', type: 'PICK' },
] as const

interface DDragonChampion {
  id: string
  name: string
  image: {
    full: string
  }
}

interface DDragonChampionListResponse {
  data: Record<string, DDragonChampion>
}

interface ChampionPortrait {
  id: string
  name: string
  imageUrl: string
}

session.ensurePlayers()
session.ensureTeams()

const championPortraits = ref<ChampionPortrait[]>([])
const isLoadingChampions = ref(false)
const championLoadError = ref('')
const championSearchQuery = ref('')
const isDraftStarted = ref(false)
const remainingSeconds = ref(DRAFT_TIMER_SECONDS)
const currentPhaseIndex = ref(0)
const confirmNotice = ref('')
const selectedChampionId = ref<string | null>(null)
const redBans = ref<Array<ChampionPortrait | null>>(
  Array(BAN_COUNT).fill(null),
)
const blueBans = ref<Array<ChampionPortrait | null>>(
  Array(BAN_COUNT).fill(null),
)
const redPicks = ref<Array<ChampionPortrait | null>>(
  Array(TEAM_SIZE).fill(null),
)
const bluePicks = ref<Array<ChampionPortrait | null>>(
  Array(TEAM_SIZE).fill(null),
)
const redBanCount = ref(0)
const blueBanCount = ref(0)
const redPickCount = ref(0)
const bluePickCount = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const filteredChampionPortraits = computed(() => {
  const query = championSearchQuery.value.trim().toLowerCase()
  if (!query) return championPortraits.value

  return championPortraits.value.filter((champion) => {
    return (
      champion.name.toLowerCase().includes(query) ||
      champion.id.toLowerCase().includes(query)
    )
  })
})

const phaseTimerText = computed(() => {
  return `:${String(remainingSeconds.value).padStart(2, '0')}`
})

const currentOrder = computed(() => pickBanOrder[currentPhaseIndex.value])

const currentPhaseLabelText = computed(() => {
  const order = currentOrder.value
  if (!order) return '밴픽 종료'
  const teamLabel = order.team === 'RED' ? '레드' : '블루'
  const typeLabel = order.type === 'BAN' ? '밴' : '픽'
  return `${teamLabel} ${typeLabel} PHASE`
})

const currentPhaseLabel = computed(() => {
  if (!isDraftStarted.value) return 'BAN / PICK'
  return currentPhaseLabelText.value
})

const phaseHeaderLabel = computed(() => {
  if (!isDraftStarted.value) return 'PICK PHASE'
  return currentPhaseLabelText.value
})

const displayedRedBans = computed(() => [...redBans.value].reverse())
const displayedBlueBans = computed(() => blueBans.value)
const unavailableChampionIds = computed(() => {
  const bannedIds = [...redBans.value, ...blueBans.value]
    .filter((champion): champion is ChampionPortrait => champion !== null)
    .map((champion) => champion.id)
  const pickedIds = [...redPicks.value, ...bluePicks.value]
    .filter((champion): champion is ChampionPortrait => champion !== null)
    .map((champion) => champion.id)
  return new Set([...bannedIds, ...pickedIds])
})

async function loadChampionPortraits() {
  isLoadingChampions.value = true
  championLoadError.value = ''

  try {
    const versionResponse = await fetch(ddragonDataVersionUrl)
    if (!versionResponse.ok) {
      throw new Error(`DDragon 버전 조회 실패 (${versionResponse.status})`)
    }

    const versions = (await versionResponse.json()) as string[]
    const latestVersion = versions[0]
    if (!latestVersion) {
      throw new Error('DDragon 버전 정보를 찾을 수 없습니다.')
    }

    const championListResponse = await fetch(
      `https://ddragon.leagueoflegends.com/cdn/${latestVersion}/data/ko_KR/champion.json`,
    )
    if (!championListResponse.ok) {
      throw new Error(`챔피언 목록 조회 실패 (${championListResponse.status})`)
    }

    const championList =
      (await championListResponse.json()) as DDragonChampionListResponse
    const collator = new Intl.Collator('ko')

    championPortraits.value = Object.values(championList.data)
      .sort((a, b) => collator.compare(a.name, b.name))
      .map((champion) => ({
        id: champion.id,
        name: champion.name,
        imageUrl: `https://ddragon.leagueoflegends.com/cdn/${latestVersion}/img/champion/${champion.image.full}`,
      }))
  } catch (error) {
    console.error(error)
    championLoadError.value =
      '챔피언 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isLoadingChampions.value = false
  }
}

onMounted(() => {
  void loadChampionPortraits()
})

function getSlot(team: 'red' | 'blue', index: number) {
  const list = team === 'red' ? session.redTeam : session.blueTeam
  return list[index]
}

function getPickedChampion(team: 'red' | 'blue', index: number) {
  const picks = team === 'red' ? redPicks.value : bluePicks.value
  return picks[index] ?? null
}

function getChampionById(championId: string): ChampionPortrait | undefined {
  return championPortraits.value.find((champion) => champion.id === championId)
}

function applyBan(team: 'RED' | 'BLUE', championId: string): boolean {
  const champion = getChampionById(championId)
  if (!champion) return false

  if (team === 'RED') {
    if (redBanCount.value >= BAN_COUNT) return false
    redBans.value[redBanCount.value] = champion
    redBanCount.value += 1
    return true
  }

  if (blueBanCount.value >= BAN_COUNT) return false
  blueBans.value[blueBanCount.value] = champion
  blueBanCount.value += 1
  return true
}

function applyPick(team: 'RED' | 'BLUE', championId: string): boolean {
  const champion = getChampionById(championId)
  if (!champion) return false

  if (team === 'RED') {
    if (redPickCount.value >= TEAM_SIZE) return false
    redPicks.value[redPickCount.value] = champion
    redPickCount.value += 1
    return true
  }

  if (bluePickCount.value >= TEAM_SIZE) return false
  bluePicks.value[bluePickCount.value] = champion
  bluePickCount.value += 1
  return true
}

function stopCountdown() {
  if (!countdownTimer) return
  clearInterval(countdownTimer)
  countdownTimer = null
}

function moveToNextPhase(): boolean {
  const isLastPhase = currentPhaseIndex.value >= pickBanOrder.length - 1
  if (isLastPhase) {
    remainingSeconds.value = 0
    stopCountdown()
    return false
  }

  currentPhaseIndex.value += 1
  remainingSeconds.value = DRAFT_TIMER_SECONDS
  selectedChampionId.value = null
  confirmNotice.value = ''
  return true
}

function startDraft() {
  if (isDraftStarted.value) return

  isDraftStarted.value = true
  currentPhaseIndex.value = 0
  remainingSeconds.value = DRAFT_TIMER_SECONDS
  confirmNotice.value = ''
  selectedChampionId.value = null
  redBans.value = Array(BAN_COUNT).fill(null)
  blueBans.value = Array(BAN_COUNT).fill(null)
  redPicks.value = Array(TEAM_SIZE).fill(null)
  bluePicks.value = Array(TEAM_SIZE).fill(null)
  redBanCount.value = 0
  blueBanCount.value = 0
  redPickCount.value = 0
  bluePickCount.value = 0

  stopCountdown()
  countdownTimer = setInterval(() => {
    if (remainingSeconds.value <= 1) {
      moveToNextPhase()
      return
    }
    remainingSeconds.value -= 1
  }, 1000)
}

function confirmBanPick() {
  if (!isDraftStarted.value) return
  if (!selectedChampionId.value) {
    confirmNotice.value = '챔피언을 선택한 뒤 확인을 눌러 주세요.'
    return
  }

  const order = currentOrder.value
  if (!order) return

  const confirmedPhaseLabel = currentPhaseLabel.value
  if (unavailableChampionIds.value.has(selectedChampionId.value)) {
    confirmNotice.value = '이미 밴/픽된 챔피언입니다. 다른 챔피언을 선택해 주세요.'
    selectedChampionId.value = null
    return
  }

  let applied = false
  if (order.type === 'BAN') {
    applied = applyBan(order.team, selectedChampionId.value)
  } else {
    applied = applyPick(order.team, selectedChampionId.value)
  }

  if (!applied) {
    confirmNotice.value = '밴/픽 정보를 저장하지 못했습니다. 다시 시도해 주세요.'
    return
  }

  const moved = moveToNextPhase()
  confirmNotice.value = moved
    ? `${confirmedPhaseLabel} 확정 · 다음 PHASE로 이동`
    : `${confirmedPhaseLabel} 확정 · 밴픽 종료`
}

function onChampionClick(championId: string) {
  if (!isDraftStarted.value) return
  if (unavailableChampionIds.value.has(championId)) return
  selectedChampionId.value =
    selectedChampionId.value === championId ? null : championId
}

onUnmounted(() => {
  stopCountdown()
})
</script>

<template>
  <section class="draft">
    <header class="draft-header">
      <p class="draft-match text-label">내전 · 밴픽</p>
      <div class="draft-phase">
        <span class="phase-label">{{ phaseHeaderLabel }}</span>
        <span class="phase-timer" aria-label="타이머">{{ phaseTimerText }}</span>
      </div>
      <button
        type="button"
        class="btn-start-draft"
        :disabled="isDraftStarted"
        @click="startDraft"
      >
        {{ isDraftStarted ? '밴픽 진행 중' : '밴픽 시작' }}
      </button>
    </header>

    <div class="draft-board">
      <aside class="side side-red" aria-label="레드 팀">
        <div class="team-bar">RED</div>
        <ol class="pick-list">
          <li
            v-for="i in TEAM_SIZE"
            :key="`red-pick-${i - 1}`"
            class="pick-slot"
            :class="{
              filled:
                session.isFilledPlayer(getSlot('red', i - 1)) ||
                !!getPickedChampion('red', i - 1),
            }"
          >
            <div
              v-if="getPickedChampion('red', i - 1)"
              class="pick-champion-art-wrap"
              :aria-label="`${getPickedChampion('red', i - 1)?.name} 픽`"
            >
              <img
                :src="getPickedChampion('red', i - 1)?.imageUrl"
                :alt="`${getPickedChampion('red', i - 1)?.name} 픽 초상화`"
                class="pick-champion-art"
              />
            </div>
            <span v-else class="pick-name">
              {{
                session.isFilledPlayer(getSlot('red', i - 1))
                  ? session.formatPlayerLabel(getSlot('red', i - 1))
                  : '—'
              }}
            </span>
          </li>
        </ol>
      </aside>

      <div class="draft-center">
        <div class="champion-portrait" aria-label="챔피언 초상화 영역">
          <div class="champion-search-wrap">
            <div class="champion-search-row">
              <input
                v-model.trim="championSearchQuery"
                type="text"
                class="champion-search input-field"
                placeholder="챔피언 검색 (예: 아리, ahri)"
                aria-label="챔피언 이름 검색"
              />
              <button
                type="button"
                class="btn-confirm-pick"
                :disabled="!isDraftStarted"
                @click="confirmBanPick"
              >
                확인
              </button>
            </div>
          </div>
          <p v-if="isLoadingChampions" class="portrait-state text-label">
            챔피언 정보를 불러오는 중...
          </p>
          <p v-else-if="championLoadError" class="portrait-state portrait-error">
            {{ championLoadError }}
          </p>
          <p v-else-if="filteredChampionPortraits.length === 0" class="portrait-state text-label">
            검색 결과가 없습니다.
          </p>
          <ul
            v-else
            class="champion-grid"
            :style="{ '--champion-columns': String(CHAMPION_COLUMNS) }"
          >
            <li
              v-for="champion in filteredChampionPortraits"
              :key="champion.id"
              class="champion-cell"
            >
              <img
                :src="champion.imageUrl"
                :alt="`${champion.name} 초상화`"
                class="champion-image"
                :class="{
                  selectable: isDraftStarted,
                  selected: selectedChampionId === champion.id,
                  unavailable: unavailableChampionIds.has(champion.id),
                }"
                loading="lazy"
                @click="onChampionClick(champion.id)"
              />
            </li>
          </ul>
        </div>
      </div>

      <aside class="side side-blue" aria-label="블루 팀">
        <div class="team-bar">BLUE</div>
        <ol class="pick-list">
          <li
            v-for="i in TEAM_SIZE"
            :key="`blue-pick-${i - 1}`"
            class="pick-slot"
            :class="{
              filled:
                session.isFilledPlayer(getSlot('blue', i - 1)) ||
                !!getPickedChampion('blue', i - 1),
            }"
          >
            <div
              v-if="getPickedChampion('blue', i - 1)"
              class="pick-champion-art-wrap"
              :aria-label="`${getPickedChampion('blue', i - 1)?.name} 픽`"
            >
              <img
                :src="getPickedChampion('blue', i - 1)?.imageUrl"
                :alt="`${getPickedChampion('blue', i - 1)?.name} 픽 초상화`"
                class="pick-champion-art"
              />
            </div>
            <span v-else class="pick-name">
              {{
                session.isFilledPlayer(getSlot('blue', i - 1))
                  ? session.formatPlayerLabel(getSlot('blue', i - 1))
                  : '—'
              }}
            </span>
          </li>
        </ol>
      </aside>
    </div>

    <footer class="draft-footer">
      <div class="ban-group ban-red" aria-label="레드 밴">
        <span
          v-for="(ban, index) in displayedRedBans"
          :key="`red-ban-${index}`"
          class="ban-slot"
        >
          <img
            v-if="ban"
            :src="ban.imageUrl"
            :alt="`${ban.name} 밴 초상화`"
            class="ban-portrait"
          />
        </span>
      </div>
      <p class="footer-title">{{ currentPhaseLabel }}</p>
      <div class="ban-group ban-blue" aria-label="블루 밴">
        <span
          v-for="(ban, index) in displayedBlueBans"
          :key="`blue-ban-${index}`"
          class="ban-slot"
        >
          <img
            v-if="ban"
            :src="ban.imageUrl"
            :alt="`${ban.name} 밴 초상화`"
            class="ban-portrait"
          />
        </span>
      </div>
    </footer>

    <nav class="draft-nav">
      <RouterLink to="/teams">← 팀 배치</RouterLink>
      <RouterLink to="/result">경기 결과로 →</RouterLink>
    </nav>
  </section>
</template>

<style scoped>
.draft {
  --draft-red: #e84057;
  --draft-red-dim: rgba(232, 64, 87, 0.15);
  --draft-blue: #00a8cc;
  --draft-blue-dim: rgba(0, 168, 204, 0.15);
  --draft-panel: #161616;
  --draft-panel-border: rgba(255, 255, 255, 0.08);

  width: 100%;
  max-width: 72rem;
  margin: 0 auto;
  text-align: left;
}

.draft-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  margin-bottom: 0.75rem;
}

.draft-match {
  margin: 0;
  letter-spacing: 0.04em;
}

.draft-phase {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
}

.phase-label {
  font-size: 0.7rem;
  letter-spacing: 0.12em;
  color: var(--color-text-subtle);
}

.phase-timer {
  display: inline-block;
  min-width: 4.5rem;
  padding: 0.2rem 0.75rem;
  font-size: 1.35rem;
  text-align: center;
  color: #fff;
  background: var(--draft-blue);
  border-radius: 2px;
}

.btn-start-draft {
  padding: 0.45rem 1rem;
  border: 1px solid var(--color-accent);
  border-radius: var(--radius-input);
  background: var(--color-accent);
  color: #fff;
  font: inherit;
  font-size: 0.9rem;
  cursor: pointer;
  transition: filter 0.2s, opacity 0.2s;
}

.btn-start-draft:hover:not(:disabled) {
  filter: brightness(1.08);
}

.btn-start-draft:disabled {
  opacity: 0.65;
  cursor: default;
}

.draft-board {
  display: grid;
  grid-template-columns: minmax(7rem, 9rem) 1fr minmax(7rem, 9rem);
  gap: 0.5rem;
  height: 26rem;
  min-height: 26rem;
  background: var(--draft-panel);
  border: 1px solid var(--draft-panel-border);
}

.side {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.team-bar {
  flex-shrink: 0;
  padding: 0.35rem 0.5rem;
  font-size: 0.8rem;
  letter-spacing: 0.15em;
  text-align: center;
  color: #fff;
}

.side-red .team-bar {
  background: var(--draft-red);
}

.side-blue .team-bar {
  background: var(--draft-blue);
}

.pick-list {
  list-style: none;
  margin: 0;
  padding: 0.25rem;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-height: 0;
}

.pick-slot {
  position: relative;
  flex: 1;
  min-height: 3.5rem;
  display: flex;
  align-items: flex-end;
  padding: 0.35rem 0.45rem;
  background: rgba(0, 0, 0, 0.45);
  border: 1px solid var(--draft-panel-border);
}

.side-red .pick-slot {
  border-left: 3px solid var(--draft-red);
}

.side-blue .pick-slot {
  border-right: 3px solid var(--draft-blue);
  justify-content: flex-end;
}

.pick-slot.filled {
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.2) 0%,
    var(--draft-red-dim) 100%
  );
}

.side-blue .pick-slot.filled {
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.2) 0%,
    var(--draft-blue-dim) 100%
  );
}

.pick-name {
  font-size: 0.72rem;
  line-height: 1.2;
  word-break: break-all;
  color: rgba(255, 255, 255, 0.9);
}

.side-blue .pick-name {
  text-align: right;
}

.pick-champion-art-wrap {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.pick-champion-art {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transform: scale(1.22);
  transform-origin: center;
  filter: saturate(1.06) contrast(1.04);
}

.draft-center {
  display: flex;
  min-height: 0;
  padding: 0.35rem;
  background: #0d0d0d;
}

.champion-portrait {
  flex: 1;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.35rem;
  background: #000;
  border: 1px solid rgba(255, 255, 255, 0.06);
  overflow-y: auto;
}

.champion-search-wrap {
  position: sticky;
  top: 0;
  z-index: 1;
  padding-bottom: 0.1rem;
  background: #000;
}

.champion-search-row {
  display: flex;
  gap: 0.35rem;
}

.champion-search {
  flex: 1;
}

.btn-confirm-pick {
  flex-shrink: 0;
  padding: 0 0.8rem;
  border: 1px solid var(--color-accent);
  border-radius: var(--radius-input);
  background: var(--color-accent);
  color: #fff;
  font: inherit;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-confirm-pick:disabled {
  opacity: 0.6;
  cursor: default;
}

.confirm-notice {
  margin: 0.25rem 0 0;
  color: var(--color-accent);
}

.portrait-state {
  margin: 0;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.portrait-error {
  color: #ff8f98;
}

.champion-grid {
  --champion-columns: 10;
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(var(--champion-columns), minmax(0, 1fr));
  gap: 0.2rem;
}

.champion-cell {
  margin: 0;
}

.champion-image {
  display: block;
  width: 80%;
  height: auto;
  margin: 0 auto;
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
}

.champion-image.selectable {
  cursor: pointer;
}

.champion-image.selectable:hover {
  transform: translateY(-1px);
}

.champion-image.unavailable {
  filter: grayscale(1) brightness(0.7);
  opacity: 0.75;
  cursor: not-allowed;
  transform: none;
}

.champion-image.selected {
  border-color: #d9f9ff;
  box-shadow: 0 0 0 2px rgba(217, 249, 255, 0.95);
}

.draft-footer {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 1rem;
  margin-top: 0.5rem;
  padding: 0.5rem 0;
}

.ban-group {
  display: flex;
  gap: 0.35rem;
}

.ban-red {
  justify-content: flex-start;
}

.ban-blue {
  justify-content: flex-end;
}

.ban-slot {
  width: 2.5rem;
  height: 2.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.55);
  border: 1px solid var(--draft-panel-border);
  transform: skewX(-8deg);
}

.ban-portrait {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transform: skewX(8deg);
}

.ban-red .ban-slot {
  border-color: rgba(232, 64, 87, 0.35);
}

.ban-blue .ban-slot {
  border-color: rgba(0, 168, 204, 0.35);
}

.footer-title {
  margin: 0;
  font-size: 0.75rem;
  letter-spacing: 0.2em;
  color: var(--color-text-faint);
  text-align: center;
}

.draft-nav {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
  padding-top: 0.5rem;
}

@media (max-width: 640px) {
  .draft-board {
    grid-template-columns: 5.5rem 1fr 5.5rem;
    height: 22rem;
    min-height: 22rem;
  }

  .pick-name {
    font-size: 0.65rem;
  }

  .ban-slot {
    width: 2rem;
    height: 2rem;
  }
}
</style>
