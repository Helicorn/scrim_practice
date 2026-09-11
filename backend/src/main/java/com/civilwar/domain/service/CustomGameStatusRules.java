package com.civilwar.domain.service;

import com.civilwar.api.ApiException;
import com.civilwar.domain.entity.CustomGameEntity;
import com.civilwar.domain.enums.CustomGameStatus;

import org.springframework.http.HttpStatus;

/**
 * CUSTOM_GAME.STATUS 전이 규칙.
 *
 * <pre>
 * CREATED → PLAYERS (로스터 등록)
 * PLAYERS → TEAM_SETUP (팀 배치)
 * TEAM_SETUP|DRAFT → RESULT_INPUT (밴픽 마감)
 * RESULT_INPUT → DRAFT (결과 저장, 시리즈 계속) | FINISHED (시리즈 종료)
 * * → CANCELLED (사용자 삭제)
 * </pre>
 */
final class CustomGameStatusRules {

	private CustomGameStatusRules() {
	}

	static void requireNotTerminal(CustomGameEntity game, String actionLabel) {
		CustomGameStatus status = game.getStatus();
		if (status == CustomGameStatus.FINISHED) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "이미 종료된 세션에는 " + actionLabel + "할 수 없습니다.");
		}
		if (status == CustomGameStatus.CANCELLED) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "취소된 세션에는 " + actionLabel + "할 수 없습니다.");
		}
	}

	static void requireTeamsReadyForPickBan(CustomGameEntity game) {
		requireNotTerminal(game, "밴픽을 저장");
		CustomGameStatus status = game.getStatus();
		if (status == CustomGameStatus.CREATED || status == CustomGameStatus.PLAYERS) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"팀 배치를 먼저 저장한 뒤 밴픽을 진행해 주세요.");
		}
	}

	static void requireResultReady(CustomGameEntity game) {
		requireNotTerminal(game, "경기 결과를 저장");
		CustomGameStatus status = game.getStatus();
		if (status == CustomGameStatus.CREATED
				|| status == CustomGameStatus.PLAYERS
				|| status == CustomGameStatus.TEAM_SETUP) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"밴픽을 마감한 뒤에 경기 결과를 저장할 수 있습니다.");
		}
	}
}
