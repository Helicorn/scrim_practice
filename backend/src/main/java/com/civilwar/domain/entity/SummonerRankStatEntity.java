package com.civilwar.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.civilwar.domain.enums.PositionName;
import com.civilwar.domain.enums.QueueType;

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
		name = "SUMMONER_RANK_STAT",
		uniqueConstraints = @UniqueConstraint(
				name = "UK_SRS_SUMMONER_QUEUE",
				columnNames = { "SUMMONER_ID", "QUEUE_TYPE" }))
@Getter
@Setter
@NoArgsConstructor
public class SummonerRankStatEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RANK_STAT_ID")
	private Long rankStatId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SUMMONER_ID", nullable = false)
	private SummonerEntity summoner;

	@Enumerated(EnumType.STRING)
	@Column(name = "QUEUE_TYPE", nullable = false, length = 30)
	private QueueType queueType = QueueType.RANKED_SOLO_5x5;

	@Column(name = "TIER", length = 20)
	private String tier;

	@Column(name = "RANK_NAME", length = 10)
	private String rankName;

	@Column(name = "LEAGUE_POINTS")
	private Integer leaguePoints;

	@Column(name = "WINS")
	private Integer wins;

	@Column(name = "LOSSES")
	private Integer losses;

	@Column(name = "WIN_RATE", precision = 5, scale = 2)
	private BigDecimal winRate;

	@Enumerated(EnumType.STRING)
	@Column(name = "MAIN_POSITION", length = 20)
	private PositionName mainPosition;

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
