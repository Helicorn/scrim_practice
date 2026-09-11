package com.civilwar.api;

import com.civilwar.api.dto.request.SaveSessionTeamsRequest;
import com.civilwar.api.dto.response.SaveSessionTeamsResponse;
import com.civilwar.domain.service.SessionTeamsService;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/sessions")
public class SessionTeamsController {

	private final SessionTeamsService sessionTeamsService;

	public SessionTeamsController(SessionTeamsService sessionTeamsService) {
		this.sessionTeamsService = sessionTeamsService;
	}

	@PutMapping("/{sessionCode}/teams")
	public SaveSessionTeamsResponse saveTeams(
			@PathVariable String sessionCode,
			@Valid @RequestBody SaveSessionTeamsRequest request) {
		return sessionTeamsService.saveTeams(sessionCode, request);
	}
}
