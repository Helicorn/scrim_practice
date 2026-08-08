<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  mapPickBanApiErrorMessage,
  saveSessionPickBan,
  type PickBanTurnInput,
} from '@/services/sessionApi'
import { matchesChampionSearch } from '@/data/championSearch'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const router = useRouter()
const TEAM_SIZE = 5
const BAN_COUNT = 5
const CHAMPION_COLUMNS = 10
const DRAFT_TIMER_SECONDS = 30
const SWAP_TIMER_SECONDS = 60
const POSITION_LABELS = ['탑', '정글', '미드', '원딜', '서포터'] as const
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

  { team: 'BLUE', type: 'BAN' },
  { team: 'RED', type: 'BAN' },
  { team: 'BLUE', type: 'BAN' },
  { team: 'RED', type: 'BAN' },

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

/** 시간 초과로 밴하지 않음 (밴 칸에 X 표시) */
type BanSlot = ChampionPortrait | 'skipped' | null

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
const redBans = ref<BanSlot[]>(Array(BAN_COUNT).fill(null))
const blueBans = ref<BanSlot[]>(Array(BAN_COUNT).fill(null))
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
const redPickOrder = ref<ChampionPortrait[]>([])
const bluePickOrder = ref<ChampionPortrait[]>([])
const isSavingPickBan = ref(false)
const pickBanSavedMatchNo = ref<number | null>(null)
const isSwapPhaseActive = ref(false)
const isSwapPhaseComplete = ref(false)
const swapRemainingSeconds = ref(0)
const selectedSwapSlot = ref<{ team: 'red' | 'blue'; index: number } | null>(
  null,
)
let countdownTimer: ReturnType<typeof setInterval> | null = null
let swapCountdownTimer: ReturnType<typeof setInterval> | null = null

const filteredChampionPortraits = computed(() => {
  const query = championSearchQuery.value.trim()
  if (!query) return championPortraits.value

  return championPortraits.value.filter((champion) =>
    matchesChampionSearch(champion, query),
  )
})

const phaseTimerText = computed(() => {
  const seconds = isSwapPhaseActive.value
    ? swapRemainingSeconds.value
    : remainingSeconds.value
  return `:${String(seconds).padStart(2, '0')}`
})

const currentOrder = computed(() => pickBanOrder[currentPhaseIndex.value])

const currentPhaseLabelText = computed(() => {
  const order = currentOrder.value
  if (!order) return '밴픽 종료'
  const teamLabel = order.team === 'RED' ? '레드' : '블루'
  const typeLabel = order.type === 'BAN' ? '밴' : '픽'
  return `${teamLabel} ${typeLabel} PHASE`
})

const isDraftComplete = computed(() => {
  return (
    isDraftStarted.value &&
    currentPhaseIndex.value >= pickBanOrder.length - 1 &&
    remainingSeconds.value === 0
  )
})

const canProceedToResult = computed(
  () => isDraftComplete.value && isSwapPhaseComplete.value,
)

const currentPhaseLabel = computed(() => {
  if (!isDraftStarted.value) return 'BAN / PICK'
  if (isSwapPhaseActive.value) return '챔피언 스왑 PHASE'
  if (isDraftComplete.value) return '밴픽 종료'
  return currentPhaseLabelText.value
})

const phaseHeaderLabel = computed(() => {
  if (!isDraftStarted.value) return 'PICK PHASE'
  if (isSwapPhaseActive.value) return '챔피언 스왑'
  if (isDraftComplete.value) return '밴픽 종료'
  return currentPhaseLabelText.value
})

const displayedRedBans = computed(() => [...redBans.value].reverse())
const displayedBlueBans = computed(() => blueBans.value)
function isBanChampion(ban: BanSlot): ban is ChampionPortrait {
  return ban !== null && ban !== 'skipped'
}

const unavailableChampionIds = computed(() => {
  const bannedIds = [...redBans.value, ...blueBans.value]
    .filter(isBanChampion)
    .map((champion) => champion.id)
  const pickedIds = [...redPicks.value, ...bluePicks.value]
    .filter((champion): champion is ChampionPortrait => champion !== null)
    .map((champion) => champion.id)
  const peerlessBlocked = session.getPeerlessBlockedChampionIds()
  return new Set([...bannedIds, ...pickedIds, ...peerlessBlocked])
})

