package com.civilwar.domain.repository;

import java.util.Optional;

import com.civilwar.domain.entity.SummonerCustomStatEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SummonerCustomStatRepository extends JpaRepository<SummonerCustomStatEntity, Long> {

	Optional<SummonerCustomStatEntity> findBySummoner_SummonerId(Long summonerId);
}
