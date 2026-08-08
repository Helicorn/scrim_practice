package com.civilwar.api.dto.response;

import java.util.List;

public record RegisterSessionPlayersResponse(
		String sessionCode,
		long gameId,
		List<SummonerSavedDto> summoners) {
}