const peerlessBlockedCount = computed(
  () => session.getPeerlessBlockedChampionIds().length,
)

function isPeerlessBlockedChampion(championId: string): boolean {
  return session.getPeerlessBlockedChampionIds().includes(championId)
}

function collectUsedChampionIds(): string[] {
  return [
    ...redPicks.value
      .filter((champion): champion is ChampionPortrait => champion !== null)
      .map((champion) => champion.id),
    ...bluePicks.value
      .filter((champion): champion is ChampionPortrait => champion !== null)
      .map((champion) => champion.id),
  ]
}

function toDraftPick(
  champion: ChampionPortrait | null,
): { id: string; name: string; imageUrl: string } | null {
  if (!champion) return null
  return {
    id: champion.id,
    name: champion.name,
    imageUrl: champion.imageUrl,
  }
}

function syncDraftToSession() {
  session.draft = {
    completed: true,
    usedChampionIds: collectUsedChampionIds(),
    redPicks: redPicks.value.map(toDraftPick),
    bluePicks: bluePicks.value.map(toDraftPick),
  }
}

function findSlotForChampion(team: 'RED' | 'BLUE', championId: string): number {
  const picks = team === 'RED' ? redPicks.value : bluePicks.value
  return picks.findIndex((champion) => champion?.id === championId)
}

function getSummonerIdForSlot(
  team: 'RED' | 'BLUE',
  slotIndex: number,
): number | undefined {
  if (slotIndex < 0) return undefined
  const slot = getSlot(team === 'RED' ? 'red' : 'blue', slotIndex)
  return slot.summonerId
}

function buildPickBanTurnsPayload(): PickBanTurnInput[] {
  const turns: PickBanTurnInput[] = []
  let redBanIndex = 0
  let blueBanIndex = 0
  let redPickIndex = 0
  let bluePickIndex = 0

  for (let phaseIndex = 0; phaseIndex < pickBanOrder.length; phaseIndex += 1) {
    const phase = pickBanOrder[phaseIndex]
    const turnNo = phaseIndex + 1

    if (phase.type === 'BAN') {
      const ban =
        phase.team === 'RED'
          ? redBans.value[redBanIndex]
          : blueBans.value[blueBanIndex]
      if (phase.team === 'RED') redBanIndex += 1
      else blueBanIndex += 1

      if (!isBanChampion(ban)) continue

      turns.push({
        turnNo,
        teamColor: phase.team,
        actionType: 'BAN',
        championKey: ban.id,
        championNameKr: ban.name,
        imageUrl: ban.imageUrl,
      })
      continue
    }

    const pick =
      phase.team === 'RED'
        ? redPickOrder.value[redPickIndex]
        : bluePickOrder.value[bluePickIndex]
    if (phase.team === 'RED') redPickIndex += 1
    else bluePickIndex += 1

    if (!pick) continue

    const slotIndex = findSlotForChampion(phase.team, pick.id)
    const summonerId = getSummonerIdForSlot(phase.team, slotIndex)

    turns.push({
      turnNo,
      teamColor: phase.team,
      actionType: 'PICK',
      championKey: pick.id,
      championNameKr: pick.name,
      imageUrl: pick.imageUrl,
      summonerId,
    })
  }

  return turns
}

async function persistPickBanToServer(): Promise<boolean> {
  if (!session.sessionId || !session.committed) return true

  const turns = buildPickBanTurnsPayload()
  if (turns.length === 0) return true

  // 스왑 종료 + 다음 버튼에서 이중 호출될 수 있음
  if (pickBanSavedMatchNo.value === session.currentGame) {
    return true
  }

  isSavingPickBan.value = true
  try {
    const result = await saveSessionPickBan(
      session.sessionId,
      session.currentGame,
      turns,
    )
    pickBanSavedMatchNo.value = result.matchNo
    confirmNotice.value = `밴픽 DB 저장 완료 · ${result.savedTurns}턴 (경기 ${result.matchNo})`
    return true
  } catch (error) {
    confirmNotice.value = mapPickBanApiErrorMessage(error)
    return false
  } finally {
    isSavingPickBan.value = false
  }
}

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
  if (!session.seriesType) {
    void router.replace('/setup')
    return
  }
  if (!session.areTeamsComplete()) {
    void router.replace('/teams')
    return
  }
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

