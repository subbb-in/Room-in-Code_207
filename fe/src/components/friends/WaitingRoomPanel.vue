<template>
  <n-modal v-model:show="isVisible" v-bind:mask-closable="false" v-bind:close-on-esc="false" v-bind:closable="false">
    <n-card class="max-w-xl! bg-(--ric-bg-card)! border! border-slate-700! p-4!">
      <div class="flex flex-col gap-4">
        <!-- ROW 1 -->
        <div class="flex items-center gap-4">
          <!-- ICON -->
          <n-icon class="text-(--ric-color-primary)" size="32">
            <Users />
          </n-icon>

          <!-- TITLE -->
          <div class="flex flex-col gap-1">
            <h4 class="font-bold text-lg text-(--ric-color-primary)">대기 중...</h4>
            <span class="text-(--ric-text-secondary)">친구의 접속을 기다리는 중입니다.</span>
          </div>
        </div>

        <!-- ROW 2 -->
        <div class="flex flex-col gap-4 border border-slate-700 bg-(--ric-bg-popover) rounded-md p-4">
          <!-- HELP -->
          <p class="text-(--ric-text-tertiary)">설정한 인원 수가 충족될 때까지 기다립니다. 약 15분 간 모이지 않을 경우 매칭이 취소됩니다.</p>

          <!-- PLAYERS -->
          <ul class="flex justify-center items-center gap-4">
            <li v-for="idx in maxCount" v-bind:key="idx" class="flex flex-col items-center gap-1">
              <div v-bind:class="idx <= currentCount ? 'border-(--ric-color-primary)' : 'border-slate-700'" class="size-12 rounded-full border-2 flex justify-center items-center transition-colors duration-300">
                <n-icon>
                  <User />
                </n-icon>
              </div>

              <span v-bind:class="idx <= currentCount ? 'text-(--ric-color-primary)' : 'text-(--ric-color-tertiary)'">{{ idx <= currentCount ? '입장' : '대기' }}</span>
            </li>
          </ul>

          <!-- BUTTON -->
          <n-button v-on:click="handleCancel" type="primary" class="w-full">매칭 취소</n-button>
        </div>
      </div>
    </n-card> 
  </n-modal>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/userStore';
import { useRoomWebSocket } from '@/ws/useRoomWebSocket';
import { computed, onUnmounted, watch } from 'vue';
import { NModal, NCard, NIcon, NButton, useMessage } from 'naive-ui';
import { User, Users } from '@vicons/fa';

const userStore = useUserStore();
const message = useMessage();

const props = defineProps<{
  show: boolean,
  roomId: string,
}>();
const emit = defineEmits<{
  'update:show': [ value: boolean ],
  'cancel': [],
}>();

const isVisible = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value)
});

// 웹 소켓
const { connect, disconnect, connected, currentCount, maxCount, sendMessage } = useRoomWebSocket();

const startMatching = () => {
  if (props.roomId.length === 0) {
    message.warning('방 식별번호가 존재하지 않습니다.');
    return;
  }

  if (!userStore.user?.userId) {
    message.warning('사용자 식별번호가 존재하지 않습니다.');
    return;
  }

  if (!connected.value) {
    connect(props.roomId, userStore.user.userId.toString());
  }
};

const stopMatching = () => {
  if (connected.value) {
    disconnect();
  }
};

const handleCancel = () => {
  stopMatching();
  emit('cancel');
};

const destroy = () => {
  stopMatching();
  emit('update:show', false);
};

watch(() => props.show, (newVal) => {
  if (newVal) startMatching();
  else destroy();
});

onUnmounted(() => {
  destroy();
});
</script>