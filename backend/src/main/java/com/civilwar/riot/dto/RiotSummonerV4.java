package com.civilwar.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Summoner-V4 응답 (2025-07 이후 id/accountId 제거됨) */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RiotSummonerV4(String puuid, Integer profileIconId, Long revisionDate, Long summonerLevel) {
}
