package com.civilwar.domain.repository;

import java.util.Optional;

import com.civilwar.domain.entity.CustomGameEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomGameRepository extends JpaRepository<CustomGameEntity, Long> {

	Optional<CustomGameEntity> findBySessionCode(String sessionCode);
}
