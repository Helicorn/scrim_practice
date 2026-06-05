package com.civilwar.domain.service;

import java.util.Optional;

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
}
