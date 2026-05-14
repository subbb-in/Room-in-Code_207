<template>
  <n-modal v-model:show="isVisible">
    <n-card
      title="AI 연습모드 설정"
      class="w-full max-w-md bg-(--ric-bg-card)! border! border-slate-700!"
      closable
      @close="handleClose"
    >
      <n-form ref="aiForm" :model="formValue" :rules="formRules">
        <n-form-item label="난이도" path="difficulty">
          <n-select v-model:value="formValue.difficulty" :options="difficultyTable" placeholder="난이도" />
        </n-form-item>

        <n-form-item label="프로그래밍 언어" path="language">
          <n-select v-model:value="formValue.language" :options="programmingLanguageTable" placeholder="프로그래밍 언어" />
        </n-form-item>

        <n-form-item label="알고리즘 유형" path="algorithm">
          <n-select v-model:value="formValue.algorithm" :options="algorithmTypeTable" placeholder="알고리즘 유형" />
        </n-form-item>

        <n-form-item label="턴 시간" path="turnTime">
          <div class="flex items-center gap-3 w-full">
            <n-slider v-model:value="formValue.turnTime" :step="5" :min="40" :max="80" class="flex-1" />
            <span class="text-sm text-(--ric-text-secondary)">{{ formValue.turnTime }}초</span>
          </div>
        </n-form-item>

        <div class="flex gap-3">
          <n-button type="primary" class="flex-1" @click="handleStart">시작하기</n-button>
          <n-button secondary class="flex-1" @click="handleClose">취소</n-button>
        </div>
      </n-form>
    </n-card>
  </n-modal>
</template>

<script setup lang="ts">
import { algorithmTypeTable } from '@/types/game/AlgorithmTypeTable';
import { difficultyTable } from '@/types/game/DifficultyTable';
import { programmingLanguageTable } from '@/types/game/ProgrammingLanguageTable';
import { NButton, NCard, NForm, NFormItem, NModal, NSelect, NSlider, type FormInst, useMessage } from 'naive-ui';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { enterAI } from '@/api/roomService';
import { useMatchStore } from '@/stores/matchStore';
import { HttpStatusCode } from 'axios';

const props = defineProps<{
  show: boolean;
}>();

const emit = defineEmits<{
  'update:show': [value: boolean];
}>();

const isVisible = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value)
});

const router = useRouter();
const message = useMessage();
const matchStore = useMatchStore();

const aiForm = ref<FormInst | null>(null);
const formValue = ref({
  difficulty: '',
  language: '',
  algorithm: '',
  turnTime: 40
});

const formRules = {
  difficulty: { required: true, message: '난이도를 선택해주세요' },
  language: { required: true, message: '프로그래밍 언어를 선택해주세요' },
  algorithm: { required: true, message: '알고리즘 유형을 선택해주세요' },
  turnTime: { required: true, message: '턴 시간을 선택해주세요' }
};

const handleClose = () => {
  isVisible.value = false;
};

const handleStart = async () => {
  await aiForm.value?.validate().then(async () => {
    try {
      const resp = await enterAI(
        formValue.value.difficulty,
        formValue.value.language,
        formValue.value.algorithm,
        formValue.value.turnTime
      );

      if (resp.statusCode === HttpStatusCode.Ok || resp.statusCode === HttpStatusCode.Created) {
        const matchId = resp.data.roomId;
        matchStore.setMatchId(matchId);
        matchStore.setProgrammingLanguage(formValue.value.language);
        handleClose();
        router.push({ name: 'aiMatch', params: { matchId } });
      } else {
        message.warning(resp.message || 'AI 모드 시작에 실패했습니다.');
      }
    } catch (err: any) {
      const serverMessage = err?.response?.data?.message;
      message.warning(serverMessage || 'AI 모드 시작 중 오류가 발생했습니다.');
    }
  }, () => {
    message.warning('필수 항목을 입력해주세요.');
  });
};
</script>
