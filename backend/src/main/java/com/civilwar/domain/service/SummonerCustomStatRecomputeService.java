package com.civilwar.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.civilwar.domain.entity.ChampionEntity;
import com.civilwar.domain.entity.CustomMatchPlayerResultEntity;
import com.civilwar.domain.entity.SummonerCustomPositionStatEntity;
import com.civilwar.domain.entity.SummonerCustomStatEntity;
import com.civilwar.domain.entity.SummonerEntity;
import com.civilwar.domain.enums.PositionName;
import com.civilwar.domain.enums.YesNo;
import com.civilwar.domain.repository.CustomMatchPlayerResultRepository;
import com.civilwar.domain.repository.SummonerCustomPositionStatRepository;
import com.civilwar.domain.repository.SummonerCustomStatRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("oracle")
@Transactional
public class SummonerCustomStatRecomputeService {

	private final CustomMatchPlayerResultRepository playerResultRepository;
	private final SummonerCustomStatRepository customStatRepository;
	private final SummonerCustomPositionStatRepository positionStatRepository;

	public SummonerCustomStatRecomputeService(
			CustomMatchPlayerResultRepository playerResultRepository,
			SummonerCustomStatRepository customStatRepository,
			SummonerCustomPositionStatRepository positionStatRepository) {
		this.playerResultRepository = playerResultRepository;
		this.customStatRepository = customStatRepository;
		this.positionStatRepository = positionStatRepository;
	}

	public void recomputeForSummoners(Set<Long> summonerIds) {
		for (Long summonerId : summonerIds) {
			if (summonerId == null) {
				continue;
			}
			List<CustomMatchPlayerResultEntity> results =
					playerResultRepository.findBySummoner_SummonerId(summonerId);
			recomputeOverall(summonerId, results);
			recomputePositions(summonerId, results);
		}
	}

	private void recomputeOverall(Long summonerId, List<CustomMatchPlayerResultEntity> results) {
		SummonerCustomStatEntity stat = customStatRepository
				.findBySummoner_SummonerId(summonerId)
				.orElseGet(() -> {
					SummonerCustomStatEntity created = new SummonerCustomStatEntity();
					if (!results.isEmpty()) {
						created.setSummoner(results.get(0).getSummoner());
					}
					return created;
				});

		if (stat.getSummoner() == null && !results.isEmpty()) {
			stat.setSummoner(results.get(0).getSummoner());
		}

		Aggregate agg = aggregate(results);
		applyOverall(stat, agg);
		customStatRepository.save(stat);
	}

	private void recomputePositions(Long summonerId, List<CustomMatchPlayerResultEntity> results) {
		Map<PositionName, List<CustomMatchPlayerResultEntity>> byPosition = new EnumMap<>(PositionName.class);
		for (CustomMatchPlayerResultEntity result : results) {
			PositionName position = result.getPositionName();
			if (position == null) {
				continue;
			}
			byPosition.computeIfAbsent(position, key -> new ArrayList<>()).add(result);
		}

		Set<PositionName> seen = new HashSet<>(byPosition.keySet());
		List<SummonerCustomPositionStatEntity> existing =
				positionStatRepository.findBySummoner_SummonerIdOrderByPositionName(summonerId);

		for (SummonerCustomPositionStatEntity row : existing) {
			if (!seen.contains(row.getPositionName())) {
				positionStatRepository.delete(row);
			}
		}

		SummonerEntity summoner = results.isEmpty() ? null : results.get(0).getSummoner();
		for (Map.Entry<PositionName, List<CustomMatchPlayerResultEntity>> entry : byPosition.entrySet()) {
			PositionName position = entry.getKey();
			Aggregate agg = aggregate(entry.getValue());
			SummonerCustomPositionStatEntity row = positionStatRepository
					.findBySummoner_SummonerIdAndPositionName(summonerId, position)
					.orElseGet(() -> {
						SummonerCustomPositionStatEntity created = new SummonerCustomPositionStatEntity();
						created.setSummoner(summoner);
						created.setPositionName(position);
						return created;
					});
			if (row.getSummoner() == null) {
				row.setSummoner(summoner);
			}
			applyPosition(row, agg);
			positionStatRepository.save(row);
		}
	}

	private static void applyOverall(SummonerCustomStatEntity stat, Aggregate agg) {
		stat.setTotalGames(agg.totalGames);
		stat.setWins(agg.wins);
		stat.setLosses(agg.losses);
		stat.setWinRate(agg.winRate);
		stat.setTotalKills(agg.totalKills);
		stat.setTotalDeaths(agg.totalDeaths);
		stat.setTotalAssists(agg.totalAssists);
		stat.setAvgKills(agg.avgKills);
		stat.setAvgDeaths(agg.avgDeaths);
		stat.setAvgAssists(agg.avgAssists);
		stat.setAvgKda(agg.avgKda);
		stat.setMainPosition(agg.mainPosition);
		stat.setMostChampion1(agg.most1);
		stat.setMostChampion2(agg.most2);
		stat.setMostChampion3(agg.most3);
	}

