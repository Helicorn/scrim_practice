package com.civilwar.api.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckRiotNeedsRequest(
		@NotNull @Size(min = 10, max = 10) List<@Valid PlayerInputDto> players) {
}
