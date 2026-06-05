package com.civilwar.domain.repository;

import java.util.Optional;

import com.civilwar.domain.entity.ChampionEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChampionRepository extends JpaRepository<ChampionEntity, Long> {

	Optional<ChampionEntity> findByChampionKey(String championKey);
}
