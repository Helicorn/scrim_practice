package com.civilwar.api.dto;

public record PlayerRiotNeedDto(
		int slotIndex,
		String gameName,
		String tagLine,
		boolean registered,
		int totalGames,
		Long summonerId,
		String puuid,
		boolean needsAccountLookup,
		boolean needsRankRefresh,
		boolean needsRiotKey) {
}
