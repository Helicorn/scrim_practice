package com.civilwar.api.dto.response;

public record ChampionSummaryDto(
		Long championId,
		String championKey,
		String nameKr,
		String imageUrl) {
}
