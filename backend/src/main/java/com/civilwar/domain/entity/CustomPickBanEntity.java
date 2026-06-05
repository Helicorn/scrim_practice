package com.civilwar.domain.entity;

import java.time.LocalDateTime;

import com.civilwar.domain.enums.PickBanAction;
import com.civilwar.domain.enums.TeamColor;

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
		name = "CUSTOM_PICK_BAN",
		uniqueConstraints = @UniqueConstraint(
				name = "UK_CPB_MATCH_TURN",
				columnNames = { "MATCH_ID", "TURN_NO" }))
@Getter
@Setter
@NoArgsConstructor
public class CustomPickBanEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "PICK_BAN_ID")
	private Long pickBanId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MATCH_ID", nullable = false)
	private CustomMatchEntity match;

	@Column(name = "TURN_NO", nullable = false)
	private Integer turnNo;

	@Enumerated(EnumType.STRING)
	@Column(name = "TEAM_COLOR", nullable = false, length = 10)
	private TeamColor teamColor;

	@Enumerated(EnumType.STRING)
	@Column(name = "ACTION_TYPE", nullable = false, length = 10)
	private PickBanAction actionType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CHAMPION_ID", nullable = false)
	private ChampionEntity champion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SUMMONER_ID")
	private SummonerEntity summoner;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
