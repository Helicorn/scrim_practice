package com.civilwar.api.dto.response;

import java.util.List;

public record CheckRiotNeedsResponse(
		boolean needsRiotKey,
		List<PlayerRiotNeedDto> players) {
}
