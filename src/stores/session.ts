import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import type {
  CivilWarMeta,
  DraftState,
  MatchResult,
  Player,
  PlayerKda,
  SavedMatchRecord,
  SeriesType,
} from '@/types/session'
import { SERIES_CONFIG } from '@/types/session'

const PLAYER_COUNT = 10
const TEAM_SIZE = 5
const RIOT_API_KEY_STORAGE = 'civilwar_riot_api_key'
const MATCH_HISTORY_STORAGE = 'civilwar_match_history'
const SESSION_META_STORAGE = 'civilwar_session_meta'

export type SaveMatchResult =
  | { ok: true }
  | { ok: false; message: string }

function loadStoredRiotApiKey(): string {
  try {
    return localStorage.getItem(RIOT_API_KEY_STORAGE) ?? ''
  } catch {
    return ''
  }
}

function emptyPlayer(): Player {
  return { gameName: '', tagLine: '' }
}

function playerKey(player: Player): string {
  return `${player.gameName.trim().toLowerCase()}#${player.tagLine.trim().toLowerCase()}`
}

function isFilledPlayer(player: Player | undefined): boolean {
  if (!player) return false
  return player.gameName.trim().length > 0
}

function clonePlayers(list: Player[]): Player[] {
  return list.map((p) => ({ gameName: p.gameName, tagLine: p.tagLine }))
}

function cloneKdaList(list: PlayerKda[]): PlayerKda[] {
  return list.map((k) => ({ kills: k.kills, deaths: k.deaths, assists: k.assists }))
}

