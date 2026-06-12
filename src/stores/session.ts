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
const PEERLESS_CHAMPIONS_STORAGE = 'civilwar_peerless_champions'

interface PeerlessChampionStore {
  sessionId: string
  championIds: string[]
}

export type SaveMatchResult =
  | { ok: true; shouldGoToDraft?: boolean }
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
  return list.map((p) => ({
    gameName: p.gameName,
    tagLine: p.tagLine,
    puuid: p.puuid,
    summonerId: p.summonerId,
    recentMatchCount: p.recentMatchCount,
  }))
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
    const meta = JSON.parse(raw) as CivilWarMeta
    if (meta.committed !== true) {
      try {
        localStorage.removeItem(SESSION_META_STORAGE)
      } catch {
        /* ignore */
      }
      return null
    }
    return meta
  } catch {
    return null
  }
}

function loadPeerlessChampions(sessionId: string | null): string[] {
  if (!sessionId) return []
  try {
    const raw = localStorage.getItem(PEERLESS_CHAMPIONS_STORAGE)
    if (!raw) return []
    const parsed = JSON.parse(raw) as PeerlessChampionStore
    if (parsed.sessionId !== sessionId) return []
    return Array.isArray(parsed.championIds) ? parsed.championIds : []
  } catch {
    return []
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
  const committed = ref(storedMeta?.committed ?? false)

  const players = ref<Player[]>(
    storedMeta?.players ? clonePlayers(storedMeta.players) : [],
  )
  const redTeam = ref<Player[]>(
    storedMeta?.redTeam ? clonePlayers(storedMeta.redTeam) : [],
  )
  const blueTeam = ref<Player[]>(
    storedMeta?.blueTeam ? clonePlayers(storedMeta.blueTeam) : [],
  )
  const draft = ref<DraftState | null>(null)
  const matchResult = ref<MatchResult | null>(null)
  const riotApiKey = ref(loadStoredRiotApiKey())
  const matchHistory = ref<SavedMatchRecord[]>(loadMatchHistory())
  const peerlessUsedChampionIds = ref<string[]>(
    loadPeerlessChampions(storedMeta?.sessionId ?? null),
  )

  const hasActiveSession = computed(
    () =>
      committed.value &&
      sessionId.value !== null &&
      seriesType.value !== null,
  )

  const hasSetupInProgress = computed(
    () => !committed.value && seriesType.value !== null,
  )

  function persistSessionMeta() {
    if (!sessionId.value || !seriesType.value || !committed.value) {
      return
    }
    ensurePlayers()
    ensureTeams()
    const meta: CivilWarMeta = {
      sessionId: sessionId.value,
      seriesType: seriesType.value,
      peerless: peerless.value,
      currentGame: currentGame.value,
      redSeriesWins: redSeriesWins.value,
      blueSeriesWins: blueSeriesWins.value,
      committed: true,
      players: clonePlayers(players.value),
      redTeam: clonePlayers(redTeam.value),
      blueTeam: clonePlayers(blueTeam.value),
    }
    try {
      localStorage.setItem(SESSION_META_STORAGE, JSON.stringify(meta))
    } catch {
      /* ignore */
    }
  }

  watch(
    [
      sessionId,
      seriesType,
      peerless,
      currentGame,
      redSeriesWins,
      blueSeriesWins,
      committed,
    ],
    () => persistSessionMeta(),
    { deep: true },
  )

  watch(
    [players, redTeam, blueTeam],
    () => {
      if (committed.value) persistSessionMeta()
    },
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
      puuid: player.puuid,
      summonerId: player.summonerId,
      recentMatchCount: player.recentMatchCount,
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

  function persistPeerlessChampions() {
    if (!sessionId.value) return
    try {
      const payload: PeerlessChampionStore = {
        sessionId: sessionId.value,
        championIds: peerlessUsedChampionIds.value,
      }
      localStorage.setItem(PEERLESS_CHAMPIONS_STORAGE, JSON.stringify(payload))
    } catch {
      /* ignore */
    }
  }

  function clearPeerlessChampions() {
    peerlessUsedChampionIds.value = []
    try {
      localStorage.removeItem(PEERLESS_CHAMPIONS_STORAGE)
    } catch {
      /* ignore */
    }
  }

  function recordPeerlessChampions(ids: string[]) {
    if (!peerless.value || ids.length === 0) return
    peerlessUsedChampionIds.value = [
      ...new Set([...peerlessUsedChampionIds.value, ...ids]),
    ]
    persistPeerlessChampions()
  }

  function getPeerlessBlockedChampionIds(): readonly string[] {
    if (!peerless.value) return []
    return peerlessUsedChampionIds.value
  }

  function resetDraftAndResultState() {
    draft.value = null
    matchResult.value = null
  }

  function resetMatchFlowState() {
    players.value = []
    redTeam.value = []
    blueTeam.value = []
    resetDraftAndResultState()
  }

  function areTeamsComplete(): boolean {
    ensurePlayers()
    ensureTeams()
    const unassigned = players.value.filter(
      (player) => isFilledPlayer(player) && !isOnTeam(player),
    )
    if (unassigned.length > 0) return false
    for (let i = 0; i < TEAM_SIZE; i += 1) {
      if (
        !isFilledPlayer(redTeam.value[i]) ||
        !isFilledPlayer(blueTeam.value[i])
      ) {
        return false
      }
    }
    return true
  }

  function beginSetup(type: SeriesType, isPeerless: boolean) {
    committed.value = false
    sessionId.value = crypto.randomUUID()
    seriesType.value = type
    peerless.value = isPeerless
    currentGame.value = 1
    redSeriesWins.value = 0
    blueSeriesWins.value = 0
    clearPeerlessChampions()
    resetMatchFlowState()
  }

  function commitActiveSession(): boolean {
    if (!sessionId.value || !seriesType.value) return false
    if (!areTeamsComplete()) return false
    committed.value = true
    persistSessionMeta()
    return true
  }

  /** 진행 중 내전 세션·입력 중 데이터 삭제 (저장된 경기 히스토리·API Key는 유지) */
  function clearActiveSession() {
    sessionId.value = null
    seriesType.value = null
    peerless.value = false
    currentGame.value = 1
    redSeriesWins.value = 0
    blueSeriesWins.value = 0
    committed.value = false
    clearPeerlessChampions()
    resetMatchFlowState()
    try {
      localStorage.removeItem(SESSION_META_STORAGE)
    } catch {
      /* ignore */
    }
  }

  function formatSessionSummary(): string {
    if (!seriesType.value) return ''
    const cfg = SERIES_CONFIG[seriesType.value]
    const peerlessLabel = peerless.value ? ' · 피어리스' : ''
    if (!committed.value) {
      return `${cfg.label}${peerlessLabel} · 설정 중`
    }
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
    if (!sessionId.value || !seriesType.value || !committed.value) {
      return {
        ok: false,
        message: '밴픽 화면까지 진행한 뒤 경기 결과를 저장할 수 있습니다.',
      }
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

    let shouldGoToDraft = false

    if (!seriesDone && currentGame.value < cfg.maxGames) {
      if (peerless.value && draft.value?.usedChampionIds?.length) {
        recordPeerlessChampions(draft.value.usedChampionIds)
      }

      currentGame.value += 1

      if (peerless.value) {
        resetDraftAndResultState()
        shouldGoToDraft = true
      } else {
        resetMatchFlowState()
      }

      persistSessionMeta()
    }

    return { ok: true, shouldGoToDraft }
  }

  return {
    sessionId,
    seriesType,
    peerless,
    currentGame,
    redSeriesWins,
    blueSeriesWins,
    hasActiveSession,
    hasSetupInProgress,
    committed,
    players,
    redTeam,
    blueTeam,
    draft,
    matchResult,
    riotApiKey,
    matchHistory,
    peerlessUsedChampionIds,
    beginSetup,
    commitActiveSession,
    areTeamsComplete,
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
    getPeerlessBlockedChampionIds,
    saveMatchToHistory,
  }
})
