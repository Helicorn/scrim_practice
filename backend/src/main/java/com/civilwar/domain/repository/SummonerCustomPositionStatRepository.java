package com.civilwar.domain.repository;

import java.util.List;
import java.util.Optional;

import com.civilwar.domain.entity.SummonerCustomPositionStatEntity;
import com.civilwar.domain.enums.PositionName;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SummonerCustomPositionStatRepository
		extends JpaRepository<SummonerCustomPositionStatEntity, Long> {

	List<SummonerCustomPositionStatEntity> findBySummoner_SummonerIdOrderByPositionName(
			Long summonerId);

	Optional<SummonerCustomPositionStatEntity> findBySummoner_SummonerIdAndPositionName(
			Long summonerId,
			PositionName positionName);
}
