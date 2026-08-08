package com.civilwar.domain.repository;

import java.util.List;

import com.civilwar.domain.entity.CustomPickBanEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomPickBanRepository extends JpaRepository<CustomPickBanEntity, Long> {

	List<CustomPickBanEntity> findByMatch_MatchIdOrderByTurnNoAsc(Long matchId);

	void deleteByMatch_MatchId(Long matchId);
}
