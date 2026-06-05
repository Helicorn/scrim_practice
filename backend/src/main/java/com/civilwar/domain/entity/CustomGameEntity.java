package com.civilwar.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.civilwar.domain.enums.CustomGameStatus;
import com.civilwar.domain.enums.SeriesType;
import com.civilwar.domain.enums.YesNo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CUSTOM_GAME")
@Getter
@Setter
@NoArgsConstructor
public class CustomGameEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "GAME_ID")
	private Long gameId;

	@Column(name = "SESSION_CODE", nullable = false, length = 64, unique = true)
	private String sessionCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "SERIES_TYPE", nullable = false, length = 10)
	private SeriesType seriesType = SeriesType.SINGLE;

	@Enumerated(EnumType.STRING)
	@Column(name = "PEERLESS_YN", nullable = false, length = 1)
	private YesNo peerlessYn = YesNo.N;

	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS", nullable = false, length = 30)
	private CustomGameStatus status = CustomGameStatus.CREATED;

	@Column(name = "CURRENT_MATCH_NO", nullable = false)
	private Integer currentMatchNo = 1;

	@Column(name = "RED_SERIES_WINS", nullable = false)
	private Integer redSeriesWins = 0;

	@Column(name = "BLUE_SERIES_WINS", nullable = false)
	private Integer blueSeriesWins = 0;

	@Column(name = "CREATED_AT", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "UPDATED_AT", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "ENDED_AT")
	private LocalDateTime endedAt;

	@OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CustomGamePlayerEntity> players = new ArrayList<>();

	@OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CustomMatchEntity> matches = new ArrayList<>();

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
