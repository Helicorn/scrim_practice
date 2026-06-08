package com.civilwar.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.civilwar.api.ApiException;
import com.civilwar.api.dto.RankStatDto;
import com.civilwar.api.dto.RefreshSessionRankStatsResponse;
import com.civilwar.domain.entity.CustomGamePlayerEntity;
import com.civilwar.domain.entity.SummonerEntity;
import com.civilwar.domain.entity.SummonerRankStatEntity;
import com.civilwar.domain.enums.QueueType;
import com.civilwar.domain.repository.CustomGamePlayerRepository;
import com.civilwar.domain.repository.CustomGameRepository;
import com.civilwar.domain.repository.SummonerRankStatRepository;
import com.civilwar.domain.repository.SummonerRepository;
import com.civilwar.riot.RiotKrClient;
import com.civilwar.riot.dto.RiotLeagueEntry;
import com.civilwar.riot.dto.RiotSummonerV4;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Service
@Profile("oracle")
@Transactional
public class SummonerRankStatRefreshService {

	private static final String SOLO_QUEUE = "RANKED_SOLO_5x5";
	private static final String FLEX_QUEUE = "RANKED_FLEX_SR";
	private static final long RIOT_DELAY_MS = 150L;

	private final CustomGameRepository customGameRepository;
	private final CustomGamePlayerRepository customGamePlayerRepository;
	private final SummonerRepository summonerRepository;
	private final SummonerRankStatRepository rankStatRepository;
	private final RiotKrClient riotKrClient;
	private final SummonerDisplayStatsService displayStatsService;

	public SummonerRankStatRefreshService(
			CustomGameRepository customGameRepository,
			CustomGamePlayerRepository customGamePlayerRepository,
			SummonerRepository summonerRepository,
			SummonerRankStatRepository rankStatRepository,
			RiotKrClient riotKrClient,
			SummonerDisplayStatsService displayStatsService) {
		this.customGameRepository = customGameRepository;
		this.customGamePlayerRepository = customGamePlayerRepository;
		this.summonerRepository = summonerRepository;
		this.rankStatRepository = rankStatRepository;
		this.riotKrClient = riotKrClient;
		this.displayStatsService = displayStatsService;
	}

	public RefreshSessionRankStatsResponse refreshSessionRankStats(
			String sessionCode,
			String riotApiKey) {
		String code = sessionCode.trim();
		String apiKey = riotApiKey.trim();
		if (code.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "세션 코드가 비어 있습니다.");
		}
		if (apiKey.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Riot API Key가 필요합니다.");
		}

		Long gameId = customGameRepository.findBySessionCode(code)
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"세션을 찾을 수 없습니다. 소환사를 먼저 저장해 주세요."))
				.getGameId();

		List<CustomGamePlayerEntity> gamePlayers =
				customGamePlayerRepository.findByGame_GameIdOrderBySortOrderAscGamePlayerIdAsc(gameId);

		int refreshed = 0;
		int skipped = 0;
		List<String> skipReasons = new ArrayList<>();
		List<RankStatDto> rankStats = new ArrayList<>();

		for (int i = 0; i < gamePlayers.size(); i++) {
			if (i > 0) {
				sleep(RIOT_DELAY_MS);
			}
			SummonerEntity summoner = gamePlayers.get(i).getSummoner();
			String label = summoner.getGameName() + "#" + summoner.getTagLine();

			if (displayStatsService.resolveSource(summoner) == SummonerStatsSource.CUSTOM) {
				skipped += 1;
				skipReasons.add(label + ": 내전 전적 있음 — 랭크 조회 생략");
				continue;
			}

			try {
				Optional<RankStatDto> saved = refreshOneSummoner(summoner, apiKey);
				if (saved.isPresent()) {
					refreshed += 1;
					rankStats.add(saved.get());
				} else {
					skipped += 1;
					skipReasons.add(label + ": 랭크 전적 없음 (솔랭·자랭 미배치)");
				}
			} catch (RestClientResponseException ex) {
				skipped += 1;
				skipReasons.add(label + ": Riot API 오류 (" + ex.getStatusCode().value() + ")");
			}
		}

		return new RefreshSessionRankStatsResponse(code, refreshed, skipped, skipReasons, rankStats);
	}

	private Optional<RankStatDto> refreshOneSummoner(SummonerEntity summoner, String apiKey) {
		String puuid = summoner.getPuuid();
		if (puuid == null || puuid.isBlank()) {
			return Optional.empty();
		}

		Optional<RiotSummonerV4> riotSummoner = riotKrClient.findSummonerByPuuid(apiKey, puuid);
		if (riotSummoner.isEmpty()) {
			return Optional.empty();
		}

		summoner.setRiotSummonerId(riotSummoner.get().id());
		summonerRepository.save(summoner);

		List<RiotLeagueEntry> entries =
				riotKrClient.findLeagueEntriesBySummonerId(apiKey, riotSummoner.get().id());
		Optional<RiotLeagueEntry> picked = pickRankedEntry(entries);
		if (picked.isEmpty()) {
			return Optional.empty();
		}

		RiotLeagueEntry entry = picked.get();
		QueueType queueType = toQueueType(entry.queueType())
				.orElseThrow(() -> new IllegalStateException("Unexpected queueType: " + entry.queueType()));

		SummonerRankStatEntity stat = rankStatRepository
				.findBySummoner_SummonerIdAndQueueType(summoner.getSummonerId(), queueType)
				.orElseGet(() -> {
					SummonerRankStatEntity created = new SummonerRankStatEntity();
					created.setSummoner(summoner);
					created.setQueueType(queueType);
					return created;
				});

		stat.setTier(blankToNull(entry.tier()));
		stat.setRankName(blankToNull(entry.rankName()));
		stat.setLeaguePoints(entry.leaguePoints());
		stat.setWins(entry.wins());
		stat.setLosses(entry.losses());
		stat.setWinRate(calculateWinRate(entry.wins(), entry.losses()));

		rankStatRepository.save(stat);

		return Optional.of(new RankStatDto(
				summoner.getSummonerId(),
				summoner.getGameName(),
				summoner.getTagLine(),
				queueType.name(),
				stat.getTier(),
				stat.getRankName(),
				stat.getLeaguePoints(),
				stat.getWins(),
				stat.getLosses(),
				stat.getWinRate() != null ? stat.getWinRate().doubleValue() : null));
	}

	static Optional<RiotLeagueEntry> pickRankedEntry(List<RiotLeagueEntry> entries) {
		Optional<RiotLeagueEntry> solo = entries.stream()
				.filter(e -> SOLO_QUEUE.equals(e.queueType()))
				.findFirst();
		if (solo.isPresent()) {
			return solo;
		}
		return entries.stream()
				.filter(e -> FLEX_QUEUE.equals(e.queueType()))
				.findFirst();
	}

	static Optional<QueueType> toQueueType(String raw) {
		if (raw == null) {
			return Optional.empty();
		}
		return switch (raw) {
			case SOLO_QUEUE -> Optional.of(QueueType.RANKED_SOLO_5x5);
			case FLEX_QUEUE -> Optional.of(QueueType.RANKED_FLEX_SR);
			default -> Optional.empty();
		};
	}

	static BigDecimal calculateWinRate(int wins, int losses) {
		int games = wins + losses;
		if (games <= 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(wins * 100.0 / games).setScale(2, RoundingMode.HALF_UP);
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
