package com.civilwar.domain.service;

import java.util.ArrayList;
import java.util.List;

import com.civilwar.api.ApiException;
import com.civilwar.api.dto.request.PlayerInputDto;
import com.civilwar.api.dto.response.RegisterSessionPlayersResponse;
import com.civilwar.api.dto.response.SummonerSavedDto;
import com.civilwar.domain.entity.CustomGameEntity;
import com.civilwar.domain.entity.CustomGamePlayerEntity;
import com.civilwar.domain.entity.SummonerCustomStatEntity;
import com.civilwar.domain.entity.SummonerEntity;
import com.civilwar.domain.enums.CustomGameStatus;
import com.civilwar.domain.enums.SeriesType;
import com.civilwar.domain.enums.YesNo;
import com.civilwar.domain.repository.CustomGamePlayerRepository;
import com.civilwar.domain.repository.CustomGameRepository;
import com.civilwar.domain.repository.SummonerCustomStatRepository;
import com.civilwar.domain.repository.SummonerRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("oracle")
@Transactional
public class SessionPlayersService {

	private final CustomGameRepository customGameRepository;
	private final CustomGamePlayerRepository customGamePlayerRepository;
	private final SummonerRepository summonerRepository;
	private final SummonerCustomStatRepository customStatRepository;

	public SessionPlayersService(
			CustomGameRepository customGameRepository,
			CustomGamePlayerRepository customGamePlayerRepository,
			SummonerRepository summonerRepository,
			SummonerCustomStatRepository customStatRepository) {
		this.customGameRepository = customGameRepository;
		this.customGamePlayerRepository = customGamePlayerRepository;
		this.summonerRepository = summonerRepository;
		this.customStatRepository = customStatRepository;
	}

	public RegisterSessionPlayersResponse registerPlayers(
			String sessionCode,
			String seriesTypeRaw,
			boolean peerless,
			List<PlayerInputDto> players) {
		String code = sessionCode.trim();
		if (code.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "세션 코드가 비어 있습니다.");
		}

		SeriesType seriesType = parseSeriesType(seriesTypeRaw);
		YesNo peerlessYn = peerless ? YesNo.Y : YesNo.N;

		CustomGameEntity game = customGameRepository.findBySessionCode(code)
				.orElseGet(() -> createGame(code, seriesType, peerlessYn));

		game.setSeriesType(seriesType);
		game.setPeerlessYn(peerlessYn);
		game.setStatus(CustomGameStatus.PLAYERS);

		customGamePlayerRepository.deleteByGame_GameId(game.getGameId());

		List<SummonerSavedDto> saved = new ArrayList<>();
		for (int i = 0; i < players.size(); i++) {
			PlayerInputDto input = players.get(i);
			SummonerEntity summoner = upsertSummoner(input);
			ensureCustomStat(summoner);

			CustomGamePlayerEntity gamePlayer = new CustomGamePlayerEntity();
			gamePlayer.setGame(game);
			gamePlayer.setSummoner(summoner);
			gamePlayer.setSortOrder(i + 1);
			customGamePlayerRepository.save(gamePlayer);

			saved.add(new SummonerSavedDto(
					summoner.getSummonerId(),
					summoner.getGameName(),
					summoner.getTagLine(),
					summoner.getPuuid()));
		}

		return new RegisterSessionPlayersResponse(code, game.getGameId(), saved);
	}

	private CustomGameEntity createGame(String sessionCode, SeriesType seriesType, YesNo peerlessYn) {
		CustomGameEntity game = new CustomGameEntity();
		game.setSessionCode(sessionCode);
		game.setSeriesType(seriesType);
		game.setPeerlessYn(peerlessYn);
		game.setStatus(CustomGameStatus.CREATED);
		return customGameRepository.save(game);
	}

	private SummonerEntity upsertSummoner(PlayerInputDto input) {
		String gameName = input.gameName().trim();
		String tagLine = input.tagLine().trim();
		String puuid = blankToNull(input.puuid());

		SummonerEntity summoner = summonerRepository
				.findByGameNameAndTagLine(gameName, tagLine)
				.orElseGet(() -> {
					SummonerEntity created = new SummonerEntity();
					created.setGameName(gameName);
					created.setTagLine(tagLine);
					return created;
				});

		if (puuid != null) {
			summoner.setPuuid(puuid);
		}
		return summonerRepository.save(summoner);
	}

	private void ensureCustomStat(SummonerEntity summoner) {
		if (customStatRepository.findBySummoner_SummonerId(summoner.getSummonerId()).isPresent()) {
			return;
		}
		SummonerCustomStatEntity stat = new SummonerCustomStatEntity();
		stat.setSummoner(summoner);
		customStatRepository.save(stat);
	}

	private static SeriesType parseSeriesType(String raw) {
		if (raw == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "seriesType이 필요합니다.");
		}
		return switch (raw.trim().toLowerCase()) {
			case "single" -> SeriesType.SINGLE;
			case "bo3" -> SeriesType.BO3;
			case "bo5" -> SeriesType.BO5;
			case "unlimited" -> SeriesType.UNLIMITED;
			default -> throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"지원하지 않는 seriesType입니다: " + raw);
		};
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
