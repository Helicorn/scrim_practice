package com.civilwar.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 소환사 전적 표시용. source=CUSTOM 이면 내전 집계, RANK 이면 랭크 fallback.
 */
public record SummonerDisplayStatsDto(
		String source,
		int totalGames,
		int wins,
		int losses,
		BigDecimal winRate,
		BigDecimal avgKda,
		String mainPosition,
		List<ChampionSummaryDto> mostChampions,
		String tier,
		String rankName,
		Integer leaguePoints,
		String queueType) {
}
