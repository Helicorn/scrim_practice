package com.civilwar.domain.repository;

import java.util.Optional;

import com.civilwar.domain.entity.SummonerRankStatEntity;
import com.civilwar.domain.enums.QueueType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SummonerRankStatRepository extends JpaRepository<SummonerRankStatEntity, Long> {

	Optional<SummonerRankStatEntity> findBySummoner_SummonerIdAndQueueType(
			Long summonerId,
			QueueType queueType);
}
