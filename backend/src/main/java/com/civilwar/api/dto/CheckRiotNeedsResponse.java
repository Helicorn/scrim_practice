package com.civilwar.api.dto;

import java.util.List;

public record CheckRiotNeedsResponse(
		boolean needsRiotKey,
		List<PlayerRiotNeedDto> players) {
}
