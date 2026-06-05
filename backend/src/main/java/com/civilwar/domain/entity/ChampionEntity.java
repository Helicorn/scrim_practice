package com.civilwar.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CHAMPION")
@Getter
@Setter
@NoArgsConstructor
public class ChampionEntity {

	@Id
	@Column(name = "CHAMPION_ID")
	private Long championId;

	@Column(name = "CHAMPION_KEY", nullable = false, length = 50)
	private String championKey;

	@Column(name = "CHAMPION_NAME_KR", length = 100)
	private String championNameKr;

	@Column(name = "CHAMPION_NAME_EN", length = 100)
	private String championNameEn;

	@Column(name = "IMAGE_URL", length = 500)
	private String imageUrl;

	@Column(name = "VERSION", length = 30)
	private String version;

	@Column(name = "UPDATED_AT", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	@PreUpdate
	void touchUpdatedAt() {
		updatedAt = LocalDateTime.now();
	}
}
