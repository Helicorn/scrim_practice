package com.civilwar.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MatchPlayerResultInputDto(
		@NotNull Long summonerId,
		@NotBlank String teamColor,
		String positionName,
		String championKey,
		String championNameKr,
		String imageUrl,
		Integer kills,
		Integer deaths,
		Integer assists) {
}
