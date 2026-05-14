import type { APIResponse } from "@/types/api/APIResponse";
import type { RoomCreateDto } from "@/types/api/dto/RoomCreateDto";
import type { RoomCreateResponse } from "@/types/api/RoomCreateResponse";
import type { RoomInfoResponse } from "@/types/api/RoomInfoResponse";
import type { RoomLeaveResponse } from "@/types/api/RoomLeaveResponse";
import type { AxiosResponse } from "axios";
import apiClient from "./apiClient";
import type { RoomSearchDto } from "@/types/api/dto/RoomSearchDto";

// ----------

/**
 * 방 목록 조회 요청
 * @param language 프로그래밍 언어
 * @param difficulty 난이도
 * @param algorithm 알고리즘 유형
 * @param mode 모드
 * @returns {Promise<APIResponse<RoomInfoResponse[]>>}
 */
export const getRoomList = async (roomSearch: RoomSearchDto): Promise<APIResponse<RoomInfoResponse[]>> => {
  const resp: AxiosResponse<APIResponse<RoomInfoResponse[]>> = await apiClient.get('/rooms', {
    params: roomSearch
  });
  return resp.data;
};

/**
 * 방 생성 요청
 * @param roomInfo 방 생성 정보
 * @returns {Promise<APIResponse<RoomCreateResponse>>}
 */
export const createRoom = async (roomInfo: RoomCreateDto): Promise<APIResponse<RoomCreateResponse>> => {
  const resp: AxiosResponse<APIResponse<RoomCreateResponse>> = await apiClient.post('/rooms', roomInfo);
  return resp.data;
};

/**
 * 방 접속 요청
 * @param roomId 방 식별번호 (UUID)
 * @param password 방 비밀번호
 * @returns {Promise<APIResponse<string>>}
 */
export const enterRoom = async (roomId: string, password: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post(`/rooms/${roomId}/participants`, { password });
  return resp.data;
};

/**
 * 방 퇴장 요청
 * @param roomId 방 식별번호 (UUID)
 * @param userId 사용자 식별번호
 * @returns {Promise<APIResponse<RoomLeaveResponse>>}
 */
export const leaveRoom = async (roomId: string, userId: string): Promise<APIResponse<RoomLeaveResponse>> => {
  const resp: AxiosResponse<APIResponse<RoomLeaveResponse>> = await apiClient.delete(`/rooms/${roomId}/participants/${userId}`);
  return resp.data;
};

/**
 * 경쟁전 모드 대기열 입장 요청(?)
 * @param language 프로그래밍 언어
 * @returns {Promise<APIResponse<string>>}
 */
export const enterContestQueue = async (language: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post(`/rooms/contest/queue`, null, {
    params: { language }
  });
  return resp.data;
};

/**
 * 경쟁전 모드 대기열 퇴장 요청(?)
 * @param language 프로그래밍 언어
 * @returns {Promise<APIResponse<string>>}
 */
export const leaveContestQueue = async (language: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.delete(`/rooms/contest/queue`, { params: { language }});
  return resp.data;
};

/**
 * AI 모드 입장 요청(?)
 * @param difficulty 난이도
 * @param language 프로그래밍 언어
 * @param algorithm 알고리즘 유형
 * @param turnTime 턴 시간
 * @returns {Promise<APIResponse<string>>}
 */
export const enterAI = async (difficulty: string, language: string, algorithm: string, turnTime: number): Promise<APIResponse<RoomCreateResponse>> => {
  const resp: AxiosResponse<APIResponse<RoomCreateResponse>> = await apiClient.post(`/rooms/ai`, { difficulty, language, algorithm, turnTime });
  return resp.data;
};