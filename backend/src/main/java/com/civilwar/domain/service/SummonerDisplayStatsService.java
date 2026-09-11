package com.civilwar.domain.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.civilwar.api.dto.response.ChampionSummaryDto;
import com.civilwar.api.dto.response.SummonerDisplayStatsDto;
import com.civilwar.domain.entity.ChampionEntity;
import com.civilwar.domain.entity.SummonerCustomStatEntity;
import com.civilwar.domain.entity.SummonerEntity;
import com.civilwar.domain.entity.SummonerRankStatEntity;
import com.civilwar.domain.enums.QueueType;
import com.civilwar.domain.repository.SummonerCustomStatRepository;
import com.civilwar.domain.repository.SummonerRankStatRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기획 정책: 내전 0판 → 랭크 전적, 1판 이상 → 내전 집계.
 */
@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class SummonerDisplayStatsService {

	private final SummonerCustomStatRepository customStatRepository;
	private final SummonerRankStatRepository rankStatRepository;

	public SummonerDisplayStatsService(
			SummonerCustomStatRepository customStatRepository,
			SummonerRankStatRepository rankStatRepository) {
		this.customStatRepository = customStatRepository;
		this.rankStatRepository = rankStatRepository;
	}

	public SummonerStatsSource resolveSource(SummonerEntity summoner) {
		return customStatRepository
				.findBySummoner_SummonerId(summoner.getSummonerId())
				.filter(stat -> stat.getTotalGames() != null && stat.getTotalGames() > 0)
				.map(stat -> SummonerStatsSource.CUSTOM)
				.orElse(SummonerStatsSource.RANK);
	}

	public Optional<SummonerCustomStatEntity> findCustomStat(SummonerEntity summoner) {
		return customStatRepository.findBySummoner_SummonerId(summoner.getSummonerId());
	}

	public Optional<SummonerRankStatEntity> findRankStat(SummonerEntity summoner) {
		return findDisplayRankStat(summoner);
	}

	/** 솔랭 우선, 없으면 자랭 fallback */
	public Optional<SummonerRankStatEntity> findDisplayRankStat(SummonerEntity summoner) {
		Long summonerId = summoner.getSummonerId();
		return rankStatRepository
				.findBySummoner_SummonerIdAndQueueType(summonerId, QueueType.RANKED_SOLO_5x5)
				.or(() -> rankStatRepository.findBySummoner_SummonerIdAndQueueType(
						summonerId,
						QueueType.RANKED_FLEX_SR));
	}

	public SummonerDisplayStatsDto toDisplayStats(SummonerEntity summoner) {
		SummonerRankStatEntity rankStat = findDisplayRankStat(summoner).orElse(null);
		if (resolveSource(summoner) == SummonerStatsSource.CUSTOM) {
			SummonerCustomStatEntity custom = findCustomStat(summoner).orElse(null);
			if (custom != null) {
				return fromCustom(custom, rankStat);
			}
		}
		return fromRank(rankStat);
	}

	private static SummonerDisplayStatsDto fromCustom(
			SummonerCustomStatEntity custom,
			SummonerRankStatEntity rankStat) {
		int totalGames = nullToZero(custom.getTotalGames());
		int wins = nullToZero(custom.getWins());
		int losses = nullToZero(custom.getLosses());
		return new SummonerDisplayStatsDto(
				SummonerStatsSource.CUSTOM.name(),
				totalGames,
				wins,
				losses,
				custom.getWinRate(),
				custom.getAvgKda(),
				custom.getMainPosition() != null ? custom.getMainPosition().name() : null,
				mostChampions(
						custom.getMostChampion1(),
						custom.getMostChampion2(),
						custom.getMostChampion3()),
				rankStat != null ? rankStat.getTier() : null,
				rankStat != null ? rankStat.getRankName() : null,
				rankStat != null ? rankStat.getLeaguePoints() : null,
				rankStat != null && rankStat.getQueueType() != null
						? rankStat.getQueueType().name()
						: null);
	}

	private static SummonerDisplayStatsDto fromRank(SummonerRankStatEntity rankStat) {
		if (rankStat == null) {
			return emptyRankStats();
		}
		int wins = nullToZero(rankStat.getWins());
		int losses = nullToZero(rankStat.getLosses());
		int totalGames = wins + losses;
		BigDecimal winRate = rankStat.getWinRate();
		if (winRate == null && totalGames > 0) {
			winRate = BigDecimal.valueOf(wins * 100.0 / totalGames);
		}
		return new SummonerDisplayStatsDto(
				SummonerStatsSource.RANK.name(),
				totalGames,
				wins,
				losses,
				winRate,
				null,
				rankStat.getMainPosition() != null ? rankStat.getMainPosition().name() : null,
				mostChampions(
						rankStat.getMostChampion1(),
						rankStat.getMostChampion2(),
						rankStat.getMostChampion3()),
				rankStat.getTier(),
				rankStat.getRankName(),
				rankStat.getLeaguePoints(),
				rankStat.getQueueType() != null ? rankStat.getQueueType().name() : null);
	}

	private static SummonerDisplayStatsDto emptyRankStats() {
		return new SummonerDisplayStatsDto(
				SummonerStatsSource.RANK.name(),
				0,
				0,
				0,
				null,
				null,
				null,
				List.of(),
				null,
				null,
				null,
				null);
	}

	private static List<ChampionSummaryDto> mostChampions(
			ChampionEntity first,
			ChampionEntity second,
			ChampionEntity third) {
		List<ChampionSummaryDto> list = new ArrayList<>(3);
		addChampion(list, first);
		addChampion(list, second);
		addChampion(list, third);
		return list;
	}

	private static void addChampion(List<ChampionSummaryDto> list, ChampionEntity champion) {
		if (champion == null) {
			return;
		}
		list.add(new ChampionSummaryDto(
				champion.getChampionId(),
				champion.getChampionKey(),
				champion.getChampionNameKr(),
				champion.getImageUrl()));
	}

	private static int nullToZero(Integer value) {
		return value == null ? 0 : value;
	}
}
