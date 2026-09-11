package com.civilwar.api.dto.response;

public record SavedSummonerDto(
		long summonerId,
		String gameName,
		String tagLine,
		String puuid,
		int totalGames,
		String tier,
		String rankName,
		Integer leaguePoints,
		String queueType,
		SummonerDisplayStatsDto displayStats) {
}
