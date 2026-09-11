package com.civilwar.api.dto.response;

public record SessionDraftPickSnapshotDto(
		String teamColor,
		Long summonerId,
		String championKey,
		String championNameKr,
		String imageUrl) {
}
