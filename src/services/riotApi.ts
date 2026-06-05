import {
  getRiotApiBaseUrl,
  LOL_MATCH_HISTORY_ACCOUNT_V1_URL,
  LOL_MATCH_V5_IDS_BY_PUUID_URL,
  LOL_MATCH_V5_MATCH_BY_ID_URL,
  RIOT_TOKEN_HEADER,
} from '@/config/riot'
import type { RiotAccount, RiotApiErrorBody } from '@/types/riot'

export class RiotApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'RiotApiError'
    this.status = status
  }
}

function buildUrl(path: string): string {
  return `${getRiotApiBaseUrl()}${path}`
}

async function riotFetch<T>(
  apiKey: string,
  path: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(buildUrl(path), {
    ...init,
    headers: {
      [RIOT_TOKEN_HEADER]: apiKey,
      Accept: 'application/json',
      ...init?.headers,
    },
  })

  if (response.status === 404) {
    throw new RiotApiError('Not found', 404)
  }

  if (!response.ok) {
    let message = `Riot API 오류 (${response.status})`
    try {
      const body = (await response.json()) as RiotApiErrorBody
      if (body.status?.message) message = body.status.message
    } catch {
      /* ignore */
    }
    throw new RiotApiError(message, response.status)
  }

  return response.json() as Promise<T>
}

/** Riot ID → 계정(PUUID) 조회 */
export async function getAccountByRiotId(
  apiKey: string,
  gameName: string,
  tagLine: string,
): Promise<RiotAccount | null> {
  const path = `${LOL_MATCH_HISTORY_ACCOUNT_V1_URL}${encodeURIComponent(gameName)}/${encodeURIComponent(tagLine)}`
  try {
    return await riotFetch<RiotAccount>(apiKey, path)
  } catch (error) {
    if (error instanceof RiotApiError && error.status === 404) {
      return null
    }
    throw error
  }
}

/** PUUID → 최근 매치 ID 목록 */
export async function getMatchIdsByPuuid(
  apiKey: string,
  puuid: string,
  start = 0,
  count = 20,
): Promise<string[]> {
  const query = new URLSearchParams({
    start: String(start),
    count: String(count),
  })
  const path = `${LOL_MATCH_V5_IDS_BY_PUUID_URL}${encodeURIComponent(puuid)}/ids?${query}`
  return riotFetch<string[]>(apiKey, path)
}

/** 매치 ID → 상세 전적 */
export async function getMatchById(
  apiKey: string,
  matchId: string,
): Promise<unknown> {
  const path = `${LOL_MATCH_V5_MATCH_BY_ID_URL}${encodeURIComponent(matchId)}`
  return riotFetch<unknown>(apiKey, path)
}

export function mapRiotApiErrorMessage(error: unknown): string {
  if (!(error instanceof RiotApiError)) {
    return '전적 검색 중 알 수 없는 오류가 발생했습니다.'
  }
  switch (error.status) {
    case 401:
      return 'Riot API Key가 유효하지 않습니다. 키를 확인하거나 갱신해 주세요.'
    case 403:
      return 'Riot API 접근이 거부되었습니다. Key 권한을 확인해 주세요.'
    case 429:
      return 'Riot API 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.'
    default:
      return error.message
  }
}
