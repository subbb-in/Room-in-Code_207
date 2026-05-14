import type { APIResponse } from "@/types/api/APIResponse";
import type { AxiosResponse } from "axios";
import apiClient from "./apiClient";

// ----------

/**
 * 로그인 요청
 * @param email 이메일
 * @param password 비밀번호
 * @returns {Promise<APIResponse<string>>}
 */
export const login = async (email: string, password: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post('/auth/login', { email, password }, {
    requiresAuth: false
  });
  return resp.data;
};

/**
 * 로그아웃 요청
 * @returns {Promise<APIResponse<string>>}
 */
export const logout = async (): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post('/auth/logout');
  return resp.data;
};

/**
 * 토큰 재발급 요청
 * @returns {Promise<APIResponse<string>>}
 */
export const reissue = async (): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post('/auth/reissue');
  return resp.data;
};