package com.civilwar.api;

import com.civilwar.api.dto.request.SavePickBanRequest;
import com.civilwar.api.dto.response.SavePickBanResponse;
import com.civilwar.domain.service.SessionPickBanService;

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
public class SessionPickBanController {

	private final SessionPickBanService sessionPickBanService;

	public SessionPickBanController(SessionPickBanService sessionPickBanService) {
		this.sessionPickBanService = sessionPickBanService;
	}

	@PostMapping("/{sessionCode}/pick-ban")
	public SavePickBanResponse savePickBan(
			@PathVariable String sessionCode,
			@Valid @RequestBody SavePickBanRequest request) {
		return sessionPickBanService.savePickBan(sessionCode, request);
	}
}
