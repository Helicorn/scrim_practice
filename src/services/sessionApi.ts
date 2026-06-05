import { getApiBaseUrl } from '@/config/api'
import { RIOT_TOKEN_HEADER } from '@/config/riot'
import type { Player, SeriesType } from '@/types/session'

export interface RankStatSummary {
  summonerId: number
  gameName: string
  tagLine: string
  queueType: string
  tier: string | null
  rankName: string | null
  leaguePoints: number | null
  wins: number
  losses: number
  winRate: number | null
}

export interface RefreshSessionRankStatsResult {
  sessionCode: string
  refreshed: number
  skipped: number
  skipReasons: string[]
  rankStats: RankStatSummary[]
}

export interface RegisterSessionPlayersResult {
  sessionCode: string
  gameId: number
  summoners: {
    summonerId: number
    gameName: string
    tagLine: string
    puuid: string | null
  }[]
}

export class SessionApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'SessionApiError'
    this.status = status
  }
}

function buildUrl(path: string): string {
  return `${getApiBaseUrl()}${path}`
}

export async function registerSessionPlayers(
  sessionCode: string,
  seriesType: SeriesType,
  peerless: boolean,
  players: Player[],
): Promise<RegisterSessionPlayersResult> {
  const body = {
    seriesType,
    peerless,
    players: players.map((p) => ({
      gameName: p.gameName.trim(),
      tagLine: p.tagLine.trim(),
      puuid: p.puuid?.trim() || null,
    })),
  }

  const response = await fetch(
    buildUrl(`/api/sessions/${encodeURIComponent(sessionCode)}/players`),
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(body),
    },
  )

  let payload: { message?: string } | RegisterSessionPlayersResult = {}
  try {
    payload = (await response.json()) as typeof payload
  } catch {
    /* non-JSON */
  }

  if (!response.ok) {
    const serverMessage =
      typeof payload === 'object' &&
      payload !== null &&
      'message' in payload &&
      typeof payload.message === 'string'
        ? payload.message
        : undefined
    const message =
      serverMessage ??
      (response.status === 404
        ? '백엔드 API를 찾을 수 없습니다. npm run server 로 서버를 실행했는지 확인해 주세요.'
        : `서버 오류 (${response.status})`)
    throw new SessionApiError(message, response.status)
  }

  return payload as RegisterSessionPlayersResult
}

export async function refreshSessionRankStats(
  sessionCode: string,
  riotApiKey: string,
): Promise<RefreshSessionRankStatsResult> {
  const response = await fetch(
    buildUrl(`/api/sessions/${encodeURIComponent(sessionCode)}/rank-stats`),
    {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        [RIOT_TOKEN_HEADER]: riotApiKey.trim(),
      },
    },
  )

  let payload: { message?: string } | RefreshSessionRankStatsResult = {}
  try {
    payload = (await response.json()) as typeof payload
  } catch {
    /* non-JSON */
  }

  if (!response.ok) {
    const serverMessage =
      typeof payload === 'object' &&
      payload !== null &&
      'message' in payload &&
      typeof payload.message === 'string'
        ? payload.message
        : undefined
    throw new SessionApiError(
      serverMessage ?? `랭크 조회 실패 (${response.status})`,
      response.status,
    )
  }

  return payload as RefreshSessionRankStatsResult
}

export function mapSessionApiErrorMessage(error: unknown): string {
  if (error instanceof SessionApiError) {
    return error.message
  }
  if (error instanceof TypeError) {
    return '백엔드에 연결할 수 없습니다. npm run server 로 서버를 실행해 주세요.'
  }
  return '소환사 정보 저장 중 알 수 없는 오류가 발생했습니다.'
}
