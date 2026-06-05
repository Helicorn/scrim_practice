package com.civilwar.domain.repository;

import java.util.List;

import com.civilwar.domain.entity.CustomMatchPlayerResultEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomMatchPlayerResultRepository
		extends JpaRepository<CustomMatchPlayerResultEntity, Long> {

	List<CustomMatchPlayerResultEntity> findByMatch_MatchIdOrderByTeamColorAscSummoner_SummonerIdAsc(
			Long matchId);
}
