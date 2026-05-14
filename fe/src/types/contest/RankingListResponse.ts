import type { UserRankingResponse } from './UserRankingResponse';

export interface RankingListResponse {
  currentPage: number;
  totalPage: number;
  totalElements: number;
  content: UserRankingResponse[];
}