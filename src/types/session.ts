/** 단판 / 3전2선 / 5전3선 */
export type SeriesType = 'single' | 'bo3' | 'bo5'

export const SERIES_CONFIG: Record<
  SeriesType,
  { label: string; winsRequired: number; maxGames: number }
> = {
  single: { label: '단판', winsRequired: 1, maxGames: 1 },
  bo3: { label: '3전 2선', winsRequired: 2, maxGames: 3 },
  bo5: { label: '5전 3선', winsRequired: 3, maxGames: 5 },
}

/** 소환사 1명 (Riot ID + 전적 조회 결과) */
export interface Player {
  gameName: string
  tagLine: string
  puuid?: string
  /** Oracle SUMMONER.SUMMONER_ID (Players 저장 후) */
  summonerId?: number
  /** Match-V5 최근 조회 매치 수 (0~20) */
  recentMatchCount?: number
}

/** 밴픽 결과 (추후 phase·챔피언 ID 등 확장) */
export interface DraftState {
  completed: boolean
}

/** 플레이어 KDA (수동 입력) */
export interface PlayerKda {
  kills: string
  deaths: string
  assists: string
}

/** 경기 결과 */
export interface MatchResult {
  winner: 'red' | 'blue' | null
  redKda: PlayerKda[]
  blueKda: PlayerKda[]
}

/** 저장된 경기 히스토리 1건 (DB 연동 전 localStorage용) */
export interface SavedMatchRecord {
  id: string
  sessionId: string
  savedAt: string
  seriesType: SeriesType
  gameNumber: number
  winner: 'red' | 'blue'
  peerless: boolean
  redTeam: Player[]
  blueTeam: Player[]
  redKda: PlayerKda[]
  blueKda: PlayerKda[]
}

/** 진행 중 내전 메타 (localStorage) */
export interface CivilWarMeta {
  sessionId: string
  seriesType: SeriesType
  peerless: boolean
  currentGame: number
  redSeriesWins: number
  blueSeriesWins: number
}
