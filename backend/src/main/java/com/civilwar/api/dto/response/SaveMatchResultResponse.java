package com.civilwar.api.dto.response;

public record SaveMatchResultResponse(
		String sessionCode,
		Long matchId,
		Integer matchNo,
		int savedPlayers,
		int redSeriesWins,
		int blueSeriesWins,
		boolean seriesFinished) {
}
