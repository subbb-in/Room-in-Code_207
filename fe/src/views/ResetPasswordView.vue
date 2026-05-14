<template>
  <DefaultLayout>
    <div class="relative max-w-md w-full bg-(--ric-bg-card) border border-slate-700 rounded-md px-4 py-8 sm:px-8 sm:py-12">
      <!-- GRADIENT -->
      <div class="absolute top-0 left-1/2 -translate-x-1/2 w-24 h-px bg-linear-to-r from-transparent via-cyan-400/50 to-transparent"></div>

      <div class="flex flex-col items-center gap-4">
        <div class="flex flex-col items-center gap-8 w-full">
          <!-- TITLE -->
          <h2 class="font-bold text-3xl">비밀번호 재설정</h2>

          <!-- TIMER -->
          <p class="text-(--ric-color-primary)">만료 시간: {{ formatTime(remainingTime) }}</p>

          <!-- FORM -->
          <div class="flex flex-col w-full">
            <n-form v-bind:model="resetPWFormValue" v-bind:rules="resetPWFormRules" ref="resetPWForm" class="w-full!">
              <n-form-item label="새 비밀번호" path="password.new" >
                <n-input type="password" v-model:value="resetPWFormValue.password.new" placeholder="새 비밀번호" />
              </n-form-item>

              <n-form-item label="새 비밀번호 확인" path="password.newConfirm">
                <n-input v-model:value="resetPWFormValue.password.newConfirm" type="password" placeholder="새 비밀번호 확인" show-password-on="click" />
              </n-form-item>

              <n-button v-on:click="handleResetPW" type="primary" class="w-full!">
                <template #icon>
                  <NIcon>
                    <LockResetRound />
                  </NIcon>
                </template>
                <span>변경</span>
              </n-button>
            </n-form>
          </div>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { checkResetPasswordToken, resetPassword } from '@/api/userService';
import DefaultLayout from '@/components/layout/DefaultLayout.vue';
import { LockResetRound } from '@vicons/material';
import { HttpStatusCode } from 'axios';
import { NForm, NFormItem, NInput, NButton, NIcon, type FormInst, useMessage } from 'naive-ui';
import { onMounted, ref, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const message = useMessage();

// 타이머 관련
const remainingTime = ref(300); // 5분 = 300초
let timerInterval: number | null = null;

// 시간을 MM:SS 형식으로 변환
const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
};

// 타이머 시작
const startTimer = () => {
  timerInterval = window.setInterval(() => {
    remainingTime.value--;
    if (remainingTime.value <= 0) {
      clearInterval(timerInterval!);
      message.warning('제한 시간이 만료되었습니다. 로그인 화면으로 돌아갑니다.');
      router.push({ name: 'signin' });
    }
  }, 1000);
};

// 타이머 종료
const stopTimer = () => {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
};

onMounted(async () => {
  // 타이머 시작
  startTimer();

  const token = route.query.token?.toString();
  if (!token) {
    message.warning('토큰 값이 유효하지 않습니다. 로그인 화면으로 돌아갑니다.');
    stopTimer();
    router.push({ name: 'signin' });
    return;
  }
  
  try {
    const resp = await checkResetPasswordToken(token);
    if (resp.statusCode === HttpStatusCode.NotFound) {
      message.info(resp.message);
      stopTimer();
      router.push({ name: 'signin' });
      return;
    }
  } catch (err: any) {
    message.warning(err.response.data.message);
    stopTimer();
    router.push({ name: 'signin' });
    return;
  }
});

// 페이지 떠날 때 타이머 정리
onBeforeUnmount(() => {
  stopTimer();
});

const resetPWForm = ref<FormInst | null>(null);
const resetPWFormValue = ref({
  password: {
    new: '',
    newConfirm: '',
  }
});
const resetPWFormRules = {
  password: {
    new: {
      required: true,
      message: '새 비밀번호를 입력해주세요'
    },
    newConfirm: {
      required: true,
      message: '새 비밀번호와 새 비밀번호 확인을 똑같이 입력해주세요'
    }
  }
};

const handleResetPW = async (e: MouseEvent) => {
  e.preventDefault();

  await resetPWForm.value?.validate().then(async () => {
    try {
      const token = route.query.token?.toString()!;
      const newPassword = resetPWFormValue.value.password.new;
      const newPasswordConfirm = resetPWFormValue.value.password.newConfirm;
      if (newPassword !== newPasswordConfirm) {
        message.info('새 비밀번호와 새 비밀번호 확인의 내용이 서로 다릅니다. 다시 입력해주세요.');
        return;
      }

      const resp = await resetPassword(token, newPassword);
      if (resp.statusCode === HttpStatusCode.Ok) {
        message.success('성공적으로 비밀번호를 재설정하였습니다. 로그인 화면으로 돌아갑니다.');
        stopTimer();
        router.push({ name: 'signin' });
        return;
      }
    } catch (err: any) {
      message.warning(err.response.data.message);
    }
  });
};
</script>