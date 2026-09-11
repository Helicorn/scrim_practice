package com.civilwar.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TeamAssignmentInputDto(
		@NotNull Long summonerId,
		@NotBlank String teamColor,
		@NotBlank String positionName) {
}
