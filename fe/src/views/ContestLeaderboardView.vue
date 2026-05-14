<template>
<DefaultLayout> 

  <div class="min-w-5xl p-12">
    <!-- 사용자 정보 영역: 순위, 상위 n%, 프로필사진, 닉네임, 등급, XP, 배지, 언어선택, 매칭시작버튼 -->
    <ContestUserProfile />
    <!-- 명예의 전당 -->
    <ContestRanking />
    <!-- 페이지네이션 -->
    <div class="flex justify-center my-8">
      <n-pagination 
        v-model:page="contestStore.currentPage" 
        v-bind:page-count="contestStore.totalPage"
        v-bind:on-update:page="changePage"
        :page-size="10"
        show-size-picker
        :page-sizes="[10, 20, 50]"
      />
    </div>
  </div>
</DefaultLayout>

</template>

<script setup lang="ts">
import ContestRanking from '@/components/contest/ContestRanking.vue';
import ContestUserProfile from '@/components/contest/ContestUserProfile.vue';
import { NPagination } from 'naive-ui';
import { useContestStore } from '@/stores/contestStore';
import { onMounted } from 'vue';
import DefaultLayout from '@/components/layout/DefaultLayout.vue';

const contestStore = useContestStore();

const changePage = (page: number) => {
  contestStore.currentPage = page;
  contestStore.getRankingData();
};

onMounted(() => {
  if (contestStore.rankingData.length === 0) {
    contestStore.getRankingData();
  }
  // 항상 본인 랭킹 정보 최신화
  contestStore.getContestUserProfile();
});
</script>

<style scoped></style>