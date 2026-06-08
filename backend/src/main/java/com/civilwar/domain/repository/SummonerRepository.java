package com.civilwar.domain.repository;

import java.util.List;
import java.util.Optional;

import com.civilwar.domain.entity.SummonerEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SummonerRepository extends JpaRepository<SummonerEntity, Long> {

	Optional<SummonerEntity> findByGameNameAndTagLine(String gameName, String tagLine);

	List<SummonerEntity> findTop100ByOrderByUpdatedAtDesc();
}