/** 밴 페이즈 시간 초과 — 챔피언 없이 X 표시 */
function applyBanSkip(team: 'RED' | 'BLUE'): boolean {
  if (team === 'RED') {
    if (redBanCount.value >= BAN_COUNT) return false
    const slotIndex = redBanCount.value
    const next = [...redBans.value]
    next[slotIndex] = 'skipped'
    redBans.value = next
    redBanCount.value += 1
    return true
  }

  if (blueBanCount.value >= BAN_COUNT) return false
  const slotIndex = blueBanCount.value
  const next = [...blueBans.value]
  next[slotIndex] = 'skipped'
  blueBans.value = next
  blueBanCount.value += 1
  return true
}

function pickRandomAvailableChampion(): ChampionPortrait | null {
  const available = championPortraits.value.filter(
    (champion) => !unavailableChampionIds.value.has(champion.id),
  )
  if (available.length === 0) return null
  const index = Math.floor(Math.random() * available.length)
  return available[index] ?? null
}

function applyPick(team: 'RED' | 'BLUE', championId: string): boolean {
  const champion = getChampionById(championId)
  if (!champion) return false

  if (team === 'RED') {
    if (redPickCount.value >= TEAM_SIZE) return false
    redPicks.value[redPickCount.value] = champion
    redPickOrder.value = [...redPickOrder.value, champion]
    redPickCount.value += 1
    return true
  }

  if (bluePickCount.value >= TEAM_SIZE) return false
  bluePicks.value[bluePickCount.value] = champion
  bluePickOrder.value = [...bluePickOrder.value, champion]
  bluePickCount.value += 1
  return true
}

function stopCountdown() {
  if (!countdownTimer) return
  clearInterval(countdownTimer)
  countdownTimer = null
}

function markDraftComplete() {
  syncDraftToSession()
}

function stopSwapCountdown() {
  if (!swapCountdownTimer) return
  clearInterval(swapCountdownTimer)
  swapCountdownTimer = null
}

function tickSwapCountdown() {
  if (!isSwapPhaseActive.value) return
  if (swapRemainingSeconds.value <= 0) return

  swapRemainingSeconds.value -= 1
  if (swapRemainingSeconds.value <= 0) {
    onSwapPhaseExpired()
  }
}

function startSwapCountdown() {
  stopSwapCountdown()
  swapCountdownTimer = setInterval(tickSwapCountdown, 1000)
}

async function onSwapPhaseExpired() {
  stopSwapCountdown()
  isSwapPhaseActive.value = false
  isSwapPhaseComplete.value = true
  selectedSwapSlot.value = null
  syncDraftToSession()
  confirmNotice.value = '스왑 시간이 종료되었습니다. 결과 화면으로 이동합니다.'
  await onProceedToResult()
}

async function onProceedToResult() {
  syncDraftToSession()
  const saved = await persistPickBanToServer()
  if (saved) {
    await router.push('/result')
  }
}

function startSwapPhase() {
  isSwapPhaseActive.value = true
  isSwapPhaseComplete.value = false
  swapRemainingSeconds.value = SWAP_TIMER_SECONDS
  selectedSwapSlot.value = null
  confirmNotice.value =
    '같은 팀 슬롯 두 개를 클릭해 챔피언 위치를 스왑하세요.'
  startSwapCountdown()
}

function finishDraft() {
  markDraftComplete()
  startSwapPhase()
}

function isSwapSlotSelected(team: 'red' | 'blue', index: number) {
  const selected = selectedSwapSlot.value
  return selected?.team === team && selected.index === index
}

function swapPicks(team: 'red' | 'blue', indexA: number, indexB: number) {
  const picks = team === 'red' ? redPicks : bluePicks
  const next = [...picks.value]
  const temp = next[indexA] ?? null
  next[indexA] = next[indexB] ?? null
  next[indexB] = temp
  picks.value = next
}

