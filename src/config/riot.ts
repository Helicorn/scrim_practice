/** Riot API (아시아 라우팅 — KR 등) */
export const LOL_MATCH_HISTORY_BASE_URL = 'https://asia.api.riotgames.com'

export const LOL_MATCH_HISTORY_ACCOUNT_V1_URL =
  '/riot/account/v1/accounts/by-riot-id/'

export const RIOT_TOKEN_HEADER = 'X-Riot-Token'

/** Vite dev proxy 경로 (브라우저 CORS 우회) */
export const RIOT_API_PROXY_PREFIX = '/riot-api'

/** 개발: proxy, 운영: Java 백엔드 프록시 (추후 VITE_RIOT_API_BASE) */
export function getRiotApiBaseUrl(): string {
  if (import.meta.env.VITE_RIOT_API_BASE) {
    return import.meta.env.VITE_RIOT_API_BASE.replace(/\/$/, '')
  }
  return import.meta.env.DEV ? RIOT_API_PROXY_PREFIX : RIOT_API_PROXY_PREFIX
}
