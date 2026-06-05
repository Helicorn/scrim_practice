package com.civilwar.domain.repository;

import java.util.List;
import java.util.Optional;

import com.civilwar.domain.entity.CustomMatchEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomMatchRepository extends JpaRepository<CustomMatchEntity, Long> {

	List<CustomMatchEntity> findByGame_GameIdOrderByMatchNoAsc(Long gameId);

	Optional<CustomMatchEntity> findByGame_GameIdAndMatchNo(Long gameId, Integer matchNo);
}
