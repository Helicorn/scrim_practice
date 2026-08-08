export function normalizeChampionSearchText(value: string): string {
  return value.trim().toLowerCase().replace(/\s+/g, '')
}

/** 쿼리 글자가 이름에 순서대로만 나타나면 매칭 (연속일 필요 없음) */
export function matchesSubsequence(haystack: string, needle: string): boolean {
  if (!needle) return true
  if (!haystack) return false

  let needleIndex = 0
  for (const ch of haystack) {
    if (ch === needle[needleIndex]) {
      needleIndex += 1
      if (needleIndex === needle.length) return true
    }
  }
  return false
}

export function matchesChampionSearch(
  champion: { id: string; name: string },
  rawQuery: string,
): boolean {
  const compactQuery = normalizeChampionSearchText(rawQuery)
  if (!compactQuery) return true

  const compactName = normalizeChampionSearchText(champion.name)
  const compactId = normalizeChampionSearchText(champion.id)

  return (
    matchesSubsequence(compactName, compactQuery) ||
    matchesSubsequence(compactId, compactQuery)
  )
}
