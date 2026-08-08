/** 단판 / 3전2선 / 5전3선 / 제한 없음 */
export type SeriesType = 'single' | 'bo3' | 'bo5' | 'unlimited'

export const SERIES_CONFIG: Record<
  SeriesType,
  {
    label: string
    /** null이면 승수로 자동 종료하지 않음 */
    winsRequired: number | null
    /** null이면 최대 판수 제한 없음 */
    maxGames: number | null
    description: string
  }
> = {
  single: {
    label: '단판',
    winsRequired: 1,
    maxGames: 1,
    description: '1승 · 최대 1판',
  },
  bo3: {
    label: '3전 2선',
    winsRequired: 2,
    maxGames: 3,
    description: '2승 · 최대 3판',
  },
  bo5: {
    label: '5전 3선',
    winsRequired: 3,
    maxGames: 5,
    description: '3승 · 최대 5판',
  },
  unlimited: {
    label: '제한 없음',
    winsRequired: null,
    maxGames: null,
    description: '승수·판수 제한 없음 · 시작 화면에서 종료',
  },
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

/** 밴픽에서 픽된 챔피언 (슬롯용 초상화) */
export interface DraftChampionPick {
  id: string
  name: string
  imageUrl: string
}

/** 밴픽 결과 */
export interface DraftState {
  completed: boolean
  /** 이번 경기 픽 챔피언 ID (피어리스 집계용, 밴 제외) */
  usedChampionIds?: string[]
  /** 레드 팀 슬롯별 픽 (탑→서폿, 스왑 반영) */
  redPicks?: Array<DraftChampionPick | null>
  /** 블루 팀 슬롯별 픽 (탑→서폿, 스왑 반영) */
  bluePicks?: Array<DraftChampionPick | null>
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

/** 진행 중 내전 메타 (localStorage, 소환사 로스터 확정 후 저장) */
export interface CivilWarMeta {
  sessionId: string
  seriesType: SeriesType
  peerless: boolean
  currentGame: number
  redSeriesWins: number
  blueSeriesWins: number
  committed: boolean
  players?: Player[]
  redTeam?: Player[]
  blueTeam?: Player[]
}
