package com.civilwar.domain.repository;

import java.util.Optional;

import com.civilwar.domain.entity.ChampionEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChampionRepository extends JpaRepository<ChampionEntity, Long> {

	Optional<ChampionEntity> findByChampionKey(String championKey);

	@Query("SELECT MAX(c.championId) FROM ChampionEntity c")
	Optional<Long> findMaxChampionId();
}
