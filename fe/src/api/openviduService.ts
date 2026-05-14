// OpenVidu 관련 서비스 전부 주석 처리
// import type { APIResponse } from "@/types/api/APIResponse";
// import type { AxiosResponse } from "axios";
// import apiClient from "./apiClient";

// ----------

// /**
//  * 토큰 생성 요청
//  * @param sessionId 세션 식별번호
//  * @returns {Promise<APIResponse<string>>}
//  */
// export const createToken = async (sessionId: string): Promise<APIResponse<string>> => {
//   const resp: AxiosResponse<APIResponse<string>> = await apiClient.post(`/sessions/${sessionId}/token`);
//   return resp.data;
// };

// /**
//  * 세션 생성 요청
//  * @returns {Promise<APIResponse<string>>}
//  */
// export const createSession = async (): Promise<APIResponse<string>> => {
//   const resp: AxiosResponse<APIResponse<string>> = await apiClient.post('/sessions/create');
//   return resp.data;
// };