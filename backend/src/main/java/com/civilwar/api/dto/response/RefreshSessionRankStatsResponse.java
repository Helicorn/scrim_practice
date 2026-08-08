package com.civilwar.api.dto.response;

import java.util.List;

public record RefreshSessionRankStatsResponse(
		String sessionCode,
		int refreshed,
		int skipped,
		List<String> skipReasons,
		List<RankStatDto> rankStats) {
}
