package com.civilwar.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SUMMONER")
@Getter
@Setter
@NoArgsConstructor
public class SummonerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "SUMMONER_ID")
	private Long summonerId;

	@Column(name = "GAME_NAME", nullable = false, length = 100)
	private String gameName;

	@Column(name = "TAG_LINE", nullable = false, length = 20)
	private String tagLine;

	@Column(name = "PUUID", length = 100)
	private String puuid;

	@Column(name = "RIOT_SUMMONER_ID", length = 100)
	private String riotSummonerId;

	@Column(name = "ACCOUNT_ID", length = 100)
	private String accountId;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "UPDATED_AT", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "summoner", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SummonerRankStatEntity> rankStats = new ArrayList<>();

	@OneToOne(mappedBy = "summoner", cascade = CascadeType.ALL, orphanRemoval = true)
	private SummonerCustomStatEntity customStat;

	@OneToMany(mappedBy = "summoner", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SummonerCustomPositionStatEntity> positionStats = new ArrayList<>();

	@OneToMany(mappedBy = "summoner")
	private List<CustomGamePlayerEntity> gamePlayers = new ArrayList<>();

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
