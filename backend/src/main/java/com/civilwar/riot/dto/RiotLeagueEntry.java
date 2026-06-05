package com.civilwar.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RiotLeagueEntry(
		@JsonProperty("queueType") String queueType,
		@JsonProperty("tier") String tier,
		@JsonProperty("rank") String rankName,
		@JsonProperty("leaguePoints") int leaguePoints,
		@JsonProperty("wins") int wins,
		@JsonProperty("losses") int losses) {
}
