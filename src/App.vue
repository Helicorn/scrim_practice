<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()

const sessionBadge = computed(() =>
  session.hasActiveSession ? session.formatSessionSummary() : '',
)
</script>

<template>
  <div class="app">
    <header>
      <h1>내전</h1>
      <p v-if="sessionBadge" class="session-badge">{{ sessionBadge }}</p>
      <nav class="main-nav">
        <RouterLink to="/">시작</RouterLink>
        <RouterLink to="/players">소환사</RouterLink>
        <RouterLink to="/teams">팀 배치</RouterLink>
        <RouterLink to="/draft">밴픽</RouterLink>
        <RouterLink to="/result">결과</RouterLink>
      </nav>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app {
  width: 100%;
  max-width: 72rem;
  margin: 0 auto;
  padding: 1rem;
  box-sizing: border-box;
}

header h1 {
  font-size: 1.25rem;
  margin: 0 0 0.35rem;
}

.session-badge {
  margin: 0 0 0.65rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
  line-height: 1.35;
}

.main-nav {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.main-nav a.router-link-active {
  color: #42b883;
}
</style>
