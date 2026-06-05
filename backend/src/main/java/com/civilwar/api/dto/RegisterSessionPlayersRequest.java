package com.civilwar.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterSessionPlayersRequest(
		@NotBlank String seriesType,
		boolean peerless,
		@NotNull @Size(min = 10, max = 10) List<@Valid PlayerInputDto> players) {
}
