<template>
  <n-modal v-model:show="isVisible">
    <n-card v-on:close="handleClose" title="방 생성" class="w-full max-w-sm bg-(--ric-bg-card)! border! border-slate-700!" closable>
      <n-form v-bind:model="friendsCreateRoomFormValue" v-bind:rules="friendsCreateRoomFormRules" ref="friendsCreateRoomForm">
        <n-form-item label="방 이름" path="room.title">
          <n-input v-model:value="friendsCreateRoomFormValue.room.title" placeholder="방 이름" />
        </n-form-item>

        <n-form-item label="비밀번호" path="room.password">
          <n-input v-model:value="friendsCreateRoomFormValue.room.password" show-password-on="click" type="password" placeholder="비밀번호" />
        </n-form-item>

        <n-form-item label="최대인원" path="room.capacity">
          <n-input-number v-model:value="friendsCreateRoomFormValue.room.capacity" v-bind:min="2" v-bind:max="4" class="w-full" />
        </n-form-item>

        <n-form-item label="난이도" path="room.difficulty">
          <n-select v-model:value="friendsCreateRoomFormValue.room.difficulty" v-bind:options="difficultyTable" placeholder="난이도" />
        </n-form-item>

        <n-form-item label="프로그래밍 언어" path="room.language">
          <n-select v-model:value="friendsCreateRoomFormValue.room.language" v-bind:options="programmingLanguageTable" placeholder="프로그래밍 언어" />
        </n-form-item>

        <n-form-item label="알고리즘 유형" path="room.algorithm">
          <n-select v-model:value="friendsCreateRoomFormValue.room.algorithm" v-bind:options="algorithmTypeTable" placeholder="알고리즘 유형" />
        </n-form-item>
        
        <n-form-item label="턴 시간" path="room.turnTime">
          <n-slider v-model:value="friendsCreateRoomFormValue.room.turnTime" v-bind:step="5" v-bind:min="40" v-bind:max="80" />
        </n-form-item>

        <div class="flex items-center gap-4 w-full">
          <n-button type="primary" v-on:click="handleCreate" class="flex-1">생성하기</n-button>
          <n-button secondary type="primary" v-on:click="handleClose" class="flex-1">취소</n-button>
        </div>
      </n-form>
    </n-card>
  </n-modal>
</template>

<script setup lang="ts">
import { createRoom } from '@/api/roomService';
import { algorithmTypeTable } from '@/types/game/AlgorithmTypeTable';
import { difficultyTable } from '@/types/game/DifficultyTable';
import { programmingLanguageTable } from '@/types/game/ProgrammingLanguageTable';
import { HttpStatusCode } from 'axios';
import { NButton, NModal, NCard, NForm, NFormItem, NInput, NInputNumber, NSelect, NSlider, type FormInst, useMessage } from 'naive-ui';
import { computed, ref } from 'vue';
import { useMatchStore } from '@/stores/matchStore';

const message = useMessage();
const matchStore = useMatchStore();

const props = defineProps<{
  show: boolean
}>();
const emit = defineEmits<{
  'update:show': [ value: boolean ],
  'created': [],  // 방 생성 성공 시 발생
  'show-waiting': [ roomId: string ]  // 대기방 표시
}>();


const isVisible = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value)
});

const friendsCreateRoomForm = ref<FormInst | null>(null);
const friendsCreateRoomFormValue = ref({
  room: {
    title: '',
    password: '',
    capacity: 2,
    language: '',
    algorithm: '',
    difficulty: '',
    turnTime: 40,
  }
});
const friendsCreateRoomFormRules = {
  room: {
    title: {
      required: true,
      message: '방 이름을 입력해주세요'
    },
    password: {
      required: true,
      message: '방 비밀번호를 입력해주세요'
    },
    capacity: {
      required: true,
      message: '최대 인원 수를 설정해주세요'
    },
    language: {
      required: true,
      message: '프로그래밍 언어를 선택해주세요'
    },
    algorithm: {
      required: true,
      message: '알고리즘 유형을 선택해주세요'
    },
    difficulty: {
      required: true,
      message: '난이도를 선택해주세요'
    },
    turnTime: {
      required: true,
      message: '턴 시간을 설정해주세요'
    }
  }
};

const handleClose = () => {
  emit('update:show', false);
};

const handleCreate = async () => {
  await friendsCreateRoomForm.value?.validate().then(async () => {
    try {
      const resp = await createRoom({
        title: friendsCreateRoomFormValue.value.room.title,
        password: friendsCreateRoomFormValue.value.room.password,
        algorithm: friendsCreateRoomFormValue.value.room.algorithm,
        capacity: friendsCreateRoomFormValue.value.room.capacity,
        difficulty: friendsCreateRoomFormValue.value.room.difficulty,
        language: friendsCreateRoomFormValue.value.room.language,
        turnTime: friendsCreateRoomFormValue.value.room.turnTime,
        mode: 'FRIENDS'
      });
      console.log(resp);
      if (resp.statusCode === HttpStatusCode.Ok) {
        message.success('방이 생성되었습니다.');

        matchStore.setProgrammingLanguage(friendsCreateRoomFormValue.value.room.language);
        
        emit('created'); // 부모에게 방 생성 성공 알림
        emit('show-waiting', resp.data.roomId); // 대기방 표시
        handleClose();
      }
    } catch (err: any) {
    message.warning(err.response.data.message);
    }
  }, (err) => {
    message.warning('방 생성에 실패하였습니다. 다시 시도해주세요.');
  });
};
</script>