<template>
  <DefaultLayout>
      <div v-confetti="{ particleCount: 200, force: 0.3 }" />
      <div class="text-6xl font-bold text-yellow-400 mb-8">랭킹전 결과</div>
      <!-- 화면 중앙 창 -->
      <div
        class="w-2xl p-8 rounded-2xl shadow-[0_0_15px_#ffd700] flex flex-col justify-center bg-yellow-100/20 dark:bg-slate-800 relative">
        <div class="flex flex-row gap-4 items-center">
          <div class="rounded-sm size-12 border bg-input-bg items-center justify-center flex
          border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900">
          <Icon class="size-8 text-yellow-400">
            <Medal />
          </Icon>
          </div>
          <span class="text-2xl font-bold text-center text-yellow-400">승리자</span>
        </div>
        <Icon>
          <Trophy class="absolute size-25 top-4 right-4 text-yellow-400 opacity-50"/>
        </Icon>

        <!-- 승리자 -->
        <div class="w-2/5 self-center border-2 border-yellow-400 rounded-2xl shadow-[0_0_18px_10px_#facc15]
        flex flex-col items-center p-8 gap-1 bg-slate-100 dark:bg-slate-900">
          <div class="rounded-full shadow-[0_0_20px_#facc15] size-12 mb-4">
            <img src="https://picsum.photos/200" class="rounded-full size-full ">
          </div>
          <span class="text-xs">닉네임 넣는곳</span>
          <span class="text-xs text-yellow-400 font-bold">+150 XP</span>
        </div>

        <!-- 패배자들 -->
        <div class="text-xs text-slate-400 py-2">보상 분배 내역</div>
        <div class="flex justify-between gap-2">
          <div v-for="player in players" :key="player.userId"
            class="flex flex-col items-center w-1/3 p-4 border border-yellow-400 bg-white dark:border-slate-700 dark:bg-slate-900 rounded-xl">
            <img src="https://picsum.photos/200" class="rounded-full size-12 mb-2 ">
            <span class="text-xs">닉네임 넣는곳</span>
            <span class="text-xs text-cyan-400 font-bold">+150 XP</span>
          </div>
        </div>
        <!-- 분석페이지로 넘어갑니다. -->
        <div class="text-sm pt-3 text-slate-700 dark:text-slate-300 self-center font-bold">5초 후 AI분석 리포트 페이지로 넘어갑니다.
        </div>
      </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import DefaultLayout from '@/components/layout/DefaultLayout.vue';
import { vConfetti } from '@neoconfetti/vue';


//더미데이터용
//-------------------------------------------------------------------
import { ref } from 'vue'
import { Medal, Trophy } from '@vicons/fa';
interface Player {
  userId: number
  profileImgUrl: string
  hp: number
  status: boolean
}

const players = ref<Player[]>([
  {
    userId: 1,
    profileImgUrl: 'https://picsum.photos/200',
    hp: 85,
    status: true,
  },
  {
    userId: 2,
    profileImgUrl: 'https://picsum.photos/200',
    hp: 42,
    status: false,
  },
  {
    userId: 3,
    profileImgUrl: 'https://picsum.photos/200',
    hp: 100,
    status: true,
  },
])
//---------------------------------------------------------------------------

</script>

<style scoped></style>