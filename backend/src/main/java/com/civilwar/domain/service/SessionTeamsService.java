package com.civilwar.domain.service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.civilwar.api.ApiException;
import com.civilwar.api.dto.request.SaveSessionTeamsRequest;
import com.civilwar.api.dto.request.TeamAssignmentInputDto;
import com.civilwar.api.dto.response.SaveSessionTeamsResponse;
import com.civilwar.domain.entity.CustomGameEntity;
import com.civilwar.domain.entity.CustomGamePlayerEntity;
import com.civilwar.domain.enums.CustomGameStatus;
import com.civilwar.domain.enums.PositionName;
import com.civilwar.domain.enums.TeamColor;
import com.civilwar.domain.repository.CustomGamePlayerRepository;
import com.civilwar.domain.repository.CustomGameRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("oracle")
@Transactional
public class SessionTeamsService {

	private static final int TEAM_SIZE = 5;
	private static final int ROSTER_SIZE = 10;
	private static final EnumSet<PositionName> REQUIRED_POSITIONS = EnumSet.allOf(PositionName.class);

	private final CustomGameRepository customGameRepository;
	private final CustomGamePlayerRepository customGamePlayerRepository;

	public SessionTeamsService(
			CustomGameRepository customGameRepository,
			CustomGamePlayerRepository customGamePlayerRepository) {
		this.customGameRepository = customGameRepository;
		this.customGamePlayerRepository = customGamePlayerRepository;
	}

	public SaveSessionTeamsResponse saveTeams(String sessionCode, SaveSessionTeamsRequest request) {
		String code = sessionCode.trim();
		if (code.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "세션 코드가 비어 있습니다.");
		}

		CustomGameEntity game = customGameRepository.findBySessionCode(code)
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"세션을 찾을 수 없습니다. 소환사 입력에서 먼저 저장해 주세요."));

		CustomGameStatus status = game.getStatus();
		CustomGameStatusRules.requireNotTerminal(game, "팀을 저장");

		List<CustomGamePlayerEntity> roster =
				customGamePlayerRepository.findByGame_GameIdOrderBySortOrderAscGamePlayerIdAsc(
						game.getGameId());
		if (roster.size() != ROSTER_SIZE) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"로스터가 10명이 아닙니다. 소환사 입력에서 다시 저장해 주세요.");
		}

		List<TeamAssignmentInputDto> assignments = request.players();
		validateAssignments(assignments, roster);

		Map<Long, CustomGamePlayerEntity> bySummonerId = new HashMap<>();
		for (CustomGamePlayerEntity row : roster) {
			bySummonerId.put(row.getSummoner().getSummonerId(), row);
		}

		for (TeamAssignmentInputDto input : assignments) {
			TeamColor teamColor = parseTeamColor(input.teamColor());
			PositionName positionName = parsePositionName(input.positionName());
			CustomGamePlayerEntity row = bySummonerId.get(input.summonerId());
			row.setTeamColor(teamColor);
			row.setPositionName(positionName);
			row.setSortOrder(sortOrderFor(teamColor, positionName));
			customGamePlayerRepository.save(row);
		}

		if (status == CustomGameStatus.CREATED
				|| status == CustomGameStatus.PLAYERS
				|| status == CustomGameStatus.TEAM_SETUP) {
			game.setStatus(CustomGameStatus.TEAM_SETUP);
		}

		return new SaveSessionTeamsResponse(
				code,
				game.getGameId(),
				assignments.size(),
				game.getStatus().name());
	}

	private static void validateAssignments(
			List<TeamAssignmentInputDto> assignments,
			List<CustomGamePlayerEntity> roster) {
		if (assignments.size() != ROSTER_SIZE) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "팀 배치는 정확히 10명이어야 합니다.");
		}

		Set<Long> rosterIds = new HashSet<>();
		for (CustomGamePlayerEntity row : roster) {
			rosterIds.add(row.getSummoner().getSummonerId());
		}

		Set<Long> seenSummonerIds = new HashSet<>();
		Map<TeamColor, EnumSet<PositionName>> positionsByTeam = Map.of(
				TeamColor.RED, EnumSet.noneOf(PositionName.class),
				TeamColor.BLUE, EnumSet.noneOf(PositionName.class));
		int redCount = 0;
		int blueCount = 0;

		for (TeamAssignmentInputDto input : assignments) {
			Long summonerId = input.summonerId();
			if (summonerId == null) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "summonerId가 필요합니다.");
			}
			if (!rosterIds.contains(summonerId)) {
				throw new ApiException(
						HttpStatus.BAD_REQUEST,
						"로스터에 없는 소환사입니다: " + summonerId);
			}
			if (!seenSummonerIds.add(summonerId)) {
				throw new ApiException(
						HttpStatus.BAD_REQUEST,
						"중복된 소환사가 있습니다: " + summonerId);
			}

			TeamColor teamColor = parseTeamColor(input.teamColor());
			PositionName positionName = parsePositionName(input.positionName());
			EnumSet<PositionName> teamPositions = positionsByTeam.get(teamColor);
			if (!teamPositions.add(positionName)) {
				throw new ApiException(
						HttpStatus.BAD_REQUEST,
						teamColor.name() + " 팀에 포지션이 중복됩니다: " + positionName.name());
			}
			if (teamColor == TeamColor.RED) {
				redCount += 1;
			} else {
				blueCount += 1;
			}
		}

		if (seenSummonerIds.size() != ROSTER_SIZE || !seenSummonerIds.equals(rosterIds)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "로스터 전원에게 팀을 배정해야 합니다.");
		}
		if (redCount != TEAM_SIZE || blueCount != TEAM_SIZE) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "레드·블루 각 5명이어야 합니다.");
		}
		for (TeamColor team : List.of(TeamColor.RED, TeamColor.BLUE)) {
			if (!positionsByTeam.get(team).equals(REQUIRED_POSITIONS)) {
				throw new ApiException(
						HttpStatus.BAD_REQUEST,
						team.name() + " 팀은 TOP·JUNGLE·MID·ADC·SUPPORT가 모두 필요합니다.");
			}
		}
	}

	private static int sortOrderFor(TeamColor teamColor, PositionName positionName) {
		int positionOrder = positionName.ordinal() + 1;
		return teamColor == TeamColor.BLUE ? positionOrder : positionOrder + TEAM_SIZE;
	}

	private static TeamColor parseTeamColor(String raw) {
		try {
			return TeamColor.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException | NullPointerException ex) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 TEAM_COLOR 입니다: " + raw);
		}
	}

	private static PositionName parsePositionName(String raw) {
		try {
			return PositionName.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException | NullPointerException ex) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 POSITION_NAME 입니다: " + raw);
		}
	}
}
