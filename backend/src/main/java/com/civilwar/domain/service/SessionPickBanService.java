package com.civilwar.domain.service;

import java.util.Locale;

import com.civilwar.api.ApiException;
import com.civilwar.api.dto.request.PickBanTurnInputDto;
import com.civilwar.api.dto.request.SavePickBanRequest;
import com.civilwar.api.dto.response.SavePickBanResponse;
import com.civilwar.domain.entity.ChampionEntity;
import com.civilwar.domain.entity.CustomGameEntity;
import com.civilwar.domain.entity.CustomMatchEntity;
import com.civilwar.domain.entity.CustomPickBanEntity;
import com.civilwar.domain.entity.SummonerEntity;
import com.civilwar.domain.enums.CustomGameStatus;
import com.civilwar.domain.enums.CustomMatchStatus;
import com.civilwar.domain.enums.PickBanAction;
import com.civilwar.domain.enums.TeamColor;
import com.civilwar.domain.repository.CustomGameRepository;
import com.civilwar.domain.repository.CustomMatchRepository;
import com.civilwar.domain.repository.CustomPickBanRepository;
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
public class SessionPickBanService {

	@PersistenceContext
	private EntityManager entityManager;

	private final CustomGameRepository customGameRepository;
	private final CustomMatchRepository customMatchRepository;
	private final CustomPickBanRepository customPickBanRepository;
	private final SummonerRepository summonerRepository;
	private final ChampionCatalogService championCatalogService;

	public SessionPickBanService(
			CustomGameRepository customGameRepository,
			CustomMatchRepository customMatchRepository,
			CustomPickBanRepository customPickBanRepository,
			SummonerRepository summonerRepository,
			ChampionCatalogService championCatalogService) {
		this.customGameRepository = customGameRepository;
		this.customMatchRepository = customMatchRepository;
		this.customPickBanRepository = customPickBanRepository;
		this.summonerRepository = summonerRepository;
		this.championCatalogService = championCatalogService;
	}

	public SavePickBanResponse savePickBan(String sessionCode, SavePickBanRequest request) {
		String code = sessionCode.trim();
		if (code.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "세션 코드가 비어 있습니다.");
		}

		CustomGameEntity game = customGameRepository.findBySessionCode(code)
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND,
						"세션을 찾을 수 없습니다. 소환사 입력에서 먼저 저장해 주세요."));

		CustomMatchEntity match = customMatchRepository
				.findByGame_GameIdAndMatchNo(game.getGameId(), request.matchNo())
				.orElseGet(() -> createMatch(game, request.matchNo()));

		// 같은 트랜잭션에서 삭제 직후 INSERT 하면 Oracle UK가 아직 안 풀린 상태로 보일 수 있음
		customPickBanRepository.deleteByMatch_MatchId(match.getMatchId());
		entityManager.flush();

		int savedTurns = 0;
		for (PickBanTurnInputDto turn : request.turns()) {
			PickBanAction actionType = parseActionType(turn.actionType());
			TeamColor teamColor = parseTeamColor(turn.teamColor());
			ChampionEntity champion = championCatalogService.upsertByKey(
					turn.championKey(),
					turn.championNameKr(),
					turn.imageUrl());

			CustomPickBanEntity entity = new CustomPickBanEntity();
			entity.setMatch(match);
			entity.setTurnNo(turn.turnNo());
			entity.setTeamColor(teamColor);
			entity.setActionType(actionType);
			entity.setChampion(champion);

			if (actionType == PickBanAction.PICK && turn.summonerId() != null) {
				SummonerEntity summoner = summonerRepository.findById(turn.summonerId())
						.orElseThrow(() -> new ApiException(
								HttpStatus.BAD_REQUEST,
								"픽 소환사를 찾을 수 없습니다. (summonerId=" + turn.summonerId() + ")"));
				entity.setSummoner(summoner);
			}

			customPickBanRepository.save(entity);
			savedTurns += 1;
		}

		match.setStatus(CustomMatchStatus.RESULT_INPUT);
		match.setCurrentTurnNo(request.turns().size() + 1);
		game.setStatus(CustomGameStatus.RESULT_INPUT);
		game.setCurrentMatchNo(request.matchNo());

		return new SavePickBanResponse(code, match.getMatchId(), match.getMatchNo(), savedTurns);
	}

	private CustomMatchEntity createMatch(CustomGameEntity game, int matchNo) {
		CustomMatchEntity match = new CustomMatchEntity();
		match.setGame(game);
		match.setMatchNo(matchNo);
		match.setStatus(CustomMatchStatus.PICK_BAN);
		match.setCurrentTurnNo(1);
		return customMatchRepository.save(match);
	}

	private static PickBanAction parseActionType(String raw) {
		try {
			return PickBanAction.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 ACTION_TYPE 입니다: " + raw);
		}
	}

	private static TeamColor parseTeamColor(String raw) {
		try {
			return TeamColor.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 TEAM_COLOR 입니다: " + raw);
		}
	}
}
