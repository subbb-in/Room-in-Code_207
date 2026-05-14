import type { APIResponse } from "@/types/api/APIResponse";
import type { AxiosResponse } from "axios";
import apiClient from "./apiClient";

export interface SubmitCodeRequest {
  sourceCode: string;
  language: string;
}

export const submitCode = async (matchId: string, payload: SubmitCodeRequest): Promise<APIResponse<boolean>> => {
  const resp: AxiosResponse<APIResponse<boolean>> = await apiClient.post(`/matches/${matchId}/submit`, payload);
  return resp.data;
};
