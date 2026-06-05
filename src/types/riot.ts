export interface RiotAccount {
  puuid: string
  gameName: string
  tagLine: string
}

export interface RiotApiErrorBody {
  status?: { message?: string; status_code?: number }
}
