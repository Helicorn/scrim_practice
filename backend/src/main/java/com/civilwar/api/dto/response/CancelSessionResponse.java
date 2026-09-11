package com.civilwar.api.dto.response;

public record CancelSessionResponse(
		String sessionCode,
		String status) {
}