function loadMatchHistory(): SavedMatchRecord[] {
  try {
    const raw = localStorage.getItem(MATCH_HISTORY_STORAGE)
    if (!raw) return []
    const parsed = JSON.parse(raw) as SavedMatchRecord[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function loadSessionMeta(): CivilWarMeta | null {
  try {
    const raw = localStorage.getItem(SESSION_META_STORAGE)
    if (!raw) return null
    return JSON.parse(raw) as CivilWarMeta
  } catch {
    return null
  }
}

function formatPlayerLabel(player: Player | undefined): string {
  if (!player) return ''
  const name = player.gameName.trim()
  const tag = player.tagLine.trim()
  if (!name) return ''
  return tag ? `${name}#${tag}` : name
}

export const useSessionStore = defineStore('session', () => {
  const storedMeta = loadSessionMeta()

  const sessionId = ref<string | null>(storedMeta?.sessionId ?? null)
  const seriesType = ref<SeriesType | null>(storedMeta?.seriesType ?? null)
  const peerless = ref(storedMeta?.peerless ?? false)
  const currentGame = ref(storedMeta?.currentGame ?? 1)
  const redSeriesWins = ref(storedMeta?.redSeriesWins ?? 0)
  const blueSeriesWins = ref(storedMeta?.blueSeriesWins ?? 0)

  const players = ref<Player[]>([])
  const redTeam = ref<Player[]>([])
  const blueTeam = ref<Player[]>([])
  const draft = ref<DraftState | null>(null)
  const matchResult = ref<MatchResult | null>(null)
  const riotApiKey = ref(loadStoredRiotApiKey())
  const matchHistory = ref<SavedMatchRecord[]>(loadMatchHistory())

  const hasActiveSession = computed(
    () => sessionId.value !== null && seriesType.value !== null,
  )

  function persistSessionMeta() {
    if (!sessionId.value || !seriesType.value) {
      try {
        localStorage.removeItem(SESSION_META_STORAGE)
      } catch {
        /* ignore */
      }
      return
    }
    const meta: CivilWarMeta = {
      sessionId: sessionId.value,
      seriesType: seriesType.value,
      peerless: peerless.value,
      currentGame: currentGame.value,
      redSeriesWins: redSeriesWins.value,
      blueSeriesWins: blueSeriesWins.value,
    }
    try {
      localStorage.setItem(SESSION_META_STORAGE, JSON.stringify(meta))
    } catch {
      /* ignore */
    }
  }

  watch(
    [sessionId, seriesType, peerless, currentGame, redSeriesWins, blueSeriesWins],
    () => persistSessionMeta(),
    { deep: true },
  )

  watch(riotApiKey, (value) => {
    try {
      localStorage.setItem(RIOT_API_KEY_STORAGE, value)
    } catch {
      /* private mode 등 */
    }
  })

  function ensurePlayers() {
    if (players.value.length === PLAYER_COUNT) return
    const existing = players.value
    players.value = Array.from({ length: PLAYER_COUNT }, (_, i) => ({
      gameName: existing[i]?.gameName ?? '',
      tagLine: existing[i]?.tagLine ?? '',
    }))
  }

  function ensureTeams() {
    ensurePlayers()
    if (redTeam.value.length !== TEAM_SIZE) {
      const existing = redTeam.value
      redTeam.value = Array.from({ length: TEAM_SIZE }, (_, i) => ({
        gameName: existing[i]?.gameName ?? '',
        tagLine: existing[i]?.tagLine ?? '',
      }))
    }
    if (blueTeam.value.length !== TEAM_SIZE) {
      const existing = blueTeam.value
      blueTeam.value = Array.from({ length: TEAM_SIZE }, (_, i) => ({
        gameName: existing[i]?.gameName ?? '',
        tagLine: existing[i]?.tagLine ?? '',
      }))
    }
  }

  function isOnTeam(player: Player): boolean {
    if (!isFilledPlayer(player)) return false
    const key = playerKey(player)
    return [...redTeam.value, ...blueTeam.value].some(
      (p) => isFilledPlayer(p) && playerKey(p) === key,
    )
  }

  function clearPlayerFromTeams(player: Player) {
    const key = playerKey(player)
    const clearIfMatch = (p: Player) =>
      isFilledPlayer(p) && playerKey(p) === key ? emptyPlayer() : p
    redTeam.value = redTeam.value.map(clearIfMatch)
    blueTeam.value = blueTeam.value.map(clearIfMatch)
  }

  function assignPlayer(team: 'red' | 'blue', slotIndex: number, player: Player) {
    clearPlayerFromTeams(player)
    const target = team === 'red' ? redTeam : blueTeam
    target.value[slotIndex] = {
      gameName: player.gameName,
      tagLine: player.tagLine,
    }
  }

  function clearSlot(team: 'red' | 'blue', slotIndex: number) {
    const target = team === 'red' ? redTeam : blueTeam
    target.value[slotIndex] = emptyPlayer()
  }

  function ensureKdaList(existing: PlayerKda[] | undefined): PlayerKda[] {
    return Array.from({ length: TEAM_SIZE }, (_, i) => ({
      kills: existing?.[i]?.kills ?? '',
      deaths: existing?.[i]?.deaths ?? '',
      assists: existing?.[i]?.assists ?? '',
    }))
  }

  function ensureMatchResult() {
    if (!matchResult.value) {
      matchResult.value = {
        winner: null,
        redKda: ensureKdaList(undefined),
        blueKda: ensureKdaList(undefined),
      }
      return
    }
    if (matchResult.value.redKda.length !== TEAM_SIZE) {
      matchResult.value.redKda = ensureKdaList(matchResult.value.redKda)
    }
    if (matchResult.value.blueKda.length !== TEAM_SIZE) {
      matchResult.value.blueKda = ensureKdaList(matchResult.value.blueKda)
    }
  }

  function getKda(team: 'red' | 'blue', slotIndex: number): PlayerKda {
    ensureMatchResult()
    const list =
      team === 'red' ? matchResult.value!.redKda : matchResult.value!.blueKda
    return list[slotIndex]
  }

  function setWinner(team: 'red' | 'blue' | null) {
    ensureMatchResult()
    matchResult.value!.winner = team
  }

  function resetMatchFlowState() {
    players.value = []
    redTeam.value = []
    blueTeam.value = []
    draft.value = null
    matchResult.value = null
  }

  function startNewSession(type: SeriesType, isPeerless: boolean) {
    sessionId.value = crypto.randomUUID()
    seriesType.value = type
    peerless.value = isPeerless
    currentGame.value = 1
    redSeriesWins.value = 0
    blueSeriesWins.value = 0
    resetMatchFlowState()
    persistSessionMeta()
  }

  /** 진행 중 내전 세션·입력 중 데이터 삭제 (저장된 경기 히스토리·API Key는 유지) */
  function clearActiveSession() {
    sessionId.value = null
    seriesType.value = null
    peerless.value = false
    currentGame.value = 1
    redSeriesWins.value = 0
    blueSeriesWins.value = 0
    resetMatchFlowState()
    persistSessionMeta()
  }

  function formatSessionSummary(): string {
    if (!seriesType.value) return ''
    const cfg = SERIES_CONFIG[seriesType.value]
    const peerlessLabel = peerless.value ? ' · 피어리스' : ''
    const score = `RED ${redSeriesWins.value} - ${blueSeriesWins.value} BLUE`
    return `${cfg.label}${peerlessLabel} · ${currentGame.value}/${cfg.maxGames}판 · ${score}`
  }

  function persistMatchHistory() {
    try {
      localStorage.setItem(
        MATCH_HISTORY_STORAGE,
        JSON.stringify(matchHistory.value),
      )
    } catch {
      /* storage full, private mode 등 */
    }
  }

  function saveMatchToHistory(): SaveMatchResult {
    ensureMatchResult()
    ensureTeams()

    const winner = matchResult.value!.winner
    if (!winner) {
      return { ok: false, message: '승리 팀을 선택해 주세요.' }
    }
    if (!sessionId.value || !seriesType.value) {
      return { ok: false, message: '진입 페이지에서 내전을 먼저 시작해 주세요.' }
    }

    const record: SavedMatchRecord = {
      id: crypto.randomUUID(),
      sessionId: sessionId.value,
      savedAt: new Date().toISOString(),
      seriesType: seriesType.value,
      gameNumber: currentGame.value,
      winner,
      peerless: peerless.value,
      redTeam: clonePlayers(redTeam.value),
      blueTeam: clonePlayers(blueTeam.value),
      redKda: cloneKdaList(matchResult.value!.redKda),
      blueKda: cloneKdaList(matchResult.value!.blueKda),
    }

    if (winner === 'red') redSeriesWins.value += 1
    else blueSeriesWins.value += 1

    matchHistory.value = [...matchHistory.value, record]
    persistMatchHistory()
    persistSessionMeta()

    const cfg = SERIES_CONFIG[seriesType.value]
    const seriesDone =
      redSeriesWins.value >= cfg.winsRequired ||
      blueSeriesWins.value >= cfg.winsRequired

    if (!seriesDone && currentGame.value < cfg.maxGames) {
      currentGame.value += 1
      resetMatchFlowState()
      persistSessionMeta()
    }

    return { ok: true }
  }

  return {
    sessionId,
    seriesType,
    peerless,
    currentGame,
    redSeriesWins,
    blueSeriesWins,
    hasActiveSession,
    players,
    redTeam,
    blueTeam,
    draft,
    matchResult,
    riotApiKey,
    matchHistory,
    startNewSession,
    clearActiveSession,
    formatSessionSummary,
    ensurePlayers,
    ensureTeams,
    isFilledPlayer,
    formatPlayerLabel,
    playerKey,
    isOnTeam,
    assignPlayer,
    clearSlot,
    ensureMatchResult,
    setWinner,
    getKda,
    saveMatchToHistory,
  }
})
