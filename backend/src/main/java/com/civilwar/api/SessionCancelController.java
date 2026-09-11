package com.civilwar.api;

import com.civilwar.api.dto.response.CancelSessionResponse;
import com.civilwar.domain.service.SessionCancelService;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/sessions")
public class SessionCancelController {

	private final SessionCancelService sessionCancelService;

	public SessionCancelController(SessionCancelService sessionCancelService) {
		this.sessionCancelService = sessionCancelService;
	}

	@PostMapping("/{sessionCode}/cancel")
	public CancelSessionResponse cancelSession(@PathVariable String sessionCode) {
		return sessionCancelService.cancelSession(sessionCode);
	}
}
