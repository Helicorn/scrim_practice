package com.civilwar.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PickBanTurnInputDto(
		@NotNull @Min(1) Integer turnNo,
		@NotBlank String teamColor,
		@NotBlank String actionType,
		@NotBlank String championKey,
		String championNameKr,
		String imageUrl,
		Long summonerId) {
}
