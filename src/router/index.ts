import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'setup',
    component: () => import('../views/SetupView.vue'),
  },
  {
    path: '/players',
    name: 'players',
    component: () => import('../views/PlayersView.vue'),
  },
  {
    path: '/teams',
    name: 'teams',
    component: () => import('../views/TeamsView.vue'),
  },
  {
    path: '/draft',
    name: 'draft',
    component: () => import('../views/DraftView.vue'),
  },
  {
    path: '/result',
    name: 'result',
    component: () => import('../views/ResultView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  if (to.name === 'setup') return true

  const session = useSessionStore()

  if (to.name === 'players' || to.name === 'teams' || to.name === 'draft') {
    if (session.hasActiveSession || session.hasSetupInProgress) return true
    return { name: 'setup' }
  }

  if (!session.hasActiveSession) {
    return { name: 'setup' }
  }
  return true
})

export default router
