package com.civilwar.domain.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.civilwar.api.ApiException;
import com.civilwar.api.dto.request.MatchPlayerResultInputDto;
import com.civilwar.api.dto.request.SaveMatchResultRequest;
import com.civilwar.api.dto.response.SaveMatchResultResponse;
import com.civilwar.domain.entity.ChampionEntity;
import com.civilwar.domain.entity.CustomGameEntity;
import com.civilwar.domain.entity.CustomMatchEntity;
import com.civilwar.domain.entity.CustomMatchPlayerResultEntity;
import com.civilwar.domain.entity.SummonerEntity;
import com.civilwar.domain.enums.CustomGameStatus;
import com.civilwar.domain.enums.CustomMatchStatus;
import com.civilwar.domain.enums.PositionName;
import com.civilwar.domain.enums.SeriesType;
import com.civilwar.domain.enums.TeamColor;
import com.civilwar.domain.enums.YesNo;
import com.civilwar.domain.repository.CustomGameRepository;
import com.civilwar.domain.repository.CustomMatchPlayerResultRepository;
import com.civilwar.domain.repository.CustomMatchRepository;
import com.civilwar.domain.repository.SummonerRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("oracle")
@Transactional
public class SessionMatchResultService {

	@PersistenceContext
	private EntityManager entityManager;

	private final CustomGameRepository customGameRepository;
	private final CustomMatchRepository customMatchRepository;
	private final CustomMatchPlayerResultRepository playerResultRepository;
	private final SummonerRepository summonerRepository;
	private final ChampionCatalogService championCatalogService;
	private final SummonerCustomStatRecomputeService customStatRecomputeService;

	public SessionMatchResultService(
			CustomGameRepository customGameRepository,
			CustomMatchRepository customMatchRepository,
			CustomMatchPlayerResultRepository playerResultRepository,
			SummonerRepository summonerRepository,
			ChampionCatalogService championCatalogService,
			SummonerCustomStatRecomputeService customStatRecomputeService) {
		this.customGameRepository = customGameRepository;
		this.customMatchRepository = customMatchRepository;
		this.playerResultRepository = playerResultRepository;
		this.summonerRepository = summonerRepository;
		this.championCatalogService = championCatalogService;
		this.customStatRecomputeService = customStatRecomputeService;
	}