function onPickSlotClick(team: 'red' | 'blue', index: number) {
  if (!isSwapPhaseActive.value) return

  const pick = getPickedChampion(team, index)
  if (!pick) {
    confirmNotice.value = '픽된 챔피언이 있는 슬롯만 선택할 수 있습니다.'
    return
  }

  const current = selectedSwapSlot.value
  if (!current) {
    selectedSwapSlot.value = { team, index }
    confirmNotice.value = `${POSITION_LABELS[index]} 슬롯 선택 · 스왑할 슬롯을 선택하세요.`
    return
  }

  if (current.team === team && current.index === index) {
    selectedSwapSlot.value = null
    confirmNotice.value = '선택을 취소했습니다.'
    return
  }

  if (current.team !== team) {
    selectedSwapSlot.value = { team, index }
    confirmNotice.value = `${POSITION_LABELS[index]} 슬롯 선택 · 같은 팀 슬롯끼리만 스왑할 수 있습니다.`
    return
  }

  swapPicks(team, current.index, index)
  selectedSwapSlot.value = null
  syncDraftToSession()
  const labelA = POSITION_LABELS[current.index]
  const labelB = POSITION_LABELS[index]
  confirmNotice.value = `${labelA} ↔ ${labelB} 챔피언 위치를 스왑했습니다.`
}

function moveToNextPhase(): boolean {
  const isLastPhase = currentPhaseIndex.value >= pickBanOrder.length - 1
  if (isLastPhase) {
    remainingSeconds.value = 0
    stopCountdown()
    finishDraft()
    return false
  }

  currentPhaseIndex.value += 1
  remainingSeconds.value = DRAFT_TIMER_SECONDS
  selectedChampionId.value = null
  return true
}

function tickCountdown() {
  if (!isDraftStarted.value || !currentOrder.value) return
  if (remainingSeconds.value <= 0) return

  remainingSeconds.value -= 1
  if (remainingSeconds.value <= 0) {
    onPhaseTimerExpired()
  }
}

function startCountdown() {
  stopCountdown()
  countdownTimer = setInterval(tickCountdown, 1000)
}

function onPhaseTimerExpired() {
  const order = currentOrder.value
  if (!order || !isDraftStarted.value) return

  stopCountdown()
  remainingSeconds.value = 0
  const phaseLabel = currentPhaseLabel.value

  if (order.type === 'BAN') {
    const skipped = applyBanSkip(order.team)
    confirmNotice.value = skipped
      ? `${phaseLabel} 시간 초과 · 밴 스킵 (×)`
      : `${phaseLabel} 시간 초과 · 밴 슬롯 저장 실패`
  } else {
    const random = pickRandomAvailableChampion()
    if (random) {
      applyPick(order.team, random.id)
      confirmNotice.value = `${phaseLabel} 시간 초과 · ${random.name} 자동 픽`
    } else {
      confirmNotice.value = `${phaseLabel} 시간 초과 · 픽 가능한 챔피언 없음`
    }
  }

  if (moveToNextPhase()) {
    startCountdown()
  } else {
    confirmNotice.value += ' · 밴픽 종료'
  }
}

function startDraft() {
  if (isDraftStarted.value) return

  isDraftStarted.value = true
  session.draft = null
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
  redPickOrder.value = []
  bluePickOrder.value = []
  pickBanSavedMatchNo.value = null
  isSwapPhaseActive.value = false
  isSwapPhaseComplete.value = false
  swapRemainingSeconds.value = 0
  selectedSwapSlot.value = null
  stopSwapCountdown()

  startCountdown()
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
  stopSwapCountdown()
  if (isDraftComplete.value) {
    syncDraftToSession()
  }
})
</script>

