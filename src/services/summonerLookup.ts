import {
  getAccountByRiotId,
  mapRiotApiErrorMessage,
  RiotApiError,
} from '@/services/riotApi'
import type { Player } from '@/types/session'

export interface SummonerLookupResult {
  /** Riot에서 찾지 못한 소환사 (gameName#tagLine) */
  notFound: string[]
  /** 입력 검증·API Key 등으로 검색을 진행하지 못한 경우 */
  validationErrors: string[]
}

const LOOKUP_DELAY_MS = 150

function formatRiotId(player: Player): string {
  const name = player.gameName.trim()
  const tag = player.tagLine.trim()
  return tag ? `${name}#${tag}` : name
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/** Riot API Key 없이 검사: 빈칸·중복 등 */
export function validatePlayerInputs(players: Player[]): string[] {
  const errors: string[] = []

  const emptyNames = players
    .map((p, i) => ({ p, i }))
    .filter(({ p }) => !p.gameName.trim())
    .map(({ i }) => `${i + 1}번`)

  if (emptyNames.length > 0) {
    errors.push(
      `소환사명이 비어 있는 칸이 있습니다: ${emptyNames.join(', ')}`,
    )
  }

  const emptyTags = players
    .map((p, i) => ({ p, i }))
    .filter(({ p }) => p.gameName.trim() && !p.tagLine.trim())
    .map(({ i }) => `${i + 1}번`)

  if (emptyTags.length > 0) {
    errors.push(`태그가 비어 있는 칸이 있습니다: ${emptyTags.join(', ')}`)
  }

  const duplicatedSlotsByRiotId = new Map<
    string,
    { displayRiotId: string; slots: number[] }
  >()
  players.forEach((player, index) => {
    const name = player.gameName.trim()
    const tag = player.tagLine.trim()
    if (!name || !tag) return

    const riotIdKey = `${name.toLowerCase()}#${tag.toLowerCase()}`
    const duplicateInfo = duplicatedSlotsByRiotId.get(riotIdKey)
    if (duplicateInfo) {
      duplicateInfo.slots.push(index + 1)
      return
    }
    duplicatedSlotsByRiotId.set(riotIdKey, {
      displayRiotId: `${name}#${tag}`,
      slots: [index + 1],
    })
  })

  const duplicateGroups = Array.from(duplicatedSlotsByRiotId.values())
    .filter(({ slots }) => slots.length > 1)
    .map(({ displayRiotId, slots }) => `${displayRiotId} (${slots.join(', ')}번)`)

  if (duplicateGroups.length > 0) {
    errors.push(`중복된 소환사명/태그가 있습니다: ${duplicateGroups.join(' · ')}`)
  }

  return errors
}

/**
 * 지정 슬롯만 Account-V1으로 PUUID 조회 (신규·puuid 없음).
 */
export async function lookupSummonersAccount(
  players: Player[],
  apiKey: string,
  slotIndices: number[],
): Promise<SummonerLookupResult> {
  const trimmedKey = apiKey.trim()
  if (!trimmedKey) {
    return {
      notFound: [],
      validationErrors: ['Riot API Key를 입력해 주세요.'],
    }
  }

  const notFound: string[] = []
  let delayPending = false

  for (const index of slotIndices) {
    const player = players[index]
    if (!player) continue

    const gameName = player.gameName.trim()
    const tagLine = player.tagLine.trim()
    const label = formatRiotId(player)

    if (delayPending) {
      await delay(LOOKUP_DELAY_MS)
    }
    delayPending = true

    try {
      const account = await getAccountByRiotId(trimmedKey, gameName, tagLine)
      if (!account) {
        notFound.push(label)
        player.puuid = undefined
        continue
      }
      player.puuid = account.puuid
    } catch (error) {
      if (error instanceof RiotApiError && error.status === 404) {
        notFound.push(label)
        continue
      }
      return {
        notFound: [],
        validationErrors: [mapRiotApiErrorMessage(error)],
      }
    }
  }

  return { notFound, validationErrors: [] }
}

export { formatRiotId }
