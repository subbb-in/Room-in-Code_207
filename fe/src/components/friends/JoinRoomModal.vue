<template>
  <n-modal v-model:show="isVisible">
    <n-card v-on:close="handleClose" title="방 입장" class="w-full max-w-sm bg-(--ric-bg-card)! border! border-slate-700!" closable>
      <n-form v-bind:model="joinRoomFormValue" v-bind:rules="joinRoomFormRules" ref="joinRoomForm">
        <n-form-item label="비밀번호" path="room.password">
          <n-input type="password" v-model:value="joinRoomFormValue.room.password" placeholder="비밀번호" show-password-on="click" />
        </n-form-item>

        <n-button type="primary" v-on:click="handleJoin">
          접속
        </n-button>
      </n-form>
    </n-card>
  </n-modal>
</template>

<script setup lang="ts">
import { enterRoom } from '@/api/roomService';
import { HttpStatusCode } from 'axios';
import { NButton, NModal, NCard, NForm, NFormItem, NInput, type FormInst, useMessage } from 'naive-ui';
import { ref, computed } from 'vue';

const message = useMessage();

const props = defineProps<{
  show: boolean,
  roomId?: string | null,
}>();
const emit = defineEmits<{
  'update:show': [ value: boolean ],
  'joined': [],
  'show-waiting': [ value: string ]
}>();

const isVisible = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value)
});

const joinRoomForm = ref<FormInst | null>(null);
const joinRoomFormValue = ref({
  room: {
    password: ''
  }
});
const joinRoomFormRules = {
  room: {
    password: {
      required: true,
      message: '비밀번호를 입력해주세요'
    }
  }
};

const handleClose = () => {
  emit('update:show', false);
};

const handleJoin = async () => {
  await joinRoomForm.value?.validate().then(async () => {
    try {
      // roomId 검증 추가
      if (!props.roomId) {
        message.error('방 ID가 없습니다');
        return;
      }

      const roomId = props.roomId.toString();
      const password = joinRoomFormValue.value.room.password;

      const resp = await enterRoom(roomId, password);
      if (resp.statusCode === HttpStatusCode.Ok) {
        message.success('접속 성공');
        
        // 성공 후 roomId를 부모에 전달
        isVisible.value = false;
        emit('show-waiting', roomId); // 부모의 대기실 표시
        emit('joined');
        return;
      }
    } catch (err: any) {
      message.warning(err.response.data.message);
    }
  }, (err) => {
    message.warning('비밀번호를 입력해주세요');
  });
};
</script>