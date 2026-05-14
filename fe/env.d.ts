/// <reference types="vite/client" />
interface ImportMetaEnv {
  // 여기에 사용하는 환경 변수 타입을 정의합니다.
  readonly VITE_API_BASE: string;
  readonly VITE_WS_BASE: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}