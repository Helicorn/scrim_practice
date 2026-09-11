package com.civilwar.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.civilwar.api.ApiException;
import com.civilwar.api.dto.response.SessionDraftPickSnapshotDto;
import com.civilwar.api.dto.response.SessionPlayerSnapshotDto;
import com.civilwar.api.dto.response.SessionSnapshotResponse;
import com.civilwar.domain.entity.ChampionEntity;
import com.civilwar.domain.entity.CustomGameEntity;
import com.civilwar.domain.entity.CustomGamePlayerEntity;
import com.civilwar.domain.entity.CustomMatchEntity;
import com.civilwar.domain.entity.CustomPickBanEntity;
import com.civilwar.domain.entity.SummonerEntity;
import com.civilwar.domain.enums.CustomGameStatus;
import com.civilwar.domain.enums.CustomMatchStatus;
import com.civilwar.domain.enums.PickBanAction;
import com.civilwar.domain.enums.SeriesType;
import com.civilwar.domain.enums.YesNo;
import com.civilwar.domain.repository.CustomGamePlayerRepository;
import com.civilwar.domain.repository.CustomGameRepository;
import com.civilwar.domain.repository.CustomMatchRepository;
import com.civilwar.domain.repository.CustomPickBanRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class SessionSnapshotService {

	private final CustomGameRepository customGameRepository;
	private final CustomGamePlayerRepository customGamePlayerRepository;
	private final CustomMatchRepository customMatchRepository;
	private final CustomPickBanRepository customPickBanRepository;

	public SessionSnapshotService(
			CustomGameRepository customGameRepository,
			CustomGamePlayerRepository customGamePlayerRepository,
			CustomMatchRepository customMatchRepository,
			CustomPickBanRepository customPickBanRepository) {
		this.customGameRepository = customGameRepository;
		this.customGamePlayerRepository = customGamePlayerRepository;
		this.customMatchRepository = customMatchRepository;
		this.customPickBanRepository = customPickBanRepository;
	}

	public SessionSnapshotResponse getSnapshot(String sessionCode) {
		String code = sessionCode.trim();
		if (code.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "세션 코드가 비어 있습니다.");
		}

		CustomGameEntity game = customGameRepository.findBySessionCode(code)
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"세션을 찾을 수 없습니다."));

		CustomGameStatus status = game.getStatus();
		if (status == CustomGameStatus.FINISHED || status == CustomGameStatus.CANCELLED) {
			throw new ApiException(
					HttpStatus.NOT_FOUND,
					"종료된 세션입니다. 이어서 진행할 수 없습니다.");
		}

		List<CustomGamePlayerEntity> roster =
				customGamePlayerRepository.findByGame_GameIdOrderBySortOrderAscGamePlayerIdAsc(
						game.getGameId());
		List<SessionPlayerSnapshotDto> players = roster.stream()
				.map(this::toPlayerSnapshot)
				.toList();

		List<CustomMatchEntity> matches =
				customMatchRepository.findByGame_GameIdOrderByMatchNoAsc(game.getGameId());

		List<String> peerlessChampionKeys = collectPeerlessChampionKeys(matches);
		List<SessionDraftPickSnapshotDto> currentMatchPicks =
				collectCurrentMatchPicks(game, matches);

		return new SessionSnapshotResponse(
				code,
				game.getGameId(),
				toFrontendSeriesType(game.getSeriesType()),
				game.getPeerlessYn() == YesNo.Y,
				status.name(),
				nullToOne(game.getCurrentMatchNo()),
				nullToZero(game.getRedSeriesWins()),
				nullToZero(game.getBlueSeriesWins()),
				suggestedRoute(status),
				players,
				peerlessChampionKeys,
				currentMatchPicks);
	}

	private SessionPlayerSnapshotDto toPlayerSnapshot(CustomGamePlayerEntity row) {
		SummonerEntity summoner = row.getSummoner();
		return new SessionPlayerSnapshotDto(
				summoner.getSummonerId(),
				summoner.getGameName(),
				summoner.getTagLine(),
				summoner.getPuuid(),
				row.getTeamColor() == null ? null : row.getTeamColor().name(),
				row.getPositionName() == null ? null : row.getPositionName().name(),
				row.getSortOrder());
	}

	private List<String> collectPeerlessChampionKeys(List<CustomMatchEntity> matches) {
		Set<String> keys = new LinkedHashSet<>();
		for (CustomMatchEntity match : matches) {
			if (match.getStatus() != CustomMatchStatus.FINISHED) {
				continue;
			}
			List<CustomPickBanEntity> turns =
					customPickBanRepository.findByMatch_MatchIdOrderByTurnNoAsc(match.getMatchId());
			for (CustomPickBanEntity turn : turns) {
				if (turn.getActionType() != PickBanAction.PICK) {
					continue;
				}
				ChampionEntity champion = turn.getChampion();
				if (champion != null && champion.getChampionKey() != null) {
					keys.add(champion.getChampionKey());
				}
			}
		}
		return List.copyOf(keys);
	}

	private List<SessionDraftPickSnapshotDto> collectCurrentMatchPicks(
			CustomGameEntity game,
			List<CustomMatchEntity> matches) {
		if (game.getStatus() != CustomGameStatus.RESULT_INPUT) {
			return List.of();
		}
		CustomMatchEntity current = matches.stream()
				.filter(m -> m.getMatchNo().equals(game.getCurrentMatchNo()))
				.findFirst()
				.orElse(null);
		if (current == null || current.getStatus() != CustomMatchStatus.RESULT_INPUT) {
			return List.of();
		}

		List<SessionDraftPickSnapshotDto> picks = new ArrayList<>();
		List<CustomPickBanEntity> turns =
				customPickBanRepository.findByMatch_MatchIdOrderByTurnNoAsc(current.getMatchId());
		for (CustomPickBanEntity turn : turns) {
			if (turn.getActionType() != PickBanAction.PICK) {
				continue;
			}
			ChampionEntity champion = turn.getChampion();
			if (champion == null) {
				continue;
			}
			Long summonerId = turn.getSummoner() == null ? null : turn.getSummoner().getSummonerId();
			picks.add(new SessionDraftPickSnapshotDto(
					turn.getTeamColor().name(),
					summonerId,
					champion.getChampionKey(),
					champion.getChampionNameKr(),
					champion.getImageUrl()));
		}
		return List.copyOf(picks);
	}

	private static String suggestedRoute(CustomGameStatus status) {
		// CREATED/PLAYERS → 팀 배치
		// TEAM_SETUP/DRAFT/PLAYING → 밴픽 (진행 중 밴픽은 마감분만 저장)
		// RESULT_INPUT → 결과 입력
		return switch (status) {
			case CREATED, PLAYERS -> "teams";
			case TEAM_SETUP, DRAFT, PLAYING -> "draft";
			case RESULT_INPUT -> "result";
			case FINISHED, CANCELLED -> "teams";
		};
	}

	private static String toFrontendSeriesType(SeriesType seriesType) {
		if (seriesType == null) {
			return "single";
		}
		return switch (seriesType) {
			case SINGLE -> "single";
			case BO3 -> "bo3";
			case BO5 -> "bo5";
			case UNLIMITED -> "unlimited";
		};
	}

	private static int nullToZero(Integer value) {
		return value == null ? 0 : value;
	}

	private static int nullToOne(Integer value) {
		return value == null ? 1 : value;
	}
}
