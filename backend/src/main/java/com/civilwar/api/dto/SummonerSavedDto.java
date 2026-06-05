package com.civilwar.api.dto;

public record SummonerSavedDto(
		long summonerId,
		String gameName,
		String tagLine,
		String puuid) {
}
