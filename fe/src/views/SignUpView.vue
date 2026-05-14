<template>
  <DefaultLayout>
    <div class="relative max-w-md w-full bg-(--ric-bg-card) border border-slate-700 rounded-md px-4 py-8 sm:px-8 sm:py-12">
      <!-- GRADIENT -->
      <div class="absolute top-0 left-1/2 -translate-x-1/2 w-24 h-px bg-linear-to-r from-transparent via-cyan-400/50 to-transparent"></div>

      <div class="flex flex-col itmes-center gap-4">
        <div class="flex flex-col items-center gap-8 w-full">
          <!-- TITLE -->
          <h2 class="font-bold text-3xl">회원가입</h2>

          <n-steps vertical v-bind:current="currentStep" v-bind:status="currentStepStatus">
            <n-step title="계정 생성">
              <CreateAccount v-if="currentStep === 1" v-bind:currentStep="currentStep" v-on:next="currentStep=2" v-on:next2="currentStep=3" />
            </n-step>

            <n-step title="이메일 인증">
              <VerifyEmail v-if="currentStep === 2" v-bind:currentStep="currentStep" v-on:next="currentStep=3" v-on:prev="currentStep=1" />
            </n-step>

            <n-step title="프로필 설정">
              <SetProfile v-if="currentStep === 3" />
            </n-step>
          </n-steps>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import DefaultLayout from '@/components/layout/DefaultLayout.vue';
import CreateAccount from '@/components/signup/CreateAccount.vue';
import SetProfile from '@/components/signup/SetProfile.vue';
import VerifyEmail from '@/components/signup/VerifyEmail.vue';
import { NSteps, NStep, type StepsProps } from 'naive-ui';
import { onMounted, provide, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useUserStore } from '@/stores/userStore';
import { getMyInfo } from '@/api/userService';

const currentStep = ref(1);
const currentStepStatus = ref<StepsProps['status']>('process');
const route = useRoute();
const userStore = useUserStore();

const signupData = ref({
  email: '',
  password: '',
  profileImage: '',
  oauthAccessToken: ''
});
provide('signupData', signupData);

onMounted(async () => {
  const oauthFlag = route.query.oauth === '1' || sessionStorage.getItem('oauthSignup') === '1';
  if (!oauthFlag) return;

  sessionStorage.removeItem('oauthSignup');
  currentStep.value = 3;

  const storedEmail = sessionStorage.getItem('oauthEmail');
  if (storedEmail) {
    signupData.value.email = storedEmail;
  }

  if (userStore.accessToken?.length) {
    signupData.value.oauthAccessToken = userStore.accessToken;
    if (!signupData.value.email) {
      try {
        const resp = await getMyInfo();
        signupData.value.email = resp.data?.email || '';
      } catch {
        // ignore profile fetch errors for now
      }
    }
  }
});
</script>