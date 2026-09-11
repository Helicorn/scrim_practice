package com.civilwar.domain.service;

import java.time.LocalDateTime;

import com.civilwar.api.ApiException;
import com.civilwar.api.dto.response.CancelSessionResponse;
import com.civilwar.domain.entity.CustomGameEntity;
import com.civilwar.domain.enums.CustomGameStatus;
import com.civilwar.domain.repository.CustomGameRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("oracle")
@Transactional
public class SessionCancelService {

	private final CustomGameRepository customGameRepository;

	public SessionCancelService(CustomGameRepository customGameRepository) {
		this.customGameRepository = customGameRepository;
	}

	public CancelSessionResponse cancelSession(String sessionCode) {
		String code = sessionCode.trim();
		if (code.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "세션 코드가 비어 있습니다.");
		}

		CustomGameEntity game = customGameRepository.findBySessionCode(code)
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"세션을 찾을 수 없습니다."));

		CustomGameStatus status = game.getStatus();
		if (status == CustomGameStatus.FINISHED) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "이미 종료된 세션입니다.");
		}
		if (status == CustomGameStatus.CANCELLED) {
			return new CancelSessionResponse(code, status.name());
		}

		game.setStatus(CustomGameStatus.CANCELLED);
		game.setEndedAt(LocalDateTime.now());

		return new CancelSessionResponse(code, CustomGameStatus.CANCELLED.name());
	}
}
