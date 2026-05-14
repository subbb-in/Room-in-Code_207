<template>
  <n-modal v-bind:show="showModal" preset="card" style="width: 700px" :closable="false" :mask-closable="false"
    class="border border-slate-700 rounded-xl! bg-slate-900!">
    <div class="p-4">
      <!-- header -->
      <div class="text-cyan-400 font-bold text-lg">MMR 매칭</div>
      <div class="flex flex-row pb-4 items-start justify-between gap-2 font-bold">
        <div class="text-3xl text-white">
          동일랭크 플레이어 매칭중
        </div>
        <div class="text-cyan-400 text-lg">
          {{ timeLeft }}초
        </div>
      </div>

      <div class="flex flex-row gap-4 p-8 items-start rounded-md font-bold border border-slate-700 bg-slate-700/50!">
        <Icon class="size-10 text-cyan-400">
          <Users /> <!-- Ionicons5 에서 가져옴 -->
        </Icon>
        <div class="flex flex-col gap-2">
          <span v-if="!contestStore.isMatched" class="text-lg">MMR 매칭중</span>
          <span class="text-md pb-4 ">같은 랭크의 4명이 입장할 때까지 대기합니다.</span>
        </div>
      </div>

      <div class="flex flex-row justify-between items-center pt-6">
        <div class="flex items-center text-cyan-400 ">
          <Icon class="size-4 mr-2">
            <Circle />
          </Icon>1분 내 매칭 실패시 복귀합니다.
        </div>
        <n-button @click="requestClose" class="bg-cyan-400!" :textColor="'white'">Cancel</n-button>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { NButton, NModal } from 'naive-ui';
import { Circle, Users } from '@vicons/fa';
import { watch, ref, onUnmounted } from 'vue';
import { useContestStore } from '@/stores/contestStore';
import { useContestWebSocket } from '@/ws/useContestWebSocket';
import { useRouter } from 'vue-router';

const props = defineProps<{
  showModal: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:showModal', value: boolean): void;
}>();

const contestStore = useContestStore();
const router = useRouter();
const { disconnect } = useContestWebSocket();

const timeLeft = ref(60);
const timer = ref<number | null>(null);

const startTimer = () => {
  stopTimer();
  timeLeft.value = 60;
  timer.value = setInterval(() => {
    timeLeft.value--;
    if (timeLeft.value <= 0) {
      handleTimeout();
    }
  }, 1000);
};

const stopTimer = () => {
  if (timer.value) {
    clearInterval(timer.value);
    timer.value = null;
  }
};

const handleTimeout = () => {
  console.log('[StartMatchModal] 매칭 시간 초과');
  stopTimer();
  disconnect();
  emit('update:showModal', false);
};

// 모달이 열릴 때 타이머 시작, 닫힐 때 타이머 중지
watch(
  () => props.showModal,
  (newVal) => {
    if (newVal) {
      startTimer();
    } else {
      stopTimer();
    }
  }
);


watch(
  () => contestStore.isMatched,
  (newVal) => {
    if (newVal) {
      console.log('[StartMatchModal] 게임 시작 - 페이지 이동 중...');
      stopTimer(); // 매칭 성공 시 타이머 중지
        emit('update:showModal', false);
    }
  }
);

const requestClose = () => {
  console.log("매칭 취소 요청");
  stopTimer();
  disconnect();
  emit('update:showModal', false);
};

onUnmounted(() => {
  stopTimer();
});

</script>

<style scoped></style>