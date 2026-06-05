package com.civilwar.api;

import com.civilwar.api.dto.RegisterSessionPlayersRequest;
import com.civilwar.api.dto.RegisterSessionPlayersResponse;
import com.civilwar.domain.service.SessionPlayersService;

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
public class SessionPlayersController {

	private final SessionPlayersService sessionPlayersService;

	public SessionPlayersController(SessionPlayersService sessionPlayersService) {
		this.sessionPlayersService = sessionPlayersService;
	}

	@PostMapping("/{sessionCode}/players")
	public RegisterSessionPlayersResponse registerPlayers(
			@PathVariable String sessionCode,
			@Valid @RequestBody RegisterSessionPlayersRequest request) {
		return sessionPlayersService.registerPlayers(
				sessionCode,
				request.seriesType(),
				request.peerless(),
				request.players());
	}
}