<template>
  <section class="draft">
    <header class="draft-header">
      <p class="draft-match text-label">내전 · 밴픽</p>
      <p v-if="session.peerless && peerlessBlockedCount > 0" class="peerless-notice">
        피어리스 · 이전 경기 픽 챔피언 {{ peerlessBlockedCount }}명 선택 불가
      </p>
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
        {{
          isSwapPhaseActive
            ? '스왑 진행 중'
            : isDraftComplete
              ? '밴픽 완료'
              : isDraftStarted
                ? '밴픽 진행 중'
                : '밴픽 시작'
        }}
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
              'has-champion': !!getPickedChampion('red', i - 1),
              'swap-selectable':
                isSwapPhaseActive && !!getPickedChampion('red', i - 1),
              'swap-selected': isSwapSlotSelected('red', i - 1),
            }"
            :role="isSwapPhaseActive ? 'button' : undefined"
            :tabindex="isSwapPhaseActive && getPickedChampion('red', i - 1) ? 0 : undefined"
            @click="onPickSlotClick('red', i - 1)"
            @keydown.enter.prevent="onPickSlotClick('red', i - 1)"
            @keydown.space.prevent="onPickSlotClick('red', i - 1)"
          >
            <span class="pick-position">{{ POSITION_LABELS[i - 1] }}</span>
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
            <span class="pick-name">
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
          <div v-if="!isSwapPhaseActive" class="champion-search-wrap">
            <input
              v-model.trim="championSearchQuery"
              type="text"
              class="champion-search input-field"
              placeholder="챔피언 검색 (예: 트페, 트티드, ahri)"
              aria-label="챔피언 이름 검색"
            />
          </div>
          <!-- <p v-if="confirmNotice" class="confirm-notice" role="status">
            {{ confirmNotice }}
          </p> -->
          <div class="champion-grid-shell">
            <div
              v-if="isSwapPhaseActive"
              class="swap-panel"
              role="status"
            >
              <p class="swap-panel-title">챔피언 위치 스왑</p>
              <p class="swap-panel-timer">
                남은 시간 {{ swapRemainingSeconds }}초
              </p>
              <!-- <p v-if="confirmNotice" class="swap-panel-notice">
                {{ confirmNotice }}
              </p>
              <p v-else class="swap-panel-hint">
                같은 팀 슬롯 두 개를 클릭해 챔피언을 맞바꿀 수 있습니다.
              </p> -->
            </div>
            <p v-else-if="isLoadingChampions" class="portrait-state text-label">
              챔피언 정보를 불러오는 중...
            </p>
            <p v-else-if="championLoadError" class="portrait-state portrait-error">
              {{ championLoadError }}
            </p>
            <p
              v-else-if="filteredChampionPortraits.length === 0"
              class="portrait-state text-label"
            >
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
                    selectable: isDraftStarted && !isDraftComplete,
                    selected: selectedChampionId === champion.id,
                    unavailable: unavailableChampionIds.has(champion.id),
                    'peerless-blocked': isPeerlessBlockedChampion(champion.id),
                  }"
                  :title="
                    isPeerlessBlockedChampion(champion.id)
                      ? `${champion.name} · 피어리스 (이전 경기 픽)`
                      : unavailableChampionIds.has(champion.id)
                        ? `${champion.name} · 선택 불가`
                        : champion.name
                  "
                  loading="lazy"
                  @click="onChampionClick(champion.id)"
                />
              </li>
            </ul>
          </div>
        </div>
        <div v-if="!isSwapPhaseActive" class="confirm-pick-wrap">
          <button
            type="button"
            class="btn-confirm-pick"
            :disabled="!isDraftStarted || isDraftComplete"
            @click="confirmBanPick"
          >
            확인
          </button>
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
              'has-champion': !!getPickedChampion('blue', i - 1),
              'swap-selectable':
                isSwapPhaseActive && !!getPickedChampion('blue', i - 1),
              'swap-selected': isSwapSlotSelected('blue', i - 1),
            }"
            :role="isSwapPhaseActive ? 'button' : undefined"
            :tabindex="isSwapPhaseActive && getPickedChampion('blue', i - 1) ? 0 : undefined"
            @click="onPickSlotClick('blue', i - 1)"
            @keydown.enter.prevent="onPickSlotClick('blue', i - 1)"
            @keydown.space.prevent="onPickSlotClick('blue', i - 1)"
          >
            <span class="pick-position">{{ POSITION_LABELS[i - 1] }}</span>
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
            <span class="pick-name">
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
          :class="{
            'ban-slot--skipped': ban === 'skipped',
            'ban-slot--filled': isBanChampion(ban),
          }"
          :title="
            ban === 'skipped'
              ? '밴 없음 (시간 초과)'
              : isBanChampion(ban)
                ? `${ban.name} 밴`
                : '빈 밴 칸'
          "
        >
          <span
            v-if="ban === 'skipped'"
            class="ban-skip-mark"
            aria-label="밴 없음 (시간 초과)"
          />
          <img
            v-else-if="isBanChampion(ban)"
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
          :class="{
            'ban-slot--skipped': ban === 'skipped',
            'ban-slot--filled': isBanChampion(ban),
          }"
          :title="
            ban === 'skipped'
              ? '밴 없음 (시간 초과)'
              : isBanChampion(ban)
                ? `${ban.name} 밴`
                : '빈 밴 칸'
          "
        >
          <span
            v-if="ban === 'skipped'"
            class="ban-skip-mark"
            aria-label="밴 없음 (시간 초과)"
          />
          <img
            v-else-if="isBanChampion(ban)"
            :src="ban.imageUrl"
            :alt="`${ban.name} 밴 초상화`"
            class="ban-portrait"
          />
        </span>
      </div>
    </footer>

    <nav class="draft-nav">
      <RouterLink to="/teams" class="draft-back">← 팀 배치</RouterLink>
      <button
        v-if="canProceedToResult"
        type="button"
        class="btn-next"
        :disabled="isSavingPickBan"
        @click="onProceedToResult"
      >
        {{ isSavingPickBan ? '저장 중…' : '다음 →' }}
      </button>
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
  --draft-board-height: min(36rem, calc(100vh - 11rem));

  width: 100%;
  max-width: 72rem;
  margin: 0 auto;
  text-align: left;
  box-sizing: border-box;
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

