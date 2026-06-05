package com.civilwar.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.civilwar.domain.enums.PositionName;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "SUMMONER_CUSTOM_POSITION_STAT",
		uniqueConstraints = @UniqueConstraint(
				name = "UK_SCPS_SUMMONER_POSITION",
				columnNames = { "SUMMONER_ID", "POSITION_NAME" }))
@Getter
@Setter
@NoArgsConstructor
public class SummonerCustomPositionStatEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "POSITION_STAT_ID")
	private Long positionStatId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SUMMONER_ID", nullable = false)
	private SummonerEntity summoner;

	@Enumerated(EnumType.STRING)
	@Column(name = "POSITION_NAME", nullable = false, length = 20)
	private PositionName positionName;

	@Column(name = "TOTAL_GAMES", nullable = false)
	private Integer totalGames = 0;

	@Column(name = "WINS", nullable = false)
	private Integer wins = 0;

	@Column(name = "LOSSES", nullable = false)
	private Integer losses = 0;

	@Column(name = "WIN_RATE", nullable = false, precision = 5, scale = 2)
	private BigDecimal winRate = BigDecimal.ZERO;

	@Column(name = "TOTAL_KILLS")
	private Integer totalKills = 0;

	@Column(name = "TOTAL_DEATHS")
	private Integer totalDeaths = 0;

	@Column(name = "TOTAL_ASSISTS")
	private Integer totalAssists = 0;

	@Column(name = "AVG_KILLS", precision = 5, scale = 2)
	private BigDecimal avgKills;

	@Column(name = "AVG_DEATHS", precision = 5, scale = 2)
	private BigDecimal avgDeaths;

	@Column(name = "AVG_ASSISTS", precision = 5, scale = 2)
	private BigDecimal avgAssists;

	@Column(name = "AVG_KDA", precision = 6, scale = 2)
	private BigDecimal avgKda;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MOST_CHAMPION_1")
	private ChampionEntity mostChampion1;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MOST_CHAMPION_2")
	private ChampionEntity mostChampion2;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MOST_CHAMPION_3")
	private ChampionEntity mostChampion3;

	@Column(name = "UPDATED_AT", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	@PreUpdate
	void touchUpdatedAt() {
		updatedAt = LocalDateTime.now();
	}
}