	private static void applyPosition(SummonerCustomPositionStatEntity row, Aggregate agg) {
		row.setTotalGames(agg.totalGames);
		row.setWins(agg.wins);
		row.setLosses(agg.losses);
		row.setWinRate(agg.winRate);
		row.setTotalKills(agg.totalKills);
		row.setTotalDeaths(agg.totalDeaths);
		row.setTotalAssists(agg.totalAssists);
		row.setAvgKills(agg.avgKills);
		row.setAvgDeaths(agg.avgDeaths);
		row.setAvgAssists(agg.avgAssists);
		row.setAvgKda(agg.avgKda);
		row.setMostChampion1(agg.most1);
		row.setMostChampion2(agg.most2);
		row.setMostChampion3(agg.most3);
	}

	private static Aggregate aggregate(List<CustomMatchPlayerResultEntity> results) {
		Aggregate agg = new Aggregate();
		agg.totalGames = results.size();

		Map<PositionName, Integer> positionGames = new EnumMap<>(PositionName.class);
		Map<Long, ChampionCount> championCounts = new HashMap<>();
		double kdaSum = 0;

		for (CustomMatchPlayerResultEntity result : results) {
			boolean win = result.getWinYn() == YesNo.Y;
			if (win) {
				agg.wins += 1;
			} else {
				agg.losses += 1;
			}

			int kills = nullToZero(result.getKills());
			int deaths = nullToZero(result.getDeaths());
			int assists = nullToZero(result.getAssists());
			agg.totalKills += kills;
			agg.totalDeaths += deaths;
			agg.totalAssists += assists;
			kdaSum += (kills + assists) / (double) Math.max(deaths, 1);

			if (result.getPositionName() != null) {
				positionGames.merge(result.getPositionName(), 1, Integer::sum);
			}

			ChampionEntity champion = result.getChampion();
			if (champion != null && champion.getChampionId() != null) {
				championCounts
						.computeIfAbsent(champion.getChampionId(), id -> new ChampionCount(champion))
						.count += 1;
			}
		}

		if (agg.totalGames > 0) {
			agg.winRate = BigDecimal.valueOf(agg.wins * 100.0 / agg.totalGames)
					.setScale(2, RoundingMode.HALF_UP);
			agg.avgKills = avg(agg.totalKills, agg.totalGames);
			agg.avgDeaths = avg(agg.totalDeaths, agg.totalGames);
			agg.avgAssists = avg(agg.totalAssists, agg.totalGames);
			agg.avgKda = BigDecimal.valueOf(kdaSum / agg.totalGames)
					.setScale(2, RoundingMode.HALF_UP);
		} else {
			agg.winRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		agg.mainPosition = positionGames.entrySet().stream()
				.max(Comparator
						.<Map.Entry<PositionName, Integer>>comparingInt(Map.Entry::getValue)
						.thenComparing(entry -> entry.getKey().name()))
				.map(Map.Entry::getKey)
				.orElse(null);

		List<ChampionEntity> most = championCounts.values().stream()
				.sorted(Comparator
						.comparingInt((ChampionCount c) -> c.count)
						.reversed()
						.thenComparing(c -> c.champion.getChampionId()))
				.limit(3)
				.map(c -> c.champion)
				.toList();
		agg.most1 = most.size() > 0 ? most.get(0) : null;
		agg.most2 = most.size() > 1 ? most.get(1) : null;
		agg.most3 = most.size() > 2 ? most.get(2) : null;

		return agg;
	}

	private static BigDecimal avg(int total, int games) {
		return BigDecimal.valueOf(total / (double) games).setScale(2, RoundingMode.HALF_UP);
	}

	private static int nullToZero(Integer value) {
		return value == null ? 0 : value;
	}

	private static final class Aggregate {
		int totalGames;
		int wins;
		int losses;
		BigDecimal winRate = BigDecimal.ZERO;
		int totalKills;
		int totalDeaths;
		int totalAssists;
		BigDecimal avgKills;
		BigDecimal avgDeaths;
		BigDecimal avgAssists;
		BigDecimal avgKda;
		PositionName mainPosition;
		ChampionEntity most1;
		ChampionEntity most2;
		ChampionEntity most3;
	}

	private static final class ChampionCount {
		final ChampionEntity champion;
		int count;

		ChampionCount(ChampionEntity champion) {
			this.champion = Objects.requireNonNull(champion);
		}
	}
}