.peerless-notice {
  margin: 0;
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-input);
  border: 1px solid rgba(255, 196, 77, 0.45);
  background: rgba(255, 196, 77, 0.12);
  font-size: 0.78rem;
  color: #ffd98a;
  text-align: center;
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
  grid-template-columns: minmax(7rem, 9rem) minmax(0, 1fr) minmax(7rem, 9rem);
  gap: 0.5rem;
  width: 100%;
  height: var(--draft-board-height);
  min-height: var(--draft-board-height);
  box-sizing: border-box;
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
  flex-direction: column;
  justify-content: flex-end;
  align-items: stretch;
  padding: 0;
  overflow: hidden;
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
  position: relative;
  z-index: 2;
  flex-shrink: 0;
  width: 100%;
  padding: 0.28rem 0.45rem;
  font-size: 0.72rem;
  line-height: 1.2;
  word-break: break-all;
  color: rgba(255, 255, 255, 0.95);
  background: rgba(0, 0, 0, 0.82);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.side-blue .pick-name {
  text-align: right;
}

.pick-slot:not(.has-champion) .pick-name {
  margin-top: auto;
  background: transparent;
  border-top: none;
  padding: 0.35rem 0.45rem;
}

.pick-position {
  position: absolute;
  top: 0.2rem;
  left: 0.35rem;
  z-index: 3;
  font-size: 0.58rem;
  letter-spacing: 0.04em;
  color: rgba(255, 255, 255, 0.72);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.85);
  pointer-events: none;
}

.side-blue .pick-position {
  left: auto;
  right: 0.35rem;
}

.pick-slot.swap-selectable {
  cursor: pointer;
}

.pick-slot.swap-selectable:hover {
  border-color: rgba(255, 255, 255, 0.45);
}

.pick-slot.swap-selected {
  box-shadow: inset 0 0 0 2px #d9f9ff;
  border-color: #d9f9ff;
}

.swap-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
  padding: 1.25rem;
  text-align: center;
}

.swap-panel-title {
  margin: 0;
  font-size: 1rem;
  letter-spacing: 0.08em;
  color: #d9f9ff;
}

.swap-panel-timer {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  color: #fff;
}

.swap-panel-notice,
.swap-panel-hint {
  margin: 0;
  max-width: 22rem;
  font-size: 0.88rem;
  line-height: 1.45;
  color: rgba(255, 255, 255, 0.82);
}

.swap-panel-notice {
  color: #b8f0ff;
}

