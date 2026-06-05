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
		name = "CUSTOM_MATCH_PLAYER_RESULT",
		uniqueConstraints = @UniqueConstraint(
				name = "UK_CMPR_MATCH_SUMMONER",
				columnNames = { "MATCH_ID", "SUMMONER_ID" }))
@Getter
@Setter
@NoArgsConstructor
public class CustomMatchPlayerResultEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RESULT_ID")
	private Long resultId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MATCH_ID", nullable = false)
	private CustomMatchEntity match;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SUMMONER_ID", nullable = false)
	private SummonerEntity summoner;

	@Enumerated(EnumType.STRING)
	@Column(name = "TEAM_COLOR", nullable = false, length = 10)
	private TeamColor teamColor;

	@Enumerated(EnumType.STRING)
	@Column(name = "POSITION_NAME", length = 20)
	private PositionName positionName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CHAMPION_ID")
	private ChampionEntity champion;

	@Enumerated(EnumType.STRING)
	@Column(name = "WIN_YN", nullable = false, length = 1)
	private YesNo winYn;

	@Column(name = "KILLS")
	private Integer kills;

	@Column(name = "DEATHS")
	private Integer deaths;

	@Column(name = "ASSISTS")
	private Integer assists;

	@Column(name = "DAMAGE")
	private Integer damage;

	@Column(name = "CS")
	private Integer cs;

	@Column(name = "NOTE", length = 500)
	private String note;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
