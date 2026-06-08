package com.civilwar.api;

import java.util.List;

import com.civilwar.api.dto.CheckRiotNeedsRequest;
import com.civilwar.api.dto.CheckRiotNeedsResponse;
import com.civilwar.api.dto.SavedSummonerDto;
import com.civilwar.domain.service.SummonerCatalogService;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("oracle")
@RequestMapping("/api/summoners")
public class SummonerController {

	private final SummonerCatalogService summonerCatalogService;

	public SummonerController(SummonerCatalogService summonerCatalogService) {
		this.summonerCatalogService = summonerCatalogService;
	}

	@GetMapping
	public List<SavedSummonerDto> listSavedSummoners() {
		return summonerCatalogService.listSavedSummoners();
	}

	@PostMapping("/check-riot-needs")
	public CheckRiotNeedsResponse checkRiotNeeds(@Valid @RequestBody CheckRiotNeedsRequest request) {
		return summonerCatalogService.checkRiotNeeds(request.players());
	}
}
