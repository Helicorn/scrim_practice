<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const saveMessage = ref('')
const saveError = ref('')
const TEAM_SIZE = 5

session.ensurePlayers()
session.ensureTeams()
session.ensureMatchResult()

function getSlot(team: 'red' | 'blue', index: number) {
  const list = team === 'red' ? session.redTeam : session.blueTeam
  return list[index]
}

function isWinner(team: 'red' | 'blue'): boolean {
  return session.matchResult?.winner === team
}

function onWinnerChange(team: 'red' | 'blue', event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  if (checked) {
    session.setWinner(team)
    return
  }
  if (session.matchResult?.winner === team) {
    session.setWinner(null)
  }
}

function onSaveMatch() {
  saveMessage.value = ''
  saveError.value = ''

  const result = session.saveMatchToHistory()
  if (result.ok) {
    saveMessage.value = `경기 결과를 저장했습니다. (총 ${session.matchHistory.length}경기)`
    return
  }
  saveError.value = result.message
}
</script>

<template>
  <section class="result">
    <h1>경기 결과</h1>
    <p class="text-hint">
      승리 팀을 선택하고, 각 플레이어 KDA를 입력하세요. (비워 두어도 됩니다)
    </p>

    <div class="result-board">
      <div class="team-panel team-red">
        <label class="win-check">
          <input
            type="checkbox"
            :checked="isWinner('red')"
            @change="onWinnerChange('red', $event)"
          />
          <span>승리</span>
        </label>

        <div class="team-box">
          <h2 class="team-title">RED</h2>
          <ol class="player-rows">
            <li
              v-for="i in TEAM_SIZE"
              :key="`red-${i - 1}`"
              class="player-row"
            >
              <div class="champion-thumb" aria-hidden="true" />
              <div class="player-info">
                <span class="player-name">
                  {{
                    session.isFilledPlayer(getSlot('red', i - 1))
                      ? session.formatPlayerLabel(getSlot('red', i - 1))
                      : '—'
                  }}
                </span>
                <div class="kda-row">
                  <label class="kda-field">
                    <span class="kda-label">K</span>
                    <input
                      v-model="session.getKda('red', i - 1).kills"
                      type="text"
                      inputmode="numeric"
                      class="input-field kda-input"
                      placeholder="0"
                      aria-label="킬"
                    />
                  </label>
                  <label class="kda-field">
                    <span class="kda-label">D</span>
                    <input
                      v-model="session.getKda('red', i - 1).deaths"
                      type="text"
                      inputmode="numeric"
                      class="input-field kda-input"
                      placeholder="0"
                      aria-label="데스"
                    />
                  </label>
                  <label class="kda-field">
                    <span class="kda-label">A</span>
                    <input
                      v-model="session.getKda('red', i - 1).assists"
                      type="text"
                      inputmode="numeric"
                      class="input-field kda-input"
                      placeholder="0"
                      aria-label="어시스트"
                    />
                  </label>
                </div>
              </div>
            </li>
          </ol>
        </div>
      </div>

      <div class="team-panel team-blue">
        <label class="win-check">
          <input
            type="checkbox"
            :checked="isWinner('blue')"
            @change="onWinnerChange('blue', $event)"
          />
          <span>승리</span>
        </label>

        <div class="team-box">
          <h2 class="team-title">BLUE</h2>
          <ol class="player-rows">
            <li
              v-for="i in TEAM_SIZE"
              :key="`blue-${i - 1}`"
              class="player-row"
            >
              <div class="champion-thumb" aria-hidden="true" />
              <div class="player-info">
                <span class="player-name">
                  {{
                    session.isFilledPlayer(getSlot('blue', i - 1))
                      ? session.formatPlayerLabel(getSlot('blue', i - 1))
                      : '—'
                  }}
                </span>
                <div class="kda-row">
                  <label class="kda-field">
                    <span class="kda-label">K</span>
                    <input
                      v-model="session.getKda('blue', i - 1).kills"
                      type="text"
                      inputmode="numeric"
                      class="input-field kda-input"
                      placeholder="0"
                      aria-label="킬"
                    />
                  </label>
                  <label class="kda-field">
                    <span class="kda-label">D</span>
                    <input
                      v-model="session.getKda('blue', i - 1).deaths"
                      type="text"
                      inputmode="numeric"
                      class="input-field kda-input"
                      placeholder="0"
                      aria-label="데스"
                    />
                  </label>
                  <label class="kda-field">
                    <span class="kda-label">A</span>
                    <input
                      v-model="session.getKda('blue', i - 1).assists"
                      type="text"
                      inputmode="numeric"
                      class="input-field kda-input"
                      placeholder="0"
                      aria-label="어시스트"
                    />
                  </label>
                </div>
              </div>
            </li>
          </ol>
        </div>
      </div>
    </div>

    <div class="result-actions">
      <button type="button" class="btn-save" @click="onSaveMatch">
        경기 결과 저장
      </button>
      <p v-if="saveMessage" class="save-feedback save-success" role="status">
        {{ saveMessage }}
      </p>
      <p v-if="saveError" class="save-feedback save-error" role="alert">
        {{ saveError }}
      </p>
    </div>

    <nav class="result-nav">
      <RouterLink to="/draft">← 밴픽</RouterLink>
      <RouterLink to="/">새 내전 (시작 화면)</RouterLink>
    </nav>
  </section>
