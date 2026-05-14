import type { APIResponse } from "@/types/api/APIResponse";
import type { UserProfileDto } from "@/types/api/dto/UserProfileDto";
import type { UserProfileResponse } from "@/types/api/UserProfileResponse";
import type { AxiosResponse } from "axios";
import apiClient from "./apiClient";

// ----------

// /**
//  * 프로필 수정 요청
//  * @param nickname 닉네임
//  * @param profileImageUrl 프로필 이미지 주소
//  * @param introText 자기소개
//  * @returns {Promise<APIResponse<UserProfileResponse>>}
//  */
// export const editProfile = async (nickname?: string, profileImageUrl?: string, introText?: string): Promise<APIResponse<UserProfileResponse>> => {
//   const resp: AxiosResponse<APIResponse<UserProfileResponse>> = await apiClient.put('/', {
//     nickname, profileImageUrl, introText
//   });
//   return resp.data;
// };

/**
 * 프로필 수정 요청
 * @param profile 프로필
 * @param file 프로필 이미지 파일
 * @returns {Promise<APIResponse<UserProfileResponse>>}
 */
export const editProfile = async (profile: UserProfileDto, file?: File): Promise<APIResponse<UserProfileResponse>> => {
  const formData = new FormData();
  formData.append("profile", JSON.stringify(profile));
  if (file) formData.append("file", file);
  
  const resp: AxiosResponse<APIResponse<UserProfileResponse>> = await apiClient.patch('/users', formData);
  return resp.data;
};

/**
 * 회원가입 요청
 * @param email 이메일
 * @param password 비밀번호
 * @param nickname 닉네임
 * @returns {Promise<APIResponse<string>>}
 */
export const signup = async (email: string, password: string, nickname: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post('/users', {
    email, password, nickname
  }, {
    requiresAuth: false
  });
  return resp.data;
};

/**
 * 비밀번호 변경 요청 (마이페이지 전용)
 * @param oldPassword 기존 비밀번호
 * @param newPassword 새 비밀번호
 * @returns {Promise<APIResponse<string>>}
 */
export const changePassword = async (oldPassword: string, newPassword: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.put('/users/password', {
    oldPassword, newPassword
  });
  return resp.data;
};

/**
 * 비밀번호 변경 요청 (비밀번호 찾기 전용)
 * @param token 토큰
 * @param newPassword 새 비밀번호
 * @returns {Promise<APIResponse<string>>}
 */
export const resetPassword = async (token: string, newPassword: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post('/users/password/reset', {
    newPassword
  }, { 
    params: { token },
    requiresAuth: false
  });
  return resp.data;
};

/**
 * 비밀번호 변경 링크 요청 (비밀번호 찾기 전용)
 * @param email 이메일
 * @returns {Promise<APIResponse<string>>}
 */
export const requestResetPasswordLink = async (email: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post('/users/password/forgot', { email }, {
    requiresAuth: false
  });
  return resp.data;
};

/**
 * 이메일 인증 코드 검증 요청
 * @param email 이메일
 * @param code 인증 코드
 * @returns {Promise<APIResponse<string>>}
 */
export const checkAuthCode = async (email: string, code: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post('/users/email-verification/verify', {
    email, code
  }, {
    requiresAuth: false
  });
  return resp.data;
};

/**
 * 이메일 인증 코드 요청
 * @param email 이메일
 * @returns {Promise<APIResponse<string>>}
 */
export const requestAuthCode = async (email: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post('/users/email-verification/request', {
    email
  }, { 
    headers: { 'Content-Type': 'application/json' },
    requiresAuth: false
  });
  return resp.data;
};

/**
 * 회원탈퇴 요청
 * @param userId 사용자 식별번호
 * @param password 비밀번호
 * @returns {Promise<APIResponse<string>>}
 */
export const deleteAccount = async (userId: number, password: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.post('/users/delete', { 
    userId, password
  });
  return resp.data;
};

/**
 * 사용자 정보 요청
 * @param userId 사용자 식별번호
 * @returns {Promise<APIResponse<UserProfileResponse>>}
 */
export const getUserInfo = async (userId: number): Promise<APIResponse<UserProfileResponse>> => {
  const resp: AxiosResponse<APIResponse<UserProfileResponse>> = await apiClient.get(`/users/${userId}`);
  return resp.data;
};

/**
 * 비밀번호 변경 요청의 토큰 검증 (비밀번호 찾기 전용)
 * @param token 토큰
 * @returns {Promise<APIResponse<string>>}
 */
export const checkResetPasswordToken = async (token: string): Promise<APIResponse<string>> => {
  const resp: AxiosResponse<APIResponse<string>> = await apiClient.get('/users/password/reset/check', {
    params: { token },
    requiresAuth: false
  });
  return resp.data;
};

/**
 * 내 정보 요청
 * @returns {Promise<APIResponse<UserProfileResponse>>}
 */
export const getMyInfo = async (): Promise<APIResponse<UserProfileResponse>> => {
  const resp: AxiosResponse<APIResponse<UserProfileResponse>> = await apiClient.get('/users/me');
  return resp.data;
};