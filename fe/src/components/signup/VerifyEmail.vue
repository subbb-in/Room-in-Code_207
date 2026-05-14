<template>
  <div class="flex flex-col gap-4">
    <p class="text-(--ric-color-primary) text-base">전송 버튼을 누르면 이메일로 6자리의 인증 코드가 발송됩니다.</p>

    <n-form v-bind:model="verifyEmailFormValue" v-bind:rules="verifyEmailFormRules" v-bind:show-feedback="false" ref="verifyEmailForm" class="flex items-end gap-4">
      <n-form-item label="인증 코드" path="verify.otpCode" class="shrink-0!">
        <n-input-otp v-model:value="verifyEmailFormValue.verify.otpCode" />
      </n-form-item>

      <n-form-item class="flex-1! w-full!">
        <n-button v-on:click="handleSendOtp" secondary type="primary" class="w-full!">
          <template #icon>
            <NIcon>
              <Send />
            </NIcon>
          </template>
          <span>{{ isOtpSent ? `재전송` : '전송' }}</span>
        </n-button>
      </n-form-item>
    </n-form>

    <div v-if="isOtpSent">
      <p class="text-center text-amber-400">남은 시간: {{ Math.floor(timeLeft / 60) }}분 {{ timeLeft % 60 }}초</p>
    </div>

    <div class="flex items-center gap-4">
      <n-button v-on:click="emit('prev')" icon-placement="left" secondary class="flex-1!">
        <template #icon>
          <NIcon>
            <ArrowLeft />
          </NIcon>
        </template>
        <span>뒤로</span>
      </n-button>
      <n-button v-on:click="handleAuthNext" icon-placement="left" type="primary" class="flex-1!" v-bind:disabled="!isOtpSent">
        <template #icon>
          <NIcon>
            <CircleCheck />
          </NIcon>
        </template>
        <span>인증</span>
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { checkAuthCode, requestAuthCode } from '@/api/userService';
import { ArrowLeft } from '@vicons/fa';
import { CircleCheck, Send } from '@vicons/tabler';
import { HttpStatusCode } from 'axios';
import { type FormInst, NForm, NFormItem, NInputOtp, NButton, NIcon, useMessage } from 'naive-ui';
import { inject, ref, onBeforeUnmount } from 'vue';

const signupData = inject<any>('signupData');
const emit = defineEmits<{
  prev: [],
  next: [],
}>();
const message = useMessage();

const isOtpSent = ref(false);
const isTimerActive = ref(false);
const timeLeft = ref(60 * 5);
let timerInterval: any = null;
const verifyEmailForm = ref<FormInst | null>(null);
const verifyEmailFormValue = ref({
  verify: {
    otpCode: ''.split('')
  }
});
const verifyEmailFormRules = {
  verify: {
    otpCode: {
      required: true,
      message: '인증 코드를 입력해주세요'
    }
  }
};

const handleSendOtp = async (e: MouseEvent) => {
  e.preventDefault();

  if (signupData.email?.length) {
    try {
      const resp = await requestAuthCode(signupData.email);
      if (resp.statusCode === HttpStatusCode.Ok) {
        message.info('인증 코드를 전송하였습니다. 메일함에서 확인해주세요.');
        isOtpSent.value = true;
        
        // 5분 타이머 시작
        isTimerActive.value = true;
        timeLeft.value = 60 * 5;
        
        if (timerInterval) {
          clearInterval(timerInterval);
        }

        timerInterval = setInterval(() => {
          timeLeft.value--;
          if (timeLeft.value <= 0) {
            clearInterval(timerInterval!);
            timerInterval = null;
            isTimerActive.value = false;
            isOtpSent.value = false;
            message.info('인증 시간이 만료되었습니다. 다시 전송해주세요.');
          }
        }, 1000);
        return;
      }
    } catch (err: any) {
      message.warning(`${err.response.data.message}`);
    }
  } else { 
    message.warning('이메일 주소를 입력해주세요');
  }
};

const handleAuthNext = async (e: MouseEvent) => {
  e.preventDefault();

  await verifyEmailForm.value?.validate().then(async () => {
    if (timerInterval) clearInterval(timerInterval);

    try {
      const code = verifyEmailFormValue.value.verify.otpCode.join('');
      const resp = await checkAuthCode(signupData.email, code);
      if (resp.statusCode === HttpStatusCode.Ok) {
        emit('next');
      }
    } catch (err: any) {
      message.warning(`${err.response.data.message}`);
    }
  }, (err) => {
    message.warning(`다음 단계로 넘어갈 수 없습니다`);
  });
};

// 컴포넌트 언마운트 시 타이머 정리
onBeforeUnmount(() => {
  if (timerInterval) clearInterval(timerInterval);
});
</script>