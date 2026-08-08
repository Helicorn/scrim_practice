package com.civilwar.api.dto.response;

public record SavePickBanResponse(
		String sessionCode,
		Long matchId,
		Integer matchNo,
		int savedTurns) {
}