</template>

<style scoped>
.result {
  text-align: left;
  max-width: 64rem;
  margin: 0 auto;
}

.result-board {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
  margin-bottom: 1.5rem;
}

.team-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.win-check {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.75rem;
  border: 2px solid var(--color-input-border);
  border-radius: var(--radius-input);
  background: var(--color-input-bg);
  cursor: pointer;
  user-select: none;
}

.team-red .win-check:has(input:checked) {
  border-color: #e84057;
  background: rgba(232, 64, 87, 0.12);
}

.team-blue .win-check:has(input:checked) {
  border-color: #00a8cc;
  background: rgba(0, 168, 204, 0.12);
}

.win-check input {
  width: 1rem;
  height: 1rem;
  margin: 0;
  cursor: pointer;
  accent-color: var(--color-accent);
}

.team-red .win-check input {
  accent-color: #e84057;
}

.team-blue .win-check input {
  accent-color: #00a8cc;
}

.team-box {
  width: 100%;
  border: 2px solid var(--color-input-border);
  background: var(--color-input-bg);
  overflow: hidden;
}

.team-red .team-box {
  border-color: rgba(232, 64, 87, 0.55);
}

.team-blue .team-box {
  border-color: rgba(0, 168, 204, 0.55);
}

.team-title {
  margin: 0;
  padding: 0.35rem 0.5rem;
  font-size: 0.85rem;
  letter-spacing: 0.12em;
  text-align: center;
  color: #fff;
}

.team-red .team-title {
  background: #e84057;
}

.team-blue .team-title {
  background: #00a8cc;
}

.player-rows {
  list-style: none;
  margin: 0;
  padding: 0;
}

.player-row {
  --champion-thumb-size: 3.25rem;
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 0.35rem;
  min-height: calc(var(--champion-thumb-size) + 0.7rem);
  padding-right: 0.35rem;
  border-top: 1px solid var(--color-input-border);
}

.player-row:first-child {
  border-top: none;
}

.champion-thumb {
  width: var(--champion-thumb-size);
  height: var(--champion-thumb-size);
  aspect-ratio: 1;
  margin: 0.35rem;
  border-radius: 4px;
  background: #000;
  border: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
  overflow: hidden;
}

.champion-thumb :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.player-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0.35rem;
  padding: 0.35rem 0.5rem 0.35rem 0;
  min-width: 0;
}

.player-name {
  font-size: 0.85rem;
  line-height: 1.3;
  word-break: break-all;
}

.kda-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.35rem;
}

.kda-field {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}

.kda-label {
  font-size: 0.65rem;
  color: var(--color-text-faint);
  line-height: 1;
}

.kda-input {
  min-height: 1.75rem;
  padding: 0.25rem 0.35rem;
  font-size: 0.8rem;
  text-align: center;
}

.result-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.btn-save {
  padding: 0.55rem 1.25rem;
  border-radius: var(--radius-input);
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #fff;
  font: inherit;
  font-size: 0.95rem;
  cursor: pointer;
  transition: filter 0.2s;
}

.btn-save:hover {
  filter: brightness(1.08);
}

.save-feedback {
  margin: 0;
  font-size: 0.85rem;
}

.save-success {
  color: var(--color-accent);
}

.save-error {
  color: #e84057;
}

.result-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

@media (max-width: 560px) {
  .result-board {
    grid-template-columns: 1fr;
  }
}
</style>
