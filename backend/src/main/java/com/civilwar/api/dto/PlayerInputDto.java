package com.civilwar.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlayerInputDto(
		@NotBlank @Size(max = 100) String gameName,
		@NotBlank @Size(max = 20) String tagLine,
		@Size(max = 100) String puuid) {
}
