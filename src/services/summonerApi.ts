import { getApiBaseUrl } from '@/config/api'
import { SessionApiError } from '@/services/sessionApi'
import type { Player } from '@/types/session'

export interface ChampionSummary {
  championId: number | null
  championKey: string | null
  nameKr: string | null
  imageUrl: string | null
}

export type SummonerStatsSource = 'CUSTOM' | 'RANK'

export interface SummonerDisplayStats {
  source: SummonerStatsSource
  totalGames: number
  wins: number
  losses: number
  winRate: number | null
  avgKda: number | null
  mainPosition: string | null
  mostChampions: ChampionSummary[]
  tier: string | null
  rankName: string | null
  leaguePoints: number | null
  queueType: string | null
}

export interface SavedSummoner {
  summonerId: number
  gameName: string
  tagLine: string
  puuid: string | null
  totalGames: number
  tier: string | null
  rankName: string | null
  leaguePoints: number | null
  queueType: string | null
  displayStats: SummonerDisplayStats | null
}

export interface PlayerRiotNeed {
  slotIndex: number
  gameName: string
  tagLine: string
  registered: boolean
  totalGames: number
  summonerId: number | null
  puuid: string | null
  needsAccountLookup: boolean
  needsRankRefresh: boolean
  needsRiotKey: boolean
}

export interface CheckRiotNeedsResult {
  needsRiotKey: boolean
  players: PlayerRiotNeed[]
}

function buildUrl(path: string): string {
  return `${getApiBaseUrl()}${path}`
}

function readApiErrorMessage(payload: unknown, fallback: string): string {
  if (
    typeof payload === 'object' &&
    payload !== null &&
    !Array.isArray(payload) &&
    'message' in payload &&
    typeof (payload as { message?: unknown }).message === 'string'
  ) {
    return (payload as { message: string }).message
  }
  return fallback
}

export async function fetchSavedSummoners(): Promise<SavedSummoner[]> {
  const response = await fetch(buildUrl('/api/summoners'), {
    headers: { Accept: 'application/json' },
  })

  let payload: unknown = []
  try {
    payload = await response.json()
  } catch {
    /* non-JSON */
  }

  if (!response.ok) {
    throw new SessionApiError(
      readApiErrorMessage(
        payload,
        `저장된 소환사 목록 조회 실패 (${response.status})`,
      ),
      response.status,
    )
  }

  return payload as SavedSummoner[]
}

export async function checkRiotNeeds(players: Player[]): Promise<CheckRiotNeedsResult> {
  const response = await fetch(buildUrl('/api/summoners/check-riot-needs'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({
      players: players.map((p) => ({
        gameName: p.gameName.trim(),
        tagLine: p.tagLine.trim(),
        puuid: p.puuid?.trim() || null,
      })),
    }),
  })

  let payload: unknown = { needsRiotKey: true, players: [] }
  try {
    payload = await response.json()
  } catch {
    /* non-JSON */
  }

  if (!response.ok) {
    throw new SessionApiError(
      readApiErrorMessage(
        payload,
        `Riot 필요 여부 확인 실패 (${response.status})`,
      ),
      response.status,
    )
  }

  return payload as CheckRiotNeedsResult
}

export function applyDbSummonerHints(
  players: Player[],
  needs: PlayerRiotNeed[],
): void {
  for (const slot of needs) {
    const player = players[slot.slotIndex]
    if (!player) continue
    if (slot.summonerId != null) {
      player.summonerId = slot.summonerId
    }
    if (slot.puuid && !player.puuid) {
      player.puuid = slot.puuid
    }
  }
}

export function formatRiotKeyRequiredMessage(needs: PlayerRiotNeed[]): string {
  const labels = needs
    .filter((s) => s.needsRiotKey)
    .map((s) => `${s.gameName}#${s.tagLine}`)
  if (labels.length === 0) {
    return 'Riot API Key가 필요합니다.'
  }
  return `아래 소환사는 Riot API Key가 필요합니다: ${labels.join(', ')} (신규 등록 또는 내전 0판)`
}

export function formatSavedSummonerLabel(s: SavedSummoner): string {
  return `${s.gameName}#${s.tagLine}`
}

export function formatSavedSummonerRank(s: SavedSummoner): string | null {
  const tier = s.displayStats?.tier ?? s.tier
  const rankName = s.displayStats?.rankName ?? s.rankName
  const leaguePoints = s.displayStats?.leaguePoints ?? s.leaguePoints
  if (!tier) return null
  const formattedTier = tier.charAt(0) + tier.slice(1).toLowerCase()
  if (rankName) {
    const lp = leaguePoints != null ? ` ${leaguePoints}LP` : ''
    return `${formattedTier} ${rankName}${lp}`
  }
  return formattedTier
}

const POSITION_LABELS: Record<string, string> = {
  TOP: '탑',
  JUNGLE: '정글',
  MID: '미드',
  ADC: '원딜',
  SUPPORT: '서폿',
}

export function formatStatsSourceLabel(s: SavedSummoner): string | null {
  const source = s.displayStats?.source
  if (source === 'CUSTOM') return '내전'
  if (source === 'RANK') return '랭크'
  return null
}

export function formatWinRecord(s: SavedSummoner): string | null {
  const stats = s.displayStats
  if (!stats) return null
  if (stats.totalGames <= 0 && stats.wins <= 0 && stats.losses <= 0) {
    return null
  }
  const rate =
    stats.winRate != null
      ? `${Number(stats.winRate).toFixed(1)}%`
      : null
  const record = `${stats.wins}승 ${stats.losses}패`
  return rate ? `${rate} · ${record}` : record
}

export function formatMainPositionLabel(s: SavedSummoner): string | null {
  const position = s.displayStats?.mainPosition
  if (!position) return null
  return POSITION_LABELS[position] ?? position
}

export function formatMostChampionNames(s: SavedSummoner): string | null {
  const names = (s.displayStats?.mostChampions ?? [])
    .map((c) => c.nameKr?.trim() || c.championKey?.trim() || '')
    .filter((name) => name.length > 0)
  if (names.length === 0) return null
  return names.join(' · ')
}

export function savedSummonerKey(s: SavedSummoner): string {
  return `${s.gameName.trim().toLowerCase()}#${s.tagLine.trim().toLowerCase()}`
}
