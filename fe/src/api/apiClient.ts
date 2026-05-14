import { useUserStore } from "@/stores/userStore";
import axios, { AxiosError, HttpStatusCode, type AxiosRequestConfig } from "axios";
import { reissue } from "./authService";

/**
 * API 요청 설정에 추가 메타정보
 */
declare module 'axios' {
  interface AxiosRequestConfig {
    /** token 재발급 필요 여부 (기본값: true) */
    requiresAuth?: boolean;
  }
}

const runtimeOrigin = typeof window !== 'undefined' ? `${window.location.origin}/` : '/';
const apiBase = import.meta.env.VITE_API_BASE ?? runtimeOrigin;
const normalizedBase = apiBase.endsWith('/') ? apiBase : `${apiBase}/`;

const apiClient = axios.create({
  baseURL: `${normalizedBase}api`,
  withCredentials: true,
});

// ----------
// Request Interceptor: Authorization 헤더 추가
// ----------

apiClient.interceptors.request.use((config) => {
  const userStore = useUserStore();
  // requiresAuth가 false가 아니면 (기본값 true) token 추가
  if (config.requiresAuth !== false && userStore?.accessToken?.length > 0) {
    config.headers.Authorization = `Bearer ${userStore.accessToken}`;
  }
  
  return config;
});

// ----------
// Response Interceptor: 401/403 시 토큰 재발급 처리
// ----------

apiClient.interceptors.response.use(
  response => response, 
  async (error: AxiosError) => {
    const userStore = useUserStore();
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };

    // requiresAuth가 false이면 재시도 안 함
    if (originalRequest.requiresAuth === false) {
      return Promise.reject(error);
    }

    // 401 또는 403 에러 시 토큰 재발급 시도
    if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // 토큰 재발급 요청
        const resp = await reissue();
        if (resp.statusCode === HttpStatusCode.Ok && resp.data) {
          // 새 토큰 저장
          userStore.accessToken = resp.data;

          // 헤더에 새 토큰 붙이고 재시도
          originalRequest.headers = {
            ...originalRequest.headers,
            Authorization: `Bearer ${resp.data}`
          };

          return apiClient(originalRequest);
        }
      } catch (err) {
        // 토큰 재발급 실패
        console.error('토큰 재발급 실패: ' + err);
        
        userStore.user = null;
        userStore.accessToken = '';
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
