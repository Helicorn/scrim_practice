import { getApiBaseUrl } from '@/config/api'
import { mapSessionApiErrorMessage, SessionApiError } from '@/services/sessionApi'
import type { Player } from '@/types/session'

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

export async function fetchSavedSummoners(): Promise<SavedSummoner[]> {
  const response = await fetch(buildUrl('/api/summoners'), {
    headers: { Accept: 'application/json' },
  })

  let payload: { message?: string } | SavedSummoner[] = []
  try {
    payload = (await response.json()) as typeof payload
  } catch {
    /* non-JSON */
  }

  if (!response.ok) {
    const message =
      typeof payload === 'object' &&
      payload !== null &&
      !Array.isArray(payload) &&
      'message' in payload &&
      typeof payload.message === 'string'
        ? payload.message
        : `저장된 소환사 목록 조회 실패 (${response.status})`
    throw new SessionApiError(message, response.status)
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

  let payload: { message?: string } | CheckRiotNeedsResult = { needsRiotKey: true, players: [] }
  try {
    payload = (await response.json()) as typeof payload
  } catch {
    /* non-JSON */
  }

  if (!response.ok) {
    const message =
      typeof payload === 'object' &&
      payload !== null &&
      !('players' in payload) &&
      'message' in payload &&
      typeof payload.message === 'string'
        ? payload.message
        : `Riot 필요 여부 확인 실패 (${response.status})`
    throw new SessionApiError(message, response.status)
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
  if (!s.tier) return null
  const tier = s.tier.charAt(0) + s.tier.slice(1).toLowerCase()
  if (s.rankName) {
    const lp = s.leaguePoints != null ? ` ${s.leaguePoints}LP` : ''
    return `${tier} ${s.rankName}${lp}`
  }
  return tier
}

export function savedSummonerKey(s: SavedSummoner): string {
  return `${s.gameName.trim().toLowerCase()}#${s.tagLine.trim().toLowerCase()}`
}
