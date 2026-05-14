import type { APIResponse } from "@/types/api/APIResponse";
import type { AxiosResponse } from "axios";
import apiClient from "./apiClient";
import type { UserRankingResponse } from "@/types/contest/UserRankingResponse";
import type { RankingListResponse } from "@/types/contest/RankingListResponse";

// ----------

/**
 * 특정 사용자의 랭킹 정보 조회
 * @param userId 사용자 식별번호
 * @returns {Promise<APIResponse<UserRankingResponse>>}
*/
export const getUserRanking = async (userId: number): Promise<APIResponse<UserRankingResponse>> => {
  const resp: AxiosResponse<APIResponse<UserRankingResponse>> = await apiClient.get(`/ranking/user/${userId}`);
  return resp.data;
};

/**
 * 전체 랭킹 목록 조회
 * @param page 페이지 번호
 * @param size 페이지당 아이템 개수
 * @returns {Promise<APIResponse<RankingListResponse>>}
 */
export const getTotalRanking = async (page: number): Promise<APIResponse<RankingListResponse>> => {
  const resp: AxiosResponse<APIResponse<RankingListResponse>> = await apiClient.get('/ranking', {
    params: { page: page }
  });
  return resp.data;
}

/**
 * 대기열에 참여하기
 * @param language 참여할 언어
 * @returns {Promise<APIResponse<string>>}
 */
export const joinContestQueue = async (language: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post('/rooms/contest/queue', null, { params: { language } });
  return resp.data;
};

/**
 * 대기열 취소하기
 * @param language 취소할 언어
 * @returns {Promise<APIResponse<string>>}
 */
export const cancelContestQueue = async (language: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.delete('/rooms/contest/queue', { params: { language } });
  return resp.data;
};