<template>
  <div
    class="w-full rounded-2xl border border-cyan-400/40 bg-linear-to-r from-slate-900/90 via-slate-800/80 to-slate-900/90 shadow-[0_0_18px_rgba(31,231,255,0.25)] backdrop-blur px-8 py-8 flex flex-row justify-between items-center gap-6 ring-pulse">
    <div>
      <div class="flex items-start justify-center">
        <div v-if="!userProfile?.rank" class="text-2xl font-bold text-slate-400">순위 정보 없음</div>
        <div v-else class="flex flex-row items-center gap-4">
        <div class="text-base text-primary font-bold text-cyan-400">내 순위</div>
        <span class="text-4xl font-bold">#{{ userProfile?.rank }}위</span>
        </div>
      </div>
    </div>

    <!-- 구분선 -->
    <div class="border border-slate-700/70 h-14 mx-4"></div>

    <img :src="getProfileImageUrl(userStore.user?.profileImageUrl)" class="rounded-full size-16 border-2 border-primary">
    <div class="flex flex-1 flex-col items-start justify-center gap-1">
      <span class="text-xl font-bold">{{ userProfile?.nickname }}</span>
      <span class="text-sm text-slate-400">{{ userProfile?.introText }}</span>
      <span class="text-sm text-cyan-400">{{ getTierByTotalXP(userProfile?.xp) }}</span>
    </div>

    <div class="flex flex-col items-start justify-center">
      <span class="text-sm font-semibold text-cyan-400">총 경험치</span>
      <span class="text-2xl font-bold">{{userProfile?.xp}}XP</span>
    </div>
    
    <div
      class="rounded-lg size-12 border border-slate-500/60 bg-slate-900/60 items-center justify-center text-center flex">
          <NIcon v-bind:color="getTierColorFromTierType(getTierByTotalXP(userProfile?.xp))" size="24" class="items-center">
            <component :is="getTierIconComponentFromTierType(getTierByTotalXP(userProfile?.xp))" />
          </NIcon>
    </div>

    <div>
      <n-select v-model:value="selectedLanguage" :options="programmingLanguageTable" placeholder="언어 선택" class="w-36!"/>
    </div>
    <n-button type="primary" @click="startMatch" class="shadow-[0_0_12px_rgba(34,211,238,0.35)]">
      매칭 시작
    </n-button>
    <StartMatchModal v-model:showModal="showMatchModal" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { NSelect, NButton, NModal, NIcon } from 'naive-ui';
import { useContestStore } from '@/stores/contestStore';
import { useMatchStore } from '@/stores/matchStore';
import { useUserStore } from '@/stores/userStore';
import { getMyInfo } from '@/api/userService';
import { getTierColorFromTierType, getTierIconComponentFromTierType, getTierByTotalXP } from '@/utils/game/TierUtil';
import StartMatchModal from '@/components/contest/StartMatchModal.vue';
import { useContestWebSocket } from '@/ws/useContestWebSocket';
import { useMessage } from 'naive-ui';
import { getProfileImageUrl } from '@/utils/imageUtil';

const { connect, disconnect, connected } = useContestWebSocket();
const contestStore = useContestStore()
const matchStore = useMatchStore()
const userStore = useUserStore()
const userProfile = ref()
const message = useMessage(); 

const showMatchModal = ref(false);
const selectedLanguage = ref<string | null>(null);
const programmingLanguageTable = [
  { label: 'Python (3.8.1)', value: 'PYTHON' },
  { label: 'Java (OpenJDK 13.0.1)', value: 'JAVA' },
  { label: 'C++ (GCC 9.2.0)', value: 'CPP' },
];
const startMatch = async () => {
  if (!selectedLanguage.value) {
    message.warning('언어를 선택해주세요');
    return;
  }

  try {
    // 웹소켓 먼저 연결 (매칭 이벤트 유실 방지)
    console.log("[ContestUserProfile] 웹소켓 연결 시도");
    contestStore.isMatched = false;
    connect();

    matchStore.setProgrammingLanguage(selectedLanguage.value);

    const res = await contestStore.joinQueue(selectedLanguage.value as any);
    showMatchModal.value = true;
  } catch (e) {
    console.error('매칭 큐 참여 실패:', e);
  }
}

onMounted(async () => {
  // 아직 랭킹 데이터가 없으면 getMyInfo 호출해서 저장

  if (!contestStore.contestUserProfile) {
    console.log("아직 랭킹 없음. 사용자 정보 대신 가져올게요.")
    try {
      const response = await getMyInfo();
      userStore.user = response.data;
      userProfile.value = response.data;
      userProfile.value.xp = 0;
    } catch (e) {
      console.error('사용자 정보 조회 오류:', e);
    }
  } else {
    console.log("랭킹정보가 있습니다.")
    userProfile.value = contestStore.contestUserProfile;
  }
});


</script>

<style scoped>
.ring-pulse {
  position: relative;
}

.ring-pulse::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 18px;
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.25), 0 0 28px rgba(34, 211, 238, 0.18);
  animation: pulseGlow 2.6s ease-in-out infinite;
  pointer-events: none;
}

@keyframes pulseGlow {
  0%,
  100% {
    opacity: 0.35;
    box-shadow: 0 0 10px rgba(56, 189, 248, 0.2), 0 0 22px rgba(34, 211, 238, 0.16);
  }
  50% {
    opacity: 0.9;
    box-shadow: 0 0 18px rgba(56, 189, 248, 0.35), 0 0 36px rgba(34, 211, 238, 0.25);
  }
}
</style>