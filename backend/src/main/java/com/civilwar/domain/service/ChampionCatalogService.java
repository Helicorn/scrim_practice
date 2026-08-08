package com.civilwar.domain.service;

import com.civilwar.domain.entity.ChampionEntity;
import com.civilwar.domain.repository.ChampionRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("oracle")
@Transactional
public class ChampionCatalogService {

	private final ChampionRepository championRepository;

	public ChampionCatalogService(ChampionRepository championRepository) {
		this.championRepository = championRepository;
	}

	public ChampionEntity upsertByKey(String championKey, String championNameKr, String imageUrl) {
		String key = championKey.trim();
		if (key.isEmpty()) {
			throw new IllegalArgumentException("챔피언 key가 비어 있습니다.");
		}

		return championRepository.findByChampionKey(key).orElseGet(() -> {
			ChampionEntity created = new ChampionEntity();
			created.setChampionId(nextChampionId());
			created.setChampionKey(key);
			created.setChampionNameKr(blankToNull(championNameKr));
			created.setChampionNameEn(key);
			created.setImageUrl(blankToNull(imageUrl));
			return championRepository.save(created);
		});
	}

	private Long nextChampionId() {
		return championRepository.findMaxChampionId().orElse(0L) + 1L;
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
