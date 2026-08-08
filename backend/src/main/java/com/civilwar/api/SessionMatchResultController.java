package com.civilwar.api;

import com.civilwar.api.dto.request.SaveMatchResultRequest;
import com.civilwar.api.dto.response.SaveMatchResultResponse;
import com.civilwar.domain.service.SessionMatchResultService;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/sessions")
public class SessionMatchResultController {

	private final SessionMatchResultService sessionMatchResultService;

	public SessionMatchResultController(SessionMatchResultService sessionMatchResultService) {
		this.sessionMatchResultService = sessionMatchResultService;
	}

	@PostMapping("/{sessionCode}/match-result")
	public SaveMatchResultResponse saveMatchResult(
			@PathVariable String sessionCode,
			@Valid @RequestBody SaveMatchResultRequest request) {
		return sessionMatchResultService.saveMatchResult(sessionCode, request);
	}
}
