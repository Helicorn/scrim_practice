package com.civilwar.api;

import com.civilwar.api.dto.RefreshSessionRankStatsResponse;
import com.civilwar.domain.service.SummonerRankStatRefreshService;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/sessions")
public class SessionRankStatsController {

	private static final String RIOT_TOKEN_HEADER = "X-Riot-Token";

	private final SummonerRankStatRefreshService rankStatRefreshService;

	public SessionRankStatsController(SummonerRankStatRefreshService rankStatRefreshService) {
		this.rankStatRefreshService = rankStatRefreshService;
	}

	@PostMapping("/{sessionCode}/rank-stats")
	public RefreshSessionRankStatsResponse refreshRankStats(
			@PathVariable String sessionCode,
			@RequestHeader(value = RIOT_TOKEN_HEADER, required = false) String riotApiKey) {
		if (riotApiKey == null || riotApiKey.isBlank()) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"Riot API Key가 필요합니다. 요청 헤더 X-Riot-Token 을 설정해 주세요.");
		}
		return rankStatRefreshService.refreshSessionRankStats(sessionCode, riotApiKey);
	}
}
