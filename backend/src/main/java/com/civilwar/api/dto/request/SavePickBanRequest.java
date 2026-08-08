package com.civilwar.api.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SavePickBanRequest(
		@NotNull @Min(1) Integer matchNo,
		@NotEmpty @Valid List<PickBanTurnInputDto> turns) {
}
