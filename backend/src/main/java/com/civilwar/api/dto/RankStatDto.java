package com.civilwar.api.dto;

public record RankStatDto(
		long summonerId,
		String gameName,
		String tagLine,
		String queueType,
		String tier,
		String rankName,
		Integer leaguePoints,
		int wins,
		int losses,
		Double winRate) {
}
