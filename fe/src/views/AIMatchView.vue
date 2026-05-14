<template>
  <div class="relative h-screen">
    <DefaultGameLayout v-bind:mode="'AI'" v-bind:emoji="false" />
  </div>
</template>

<script setup lang="ts">
import DefaultGameLayout from '@/components/layout/DefaultGameLayout.vue';
import { useRoute, useRouter } from 'vue-router';
import { useMatchStore } from '@/stores/matchStore';
import { useUserStore } from '@/stores/userStore';
import { useAIWebSocket } from '@/ws/useAIWebSocket';
import { onMounted, ref, watch } from 'vue';

const router = useRouter();
const route = useRoute();
const matchStore = useMatchStore();
const userStore = useUserStore();

// AI WebSocket 기능 가져오기
const { sendAIRequest } = useAIWebSocket();

// 마지막으로 확인된 턴 소유자 ID (레이스 컨디션 방지용)
const lastKnownTurnUserId = ref<string | number | null>(null);

onMounted(() => {
  const routeMatchId = String(route.params.matchId || '');
  if (routeMatchId && !matchStore.gameInfo.matchId) {
    matchStore.setMatchId(routeMatchId);
  }
});

// 타이머와 현재 턴 유저를 실시간으로 감시하여 턴 종료 처리
watch(
  () => [matchStore.gameInfo.remainingTimePerSec, matchStore.gameInfo.currentTurnUserId] as [number, string | number],
  ([newTime, newUserId]) => {
    const matchId = matchStore.gameInfo.matchId;
    const myId = String(userStore.user?.userId);

    // [1] 시간이 남아있을 때는 마지막으로 확인된 턴 소유자를 계속 업데이트
    if (typeof newTime === 'number' && newTime > 0) {
      lastKnownTurnUserId.value = newUserId;
    }

    // [2] 타이머가 0이 되는 순간의 판별 로직
    if (typeof newTime === 'number' && newTime === 0 && matchStore.gameInfo.phaseType === 'PLAYING') {
      // 서버가 0초와 다음 턴을 동시에 보내더라도, 
      // lastKnownTurnUserId는 직전 메시지(1초 시점)의 소유자를 기억하고 있음
      const wasMyTurn = String(lastKnownTurnUserId.value) === myId;

      if (wasMyTurn && matchId) {
        console.info('[AIMatchView] 내 턴 종료 감지 (시간 초과) -> TURN_END 전송');
        sendAIRequest(matchId);
        
        // 중복 전송 방지를 위해 기록 초기화
        lastKnownTurnUserId.value = null;
      }
    }
  },
  { immediate: true }
);

// AI 리포트가 도착하면 자동으로 리포트 페이지로 이동
watch(() => matchStore.aiReport, (newReport) => {
  if (newReport) {
    const matchId = newReport.matchId || matchStore.gameInfo.matchId;
    if (matchId) {
      router.push({ name: 'aiReport', params: { matchId } });
    }
  }
});
</script>
