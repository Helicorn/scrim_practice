package com.civilwar.domain.repository;

import java.util.List;

import com.civilwar.domain.entity.CustomGamePlayerEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomGamePlayerRepository extends JpaRepository<CustomGamePlayerEntity, Long> {

	List<CustomGamePlayerEntity> findByGame_GameIdOrderBySortOrderAscGamePlayerIdAsc(Long gameId);

	void deleteByGame_GameId(Long gameId);
}
