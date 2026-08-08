package com.civilwar.api.dto.response;

public record SummonerSavedDto(
		long summonerId,
		String gameName,
		String tagLine,
		String puuid) {
}
