package com.civilwar.domain.service;

import java.util.ArrayList;
import java.util.List;

import com.civilwar.api.dto.request.PlayerInputDto;
import com.civilwar.api.dto.response.CheckRiotNeedsResponse;
import com.civilwar.api.dto.response.PlayerRiotNeedDto;
import com.civilwar.api.dto.response.SavedSummonerDto;
import com.civilwar.domain.entity.SummonerCustomStatEntity;
import com.civilwar.domain.entity.SummonerEntity;
import com.civilwar.domain.repository.SummonerCustomStatRepository;
import com.civilwar.domain.repository.SummonerRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class SummonerCatalogService {

	private final SummonerRepository summonerRepository;
	private final SummonerCustomStatRepository customStatRepository;
	private final SummonerDisplayStatsService displayStatsService;

	public SummonerCatalogService(
			SummonerRepository summonerRepository,
			SummonerCustomStatRepository customStatRepository,
			SummonerDisplayStatsService displayStatsService) {
		this.summonerRepository = summonerRepository;
		this.customStatRepository = customStatRepository;
		this.displayStatsService = displayStatsService;
	}

	public List<SavedSummonerDto> listSavedSummoners() {
		return summonerRepository.findTop100ByOrderByUpdatedAtDesc().stream()
				.map(this::toDto)
				.toList();
	}

	public CheckRiotNeedsResponse checkRiotNeeds(List<PlayerInputDto> inputs) {
		List<PlayerRiotNeedDto> slots = new ArrayList<>();
		boolean needsRiotKey = false;

		for (int i = 0; i < inputs.size(); i++) {
			PlayerInputDto input = inputs.get(i);
			String gameName = input.gameName().trim();
			String tagLine = input.tagLine().trim();

			SummonerEntity summoner = summonerRepository
					.findByGameNameAndTagLine(gameName, tagLine)
					.orElse(null);

			boolean registered = summoner != null;
			int totalGames = registered ? resolveTotalGames(summoner) : 0;
			String puuid = registered ? summoner.getPuuid() : null;
			Long summonerId = registered ? summoner.getSummonerId() : null;

			boolean needsAccountLookup = !registered || isBlank(puuid);
			boolean needsRankRefresh = !registered || totalGames <= 0;
			boolean slotNeedsRiotKey = needsAccountLookup || needsRankRefresh;

			if (slotNeedsRiotKey) {
				needsRiotKey = true;
			}

			slots.add(new PlayerRiotNeedDto(
					i,
					gameName,
					tagLine,
					registered,
					totalGames,
					summonerId,
					puuid,
					needsAccountLookup,
					needsRankRefresh,
					slotNeedsRiotKey));
		}

		return new CheckRiotNeedsResponse(needsRiotKey, slots);
	}

	private int resolveTotalGames(SummonerEntity summoner) {
		return customStatRepository
				.findBySummoner_SummonerId(summoner.getSummonerId())
				.map(SummonerCustomStatEntity::getTotalGames)
				.filter(total -> total != null && total > 0)
				.orElse(0);
	}

	private SavedSummonerDto toDto(SummonerEntity summoner) {
		var displayStats = displayStatsService.toDisplayStats(summoner);
		int totalGames = resolveTotalGames(summoner);

		return new SavedSummonerDto(
				summoner.getSummonerId(),
				summoner.getGameName(),
				summoner.getTagLine(),
				summoner.getPuuid(),
				totalGames,
				displayStats.tier(),
				displayStats.rankName(),
				displayStats.leaguePoints(),
				displayStats.queueType(),
				displayStats);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
