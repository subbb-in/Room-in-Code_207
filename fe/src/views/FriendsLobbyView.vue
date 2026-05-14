<template>
  <DefaultLayout>
    <div class="flex flex-col gap-8 w-full">
      <div class="flex justify-between items-center gap-4">
        <!-- TITLE -->
        <div class="flex flex-col gap-2">
          <h2 class="font-bold text-3xl">게임 방 목록</h2>
          <p class="text-xl text-(--ric-text-secondary)">릴레이 코딩 경쟁에 참여하고, 랭킹을 올려보세요!</p>
        </div>

        <!-- BUTTON -->
        <div class="flex items-center gap-4">
          <!-- CREATE -->
          <n-button type="primary" size="large" v-on:click="showCreateRoomModal = true">
            <template #icon>
              <NIcon>
                <AddCircleOutlineOutlined />
              </NIcon>
            </template>
            <span>방 생성하기</span>
          </n-button>

          <!-- REFRESH -->
          <n-button secondary type="primary" size="large" v-on:click="fetchRooms">
            <template #icon>
              <NIcon>
                <Refresh />
              </NIcon>
            </template>
            <span>갱신</span>
          </n-button>
        </div>
      </div>

      <!-- FILTER -->
      <div class="bg-(--ric-bg-card) border border-slate-700 rounded-md p-4 sm:p-8">
        <n-form inline v-bind:show-feedback="false" class="w-full! grid! grid-col-1! gap-4 sm:grid-cols-4!">
          <n-form-item label="난이도" class="flex-1! w-full!">
            <n-select v-model:value="filterDifficulty" v-bind:options="difficultyTable" placeholder="난이도" clearable />
          </n-form-item>

          <n-form-item label="프로그래밍 언어" class="flex-1! w-full!">
            <n-select v-model:value="filterLanguage" v-bind:options="programmingLanguageTable" placeholder="프로그래밍 언어" clearable />
          </n-form-item>

          <n-form-item label="알고리즘 유형" class="flex-1! w-full!">
            <n-select v-model:value="filterAlgorithm" v-bind:options="algorithmTypeTable" placeholder="알고리즘 유형" clearable />
          </n-form-item>

          <n-form-item label="방 이름 검색" class="flex-1! w-full!">
            <n-input v-model:value="searchKeyword" placeholder="방 이름을 입력해주세요" clearable />
          </n-form-item>
        </n-form>
      </div>

      <!-- ROOM LIST -->
      <ul class="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <li v-for="room in filteredRoomList" v-bind:key="room.id" class="bg-(--ric-bg-card) border border-slate-700 rounded-md p-4 transition duration-300 hover:border-(--ric-color-primary)/50 hover:-translate-y-1">
          <div class="flex flex-col gap-4">
            <!-- INFO 1 -->
            <div class="flex justify-between items-start gap-4">
              <!-- DIFFICULTY & TITLE -->
              <div class="flex flex-col gap-1">
                <span class="text-amber-400 text-sm">{{ room.diff }}</span>

                <div class="flex items-center gap-4">
                  <h4 class="font-bold text-xl">{{ room.title }}</h4>
                  <span class="text-sm text-(--ric-text-secondary) border border-slate-700 bg-(--ric-bg-popover) rounded-sm px-2 py-1">{{ room.lang }}</span>
                </div>
              </div>

              <!-- LOCK -->
              <n-icon class="text-(--ric-color-primary)">
                <Lock />
              </n-icon>
            </div>
            
            <!-- ALGORITHM TYPES -->
            <span class="font-bold text-sm text-(--ric-color-primary)">#{{ room.algo }}</span>

            <!-- INFO 2 -->
            <div class="flex justify-between items-center gap-4">
              <!-- NUMBER OF PEOPLE -->
              <span class="text-sm text-(--ric-text-secondary)">{{ room.cnt }}/{{ room.max }}</span>

              <!-- BUTTON -->
              <n-button type="primary" v-on:click="handleJoinRoom(room.id)">입장</n-button>
            </div>
          </div>
        </li>
      </ul>
    </div>
  </DefaultLayout>

  <CreateRoomModal v-model:show="showCreateRoomModal" v-on:created="fetchRooms" v-on:show-waiting="handleShowWaiting" />
  <JoinRoomModal v-model:show="showJoinRoomModal" v-bind:room-id="selectedRoomId" v-on:show-waiting="handleShowWaiting" />
  <WaitingRoomPanel v-model:show="showWaitingPanel" v-bind:room-id="waitingRoomId" v-on:cancel="handleCancelWaiting" />
