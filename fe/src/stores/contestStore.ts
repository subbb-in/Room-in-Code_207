
import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { getUserRanking, getTotalRanking, joinContestQueue } from '@/api/contestService';
import type { UserRankingResponse } from '@/types/contest/UserRankingResponse';
import { useUserStore } from './userStore';

export const useContestStore = defineStore('contest', () => {
  const userStore = useUserStore();
  const contestUserProfile = ref<UserRankingResponse>();
  const rankingData = ref<UserRankingResponse[]>([]);
  const currentPage = ref<number>(1);
  const totalPage = ref<number>();
  const isMatched = ref(false);

  //사용자 랭킹 정보 조회
  const getContestUserProfile = async () => {
    try {
      if (!userStore.user?.userId) {
        console.warn('userId가 아직 로드되지 않았습니다');
        return;
      }
      const res = await getUserRanking(userStore.user?.userId);
      console.log('내 프로필 API 원본 응답:', res);
      contestUserProfile.value = res.data;
      console.log('스토어에 저장된 내 프로필:', contestUserProfile.value);
    } catch (e: any) {
      console.error('❌ 내 프로필 조회 실패:', e.response?.status, e.config.url);
    }
  }




  const getRankingData = async () => {
    try {

      const response = await getTotalRanking(currentPage.value);
      console.log('전체 랭킹 API 원본 응답:', response.data);
      const { content, currentPage: current, totalPage: total } = response.data;
      rankingData.value = content;
      currentPage.value = current;
      totalPage.value = total;

      console.log('현재 페이지 데이터(content):', rankingData.value);
      console.log(`페이지 정보: ${currentPage.value} / ${totalPage.value}`);

    } catch (e: any) {
      console.error('❌ 랭킹 리스트 조회 실패:', e.response?.data);
    }
  }

  // 매칭 큐에 참여
  const joinQueue = async (language: string) => {
    try {
      console.log("언어 : " + language);
      const res = await joinContestQueue(language);
      console.log('✅ 매칭 큐 참여 성공:', res);
      return res;
    } catch (e: any) {
      console.error('❌ 매칭 큐 참여 실패:', e.response?.data);
      throw e;
    }
  };


  return {
    contestUserProfile,
    rankingData,
    currentPage,
    totalPage,
    getContestUserProfile,
    getRankingData,
    joinQueue,
    isMatched,
  };
});
