package com.civilwar.api.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record SaveSessionTeamsRequest(
		@NotEmpty @Size(min = 10, max = 10) @Valid List<TeamAssignmentInputDto> players) {
}
