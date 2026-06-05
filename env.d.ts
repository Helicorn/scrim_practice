/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 운영 환경 Riot API 프록시 base (Java 백엔드) */
  readonly VITE_RIOT_API_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}
