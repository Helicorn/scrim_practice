<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import type { Player } from '@/types/session'

const session = useSessionStore()
const selectedPlayer = ref<Player | null>(null)

const TEAM_SIZE = 5
const POSITION_LABELS = ['탑', '정글', '미드', '원딜', '서포터'] as const

session.ensurePlayers()
session.ensureTeams()

const unassignedPlayers = computed(() =>
  session.players.filter(
    (p) => session.isFilledPlayer(p) && !session.isOnTeam(p),
  ),
)

function isTeamSlotFilled(team: 'red' | 'blue', slotIndex: number): boolean {
  return session.isFilledPlayer(getSlot(team, slotIndex))
}

const isTeamsComplete = computed(() => {
  if (unassignedPlayers.value.length > 0) return false
  for (let i = 0; i < TEAM_SIZE; i += 1) {
    if (!isTeamSlotFilled('red', i) || !isTeamSlotFilled('blue', i)) {
      return false
    }
  }
  return true
})

function isSelected(player: Player): boolean {
  if (!selectedPlayer.value) return false
  return session.playerKey(selectedPlayer.value) === session.playerKey(player)
}

function selectPlayer(player: Player) {
  selectedPlayer.value = isSelected(player) ? null : player
}

function getSlot(team: 'red' | 'blue', slotIndex: number): Player | undefined {
  const teamList = team === 'red' ? session.redTeam : session.blueTeam
  return teamList[slotIndex]
}

function onSlotClick(team: 'red' | 'blue', slotIndex: number) {
  const slot = getSlot(team, slotIndex)

  if (selectedPlayer.value) {
    session.assignPlayer(team, slotIndex, selectedPlayer.value)
    selectedPlayer.value = null
    return
  }

  if (session.isFilledPlayer(slot)) {
    session.clearSlot(team, slotIndex)
  }
}
</script>

<template>
  <section class="teams">
    <h1>팀 배치</h1>
    <p class="text-hint">
      가운데 소환사를 선택한 뒤 좌(레드)·우(블루) 칸을 눌러 배치하세요. 배치된 칸을
      다시 누르면 비웁니다.
    </p>

    <div class="teams-board">
      <div class="team-column team-red">
        <h2 class="team-title">레드</h2>
        <ol class="team-slots">
          <li
            v-for="slotIndex in TEAM_SIZE"
            :key="`red-${slotIndex - 1}`"
            class="team-slot"
            :class="{ filled: session.isFilledPlayer(getSlot('red', slotIndex - 1)) }"
            @click="onSlotClick('red', slotIndex - 1)"
          >
            <span class="position-label">{{ POSITION_LABELS[slotIndex - 1] }}</span>
            <span
              v-if="session.isFilledPlayer(getSlot('red', slotIndex - 1))"
              class="slot-label"
            >
              {{ session.formatPlayerLabel(getSlot('red', slotIndex - 1)) }}
            </span>
            <span v-else class="slot-placeholder">빈 칸</span>
          </li>
        </ol>
      </div>

      <div class="pool-column">
        <p v-if="unassignedPlayers.length === 0" class="pool-empty text-label">
          배치할 소환사가 없습니다.<br />
          소환사 입력에서 이름을 먼저 등록하세요.
        </p>
        <ul v-else class="pool-list">
          <li v-for="(player, index) in unassignedPlayers" :key="index">
            <button
              type="button"
              class="pool-player"
              :class="{ selected: isSelected(player) }"
              @click="selectPlayer(player)"
            >
              {{ session.formatPlayerLabel(player) }}
            </button>
          </li>
        </ul>
      </div>

      <div class="team-column team-blue">
        <h2 class="team-title">블루</h2>
        <ol class="team-slots">
          <li
            v-for="slotIndex in TEAM_SIZE"
            :key="`blue-${slotIndex - 1}`"
            class="team-slot"
            :class="{ filled: session.isFilledPlayer(getSlot('blue', slotIndex - 1)) }"
            @click="onSlotClick('blue', slotIndex - 1)"
          >
            <span class="position-label">{{ POSITION_LABELS[slotIndex - 1] }}</span>
            <span
              v-if="session.isFilledPlayer(getSlot('blue', slotIndex - 1))"
              class="slot-label"
            >
              {{ session.formatPlayerLabel(getSlot('blue', slotIndex - 1)) }}
            </span>
            <span v-else class="slot-placeholder">빈 칸</span>
          </li>
        </ol>
      </div>
    </div>

    <div class="teams-actions">
      <RouterLink to="/players" class="teams-back">← 소환사 입력</RouterLink>
      <RouterLink
        v-if="isTeamsComplete"
        to="/draft"
        class="btn-next"
      >
        다음 →
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.teams {
  text-align: left;
}

.teams-board {
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  gap: 1.5rem;
  min-height: 28rem;
  margin-bottom: 1.5rem;
}

.team-column {
  flex: 0 0 11.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.team-title {
  margin: 0;
  font-size: 1rem;
  text-align: center;
}

.team-red .team-title {
  color: #ff6b7a;
}

.team-blue .team-title {
  color: #5eb3ff;
}

.team-slots {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.team-slot {
  position: relative;
  flex: 1;
  min-height: 3.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
  border: 2px solid var(--color-input-border);
  border-radius: var(--radius-input);
  background: var(--color-input-bg);
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s;
}

.position-label {
  position: absolute;
  top: 0.28rem;
  left: 0.42rem;
  font-size: 0.72rem;
  color: var(--color-text-faint);
  line-height: 1;
}

.team-red .team-slot {
  border-color: rgba(255, 107, 122, 0.45);
}

.team-blue .team-slot {
  border-color: rgba(94, 179, 255, 0.45);
}

.team-red .team-slot.filled {
  border-color: #ff6b7a;
  background: rgba(255, 107, 122, 0.08);
}

.team-blue .team-slot.filled {
  border-color: #5eb3ff;
  background: rgba(94, 179, 255, 0.08);
}

.team-slot:hover {
  border-color: var(--color-accent);
}

.slot-label {
  font-size: 0.9rem;
  text-align: center;
  word-break: break-all;
  line-height: 1.3;
}

.slot-placeholder {
  color: var(--color-text-faint);
  font-size: 0.85rem;
}

.pool-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 0.5rem;
}

.pool-empty {
  margin: 0;
  text-align: center;
  line-height: 1.5;
}

.pool-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.45rem;
  width: 100%;
  max-width: 14rem;
}

.pool-player {
  width: 100%;
  padding: 0.45rem 0.65rem;
  border-radius: var(--radius-input);
  border: 1px solid var(--color-input-border);
  background: var(--color-input-bg);
  color: inherit;
  font: inherit;
  cursor: pointer;
  text-align: center;
  transition: border-color 0.2s, background-color 0.2s;
}

.pool-player:hover {
  border-color: var(--color-accent);
}

.pool-player.selected {
  border-color: var(--color-accent);
  background: rgba(66, 184, 131, 0.12);
}

.teams-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.teams-back {
  color: var(--color-accent);
  text-decoration: none;
  font-size: 0.95rem;
}

.teams-back:hover {
  text-decoration: underline;
}

.btn-next {
  display: inline-flex;
  align-items: center;
  padding: 0.55rem 1.35rem;
  border-radius: var(--radius-input);
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #fff;
  font: inherit;
  font-size: 0.95rem;
  text-decoration: none;
  cursor: pointer;
  transition: filter 0.2s;
}

.btn-next:hover {
  filter: brightness(1.08);
}
</style>
