package com.civilwar.domain.entity;

import java.time.LocalDateTime;

import com.civilwar.domain.enums.PositionName;
import com.civilwar.domain.enums.TeamColor;
import com.civilwar.domain.enums.YesNo;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "CUSTOM_GAME_PLAYER",
		uniqueConstraints = @UniqueConstraint(
				name = "UK_CGP_GAME_SUMMONER",
				columnNames = { "GAME_ID", "SUMMONER_ID" }))
@Getter
@Setter
@NoArgsConstructor
public class CustomGamePlayerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "GAME_PLAYER_ID")
	private Long gamePlayerId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "GAME_ID", nullable = false)
	private CustomGameEntity game;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SUMMONER_ID", nullable = false)
	private SummonerEntity summoner;

	@Enumerated(EnumType.STRING)
	@Column(name = "TEAM_COLOR", length = 10)
	private TeamColor teamColor;

	@Enumerated(EnumType.STRING)
	@Column(name = "POSITION_NAME", length = 20)
	private PositionName positionName;

	@Enumerated(EnumType.STRING)
	@Column(name = "IS_CAPTAIN_YN", nullable = false, length = 1)
	private YesNo captainYn = YesNo.N;

	@Column(name = "PLAYER_SCORE")
	private Integer playerScore = 0;

	@Column(name = "SORT_ORDER")
	private Integer sortOrder;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
