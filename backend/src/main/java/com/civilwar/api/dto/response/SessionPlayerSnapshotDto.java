package com.civilwar.api.dto.response;

public record SessionPlayerSnapshotDto(
		Long summonerId,
		String gameName,
		String tagLine,
		String puuid,
		String teamColor,
		String positionName,
		Integer sortOrder) {
}
