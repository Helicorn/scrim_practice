package com.civilwar.domain.repository;

import java.util.List;

import com.civilwar.domain.entity.SummonerCustomPositionStatEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SummonerCustomPositionStatRepository
		extends JpaRepository<SummonerCustomPositionStatEntity, Long> {

	List<SummonerCustomPositionStatEntity> findBySummoner_SummonerIdOrderByPositionName(
			Long summonerId);
}
