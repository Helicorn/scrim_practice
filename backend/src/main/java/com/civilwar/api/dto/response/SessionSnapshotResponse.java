package com.civilwar.api.dto.response;

import java.util.List;

public record SessionSnapshotResponse(
		String sessionCode,
		Long gameId,
		String seriesType,
		boolean peerless,
		String status,
		int currentMatchNo,
		int redSeriesWins,
		int blueSeriesWins,
		String suggestedRoute,
		List<SessionPlayerSnapshotDto> players,
		List<String> peerlessChampionKeys,
		List<SessionDraftPickSnapshotDto> currentMatchPicks) {
}
