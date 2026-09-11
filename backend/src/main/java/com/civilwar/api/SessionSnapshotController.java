package com.civilwar.api;

import com.civilwar.api.dto.response.SessionSnapshotResponse;
import com.civilwar.domain.service.SessionSnapshotService;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/sessions")
public class SessionSnapshotController {

	private final SessionSnapshotService sessionSnapshotService;

	public SessionSnapshotController(SessionSnapshotService sessionSnapshotService) {
		this.sessionSnapshotService = sessionSnapshotService;
	}

	@GetMapping("/{sessionCode}")
	public SessionSnapshotResponse getSnapshot(@PathVariable String sessionCode) {
		return sessionSnapshotService.getSnapshot(sessionCode);
	}
}
