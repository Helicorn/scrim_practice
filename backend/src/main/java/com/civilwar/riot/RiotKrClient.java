package com.civilwar.riot;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.civilwar.config.CivilwarProperties;
import com.civilwar.riot.dto.RiotLeagueEntry;
import com.civilwar.riot.dto.RiotSummonerV4;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

@Component
public class RiotKrClient {

	private static final String RIOT_TOKEN_HEADER = "X-Riot-Token";

	private final RestClient restClient;
	private final String krBaseUrl;

	public RiotKrClient(CivilwarProperties properties) {
		this.krBaseUrl = properties.riot().krBaseUrl().replaceAll("/$", "");
		this.restClient = RestClient.builder().build();
	}

	public Optional<RiotSummonerV4> findSummonerByPuuid(String apiKey, String puuid) {
		String url = krBaseUrl + "/lol/summoner/v4/summoners/by-puuid/"
				+ encodePath(puuid);
		return getOptional(apiKey, url, RiotSummonerV4.class);
	}

	/** Riot이 summoner id를 제거한 뒤 권장되는 랭크 조회 경로 */
	public List<RiotLeagueEntry> findLeagueEntriesByPuuid(String apiKey, String puuid) {
		String url = krBaseUrl + "/lol/league/v4/entries/by-puuid/"
				+ encodePath(puuid);
		RiotLeagueEntry[] entries = get(apiKey, url, RiotLeagueEntry[].class);
		if (entries == null || entries.length == 0) {
			return List.of();
		}
		return Arrays.asList(entries);
	}

	private <T> Optional<T> getOptional(String apiKey, String url, Class<T> type) {
		try {
			return Optional.ofNullable(get(apiKey, url, type));
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == 404) {
				return Optional.empty();
			}
			throw ex;
		}
	}

	private <T> T get(String apiKey, String url, Class<T> type) {
		return restClient.get()
				.uri(URI.create(url))
				.header(RIOT_TOKEN_HEADER, apiKey)
				.header(HttpHeaders.ACCEPT, "application/json")
				.retrieve()
				.body(type);
	}

	private static String encodePath(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Riot API path segment must not be blank");
		}
		return UriUtils.encodePathSegment(value.trim(), StandardCharsets.UTF_8);
	}
}