	public SaveMatchResultResponse saveMatchResult(String sessionCode, SaveMatchResultRequest request) {
		String code = sessionCode.trim();
		if (code.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "세션 코드가 비어 있습니다.");
		}
		if (request.players().size() != 10) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "경기 결과는 10명의 소환사가 필요합니다.");
		}

		CustomGameEntity game = customGameRepository.findBySessionCode(code)
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"세션을 찾을 수 없습니다. 소환사 입력에서 먼저 저장해 주세요."));

		CustomGameStatusRules.requireResultReady(game);

		TeamColor winTeam = parseTeamColor(request.winTeamColor());

		CustomMatchEntity match = customMatchRepository
				.findByGame_GameIdAndMatchNo(game.getGameId(), request.matchNo())
				.orElseGet(() -> createMatch(game, request.matchNo()));

		boolean wasFinished = match.getStatus() == CustomMatchStatus.FINISHED;
		TeamColor previousWinTeam = match.getWinTeamColor();

		playerResultRepository.deleteByMatch_MatchId(match.getMatchId());
		entityManager.flush();

		Set<Long> summonerIds = new HashSet<>();
		int savedPlayers = 0;
		for (MatchPlayerResultInputDto player : request.players()) {
			TeamColor teamColor = parseTeamColor(player.teamColor());
			PositionName positionName = parsePositionNameOptional(player.positionName());
			SummonerEntity summoner = summonerRepository.findById(player.summonerId())
					.orElseThrow(() -> new ApiException(
							HttpStatus.BAD_REQUEST,
							"소환사를 찾을 수 없습니다. (summonerId=" + player.summonerId() + ")"));

			ChampionEntity champion = null;
			if (player.championKey() != null && !player.championKey().isBlank()) {
				champion = championCatalogService.upsertByKey(
						player.championKey(),
						player.championNameKr(),
						player.imageUrl());
			}

			CustomMatchPlayerResultEntity entity = new CustomMatchPlayerResultEntity();
			entity.setMatch(match);
			entity.setSummoner(summoner);
			entity.setTeamColor(teamColor);
			entity.setPositionName(positionName);
			entity.setChampion(champion);
			entity.setWinYn(teamColor == winTeam ? YesNo.Y : YesNo.N);
			entity.setKills(player.kills());
			entity.setDeaths(player.deaths());
			entity.setAssists(player.assists());
			playerResultRepository.save(entity);

			summonerIds.add(summoner.getSummonerId());
			savedPlayers += 1;
		}

		match.setWinTeamColor(winTeam);
		match.setStatus(CustomMatchStatus.FINISHED);
		match.setEndedAt(LocalDateTime.now());

		updateSeriesWins(game, wasFinished, previousWinTeam, winTeam);

		boolean seriesFinished = isSeriesFinished(game);
		if (seriesFinished) {
			game.setStatus(CustomGameStatus.FINISHED);
			game.setEndedAt(LocalDateTime.now());
			game.setCurrentMatchNo(request.matchNo());
		} else {
			game.setStatus(CustomGameStatus.DRAFT);
			game.setEndedAt(null);
			game.setCurrentMatchNo(request.matchNo() + 1);
		}

		customStatRecomputeService.recomputeForSummoners(summonerIds);

		return new SaveMatchResultResponse(
				code,
				match.getMatchId(),
				match.getMatchNo(),
				savedPlayers,
				nullToZero(game.getRedSeriesWins()),
				nullToZero(game.getBlueSeriesWins()),
				seriesFinished,
				nullToZero(game.getCurrentMatchNo()),
				game.getStatus().name());
	}

	private CustomMatchEntity createMatch(CustomGameEntity game, int matchNo) {
		CustomMatchEntity match = new CustomMatchEntity();
		match.setGame(game);
		match.setMatchNo(matchNo);
		match.setStatus(CustomMatchStatus.RESULT_INPUT);
		match.setCurrentTurnNo(1);
		return customMatchRepository.save(match);
	}

	private static void updateSeriesWins(
			CustomGameEntity game,
			boolean wasFinished,
			TeamColor previousWinTeam,
			TeamColor winTeam) {
		int red = nullToZero(game.getRedSeriesWins());
		int blue = nullToZero(game.getBlueSeriesWins());

		if (wasFinished && previousWinTeam != null) {
			if (previousWinTeam == TeamColor.RED) {
				red = Math.max(0, red - 1);
			} else {
				blue = Math.max(0, blue - 1);
			}
		}

		if (winTeam == TeamColor.RED) {
			red += 1;
		} else {
			blue += 1;
		}

		game.setRedSeriesWins(red);
		game.setBlueSeriesWins(blue);
	}

	private static boolean isSeriesFinished(CustomGameEntity game) {
		SeriesType seriesType = game.getSeriesType();
		if (seriesType == null || seriesType == SeriesType.UNLIMITED) {
			return false;
		}
		int required = winsRequired(seriesType);
		return nullToZero(game.getRedSeriesWins()) >= required
				|| nullToZero(game.getBlueSeriesWins()) >= required;
	}

	private static int winsRequired(SeriesType seriesType) {
		return switch (seriesType) {
			case SINGLE -> 1;
			case BO3 -> 2;
			case BO5 -> 3;
			case UNLIMITED -> Integer.MAX_VALUE;
		};
	}

	private static TeamColor parseTeamColor(String raw) {
		try {
			return TeamColor.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 TEAM_COLOR 입니다: " + raw);
		}
	}

	private static PositionName parsePositionNameOptional(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return PositionName.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 POSITION_NAME 입니다: " + raw);
		}
	}

	private static int nullToZero(Integer value) {
		return value == null ? 0 : value;
	}
}
