package com.civilwar.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.civilwar.domain.enums.CustomMatchStatus;
import com.civilwar.domain.enums.TeamColor;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "CUSTOM_MATCH",
		uniqueConstraints = @UniqueConstraint(
				name = "UK_CM_GAME_MATCH_NO",
				columnNames = { "GAME_ID", "MATCH_NO" }))
@Getter
@Setter
@NoArgsConstructor
public class CustomMatchEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MATCH_ID")
	private Long matchId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "GAME_ID", nullable = false)
	private CustomGameEntity game;

	@Column(name = "MATCH_NO", nullable = false)
	private Integer matchNo;

	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS", nullable = false, length = 30)
	private CustomMatchStatus status = CustomMatchStatus.CREATED;

	@Enumerated(EnumType.STRING)
	@Column(name = "WIN_TEAM_COLOR", length = 10)
	private TeamColor winTeamColor;

	@Column(name = "CURRENT_TURN_NO", nullable = false)
	private Integer currentTurnNo = 1;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "UPDATED_AT", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "ENDED_AT")
	private LocalDateTime endedAt;

	@OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CustomPickBanEntity> pickBans = new ArrayList<>();

	@OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CustomMatchPlayerResultEntity> playerResults = new ArrayList<>();

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
