package com.civilwar.api.dto.response;

public record SaveSessionTeamsResponse(
		String sessionCode,
		Long gameId,
		int savedPlayers,
		String status) {
}