</template>

<script setup lang="ts">
import { getRoomList } from '@/api/roomService';
import CreateRoomModal from '@/components/friends/CreateRoomModal.vue';
import JoinRoomModal from '@/components/friends/JoinRoomModal.vue';
import WaitingRoomPanel from '@/components/friends/WaitingRoomPanel.vue';
import DefaultLayout from '@/components/layout/DefaultLayout.vue';
import { useMatchStore } from '@/stores/matchStore';
import { useUserStore } from '@/stores/userStore';
import { type RoomInfoResponse } from '@/types/api/RoomInfoResponse';
import { algorithmTypeTable } from '@/types/game/AlgorithmTypeTable';
import { difficultyTable } from '@/types/game/DifficultyTable';
import { programmingLanguageTable } from '@/types/game/ProgrammingLanguageTable';
import { Lock } from '@vicons/fa';
import { AddCircleOutlineOutlined } from '@vicons/material';
import { Refresh } from '@vicons/tabler';
import { HttpStatusCode } from 'axios';
import { NButton, NIcon, NForm, NFormItem, NInput, NSelect, type FormInst, useMessage } from 'naive-ui';
import { onMounted, ref, computed } from 'vue';

const userStore = useUserStore();
const matchStore = useMatchStore();
const message = useMessage();
const roomList = ref<RoomInfoResponse[] | null>(null);

const showCreateRoomModal = ref(false);
const showJoinRoomModal = ref(false);
const showWaitingPanel = ref(false);
const selectedRoomId = ref<string>('');
const waitingRoomId = ref<string>('');

// 필터 값들
const filterDifficulty = ref<string | null>(null);
const filterLanguage = ref<string | null>(null);
const filterAlgorithm = ref<string | null>(null);
const searchKeyword = ref('');

// 필터링된 방 목록
const filteredRoomList = computed(() => {
  if (!roomList.value) {
    return [];
  }
  
  return roomList.value.filter(room => {
    // 난이도 필터
    if (filterDifficulty.value && room.diff !== filterDifficulty.value) {
      return false;
    }
    
    // 프로그래밍 언어 필터
    if (filterLanguage.value && room.lang !== filterLanguage.value) {
      return false;
    }
    
    // 알고리즘 유형 필터
    if (filterAlgorithm.value && room.algo !== filterAlgorithm.value) {
      return false;
    }
    
    // 방 이름 검색 (부분 검색, 대소문자 무시)
    if (searchKeyword.value && !room.title.toLowerCase().includes(searchKeyword.value.toLowerCase())) {
      return false;
    }
    
    return true;
  });
});

const fetchRooms = async () => {
  try {
    const resp = await getRoomList({ mode: "FRIENDS" });
    if (resp.statusCode === HttpStatusCode.Ok) {
      roomList.value = resp.data;
    }
  } catch (err: any) {
    message.warning(err.response.data.message);
  }
};

const handleJoinRoom = (roomId: string) => {
  selectedRoomId.value = roomId;
  const selectedRoom = roomList.value?.find(room => room.id === roomId);
  if (selectedRoom?.lang) {
    matchStore.setProgrammingLanguage(selectedRoom.lang);
  }
  showJoinRoomModal.value = true;
};

const handleShowWaiting = (roomId: string) => {
  waitingRoomId.value = roomId;
  showWaitingPanel.value = true;
};

const handleCancelWaiting = async () => {
  try {
    // WebSocket disconnect가 이미 LEAVE 메시지를 처리했으므로
    // 여기서는 방 목록만 갱신
    message.info('방을 나갔습니다.');
    
    // 방을 나간 후 방 목록을 다시 갱신
    await fetchRooms();
  } catch (err: any) {
    message.warning(err.response?.data?.message);
  } finally {
    showWaitingPanel.value = false;
    waitingRoomId.value = '';
  }
};

onMounted(() => {
  fetchRooms();
});
</script>