.pick-champion-art-wrap {
  position: absolute;
  inset: 0 0 auto 0;
  height: calc(100% - 1.65rem);
  overflow: hidden;
  z-index: 0;
}

.pick-champion-art {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  object-position: center top;
  transform: scale(1.12);
  transform-origin: center top;
  filter: saturate(1.06) contrast(1.04);
}

.draft-center {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
  height: 100%;
  min-height: 0;
  padding: 0.35rem;
  box-sizing: border-box;
  background: #0d0d0d;
}

.champion-portrait {
  flex: 1;
  min-width: 0;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.35rem;
  box-sizing: border-box;
  background: #000;
  border: 1px solid rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.champion-search-wrap {
  flex-shrink: 0;
  width: 100%;
  position: sticky;
  top: 0;
  z-index: 1;
  padding-bottom: 0.1rem;
  background: #000;
}

.champion-grid-shell {
  flex: 1;
  min-width: 0;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.portrait-state {
  margin: 0;
  flex: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  box-sizing: border-box;
}

.portrait-error {
  color: #ff8f98;
}

.champion-grid {
  --champion-columns: 10;
  list-style: none;
  margin: 0;
  padding: 0;
  width: 100%;
  display: grid;
  grid-template-columns: repeat(var(--champion-columns), minmax(0, 1fr));
  gap: 0.2rem;
  align-content: start;
  box-sizing: border-box;
}

.champion-search {
  width: 100%;
  box-sizing: border-box;
}

.confirm-pick-wrap {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  padding: 0.45rem 0 0.15rem;
}

.btn-confirm-pick {
  min-width: 8.5rem;
  padding: 0.55rem 1.75rem 0.5rem;
  border: none;
  clip-path: polygon(0 0, 100% 0, 92% 100%, 8% 100%);
  background: var(--color-accent);
  color: #fff;
  font: inherit;
  font-size: 0.95rem;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: filter 0.15s;
}

.btn-confirm-pick:hover:not(:disabled) {
  filter: brightness(1.08);
}

.btn-confirm-pick:disabled {
  opacity: 0.55;
  cursor: default;
}

.confirm-notice {
  margin: 0.25rem 0 0;
  color: var(--color-accent);
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

.champion-image.peerless-blocked {
  box-shadow: inset 0 0 0 1px rgba(255, 196, 77, 0.55);
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

.ban-slot--filled {
  background: rgba(0, 0, 0, 0.72);
}

.ban-slot--skipped {
  background: rgba(232, 64, 87, 0.45);
  border-color: #ff8a98 !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.25);
}


.ban-skip-mark {
  position: relative;
  width: 62%;
  height: 62%;
  flex-shrink: 0;
  transform: skewX(8deg);
}

.ban-skip-mark::before,
.ban-skip-mark::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 100%;
  height: 3px;
  background: #fff;
  border-radius: 1px;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.65);
}

.ban-skip-mark::before {
  transform: translate(-50%, -50%) rotate(45deg);
}

.ban-skip-mark::after {
  transform: translate(-50%, -50%) rotate(-45deg);
}

.ban-red .ban-slot {
  border-color: rgba(232, 64, 87, 0.35);
}

.ban-blue .ban-slot {
  border-color: rgba(0, 168, 204, 0.35);
}

.ban-blue .ban-slot.ban-slot--skipped {
  background: rgba(0, 168, 204, 0.45);
  border-color: #7ee8ff !important;
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
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1rem;
  padding-top: 0.5rem;
}

.draft-back {
  color: var(--color-accent);
  text-decoration: none;
  font-size: 0.95rem;
}

.draft-back:hover {
  text-decoration: underline;
}

.btn-next {
  display: inline-flex;
  align-items: center;
  padding: 0.55rem 1.35rem;
  border-radius: var(--radius-input);
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #fff;
  font: inherit;
  font-size: 0.95rem;
  text-decoration: none;
  cursor: pointer;
  transition: filter 0.2s;
}

.btn-next:hover {
  filter: brightness(1.08);
}

@media (max-width: 640px) {
  .draft {
    --draft-board-height: min(30rem, calc(100vh - 10rem));
  }

  .draft-board {
    grid-template-columns: 5.5rem 1fr 5.5rem;
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
