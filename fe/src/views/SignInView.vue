<template>
  <DefaultLayout>
    <div class="relative max-w-md w-full bg-(--ric-bg-card) border border-slate-700 rounded-md px-4 py-8 sm:px-8 sm:py-12">
      <!-- GRADIENT -->
      <div class="absolute top-0 left-1/2 -translate-x-1/2 w-24 h-px bg-linear-to-r from-transparent via-cyan-400/50 to-transparent"></div>

      <div class="flex flex-col items-center gap-4">
        <div class="flex flex-col items-center gap-8 w-full">
          <!-- TITLE -->
          <h2 class="font-bold text-3xl">로그인</h2>

          <div class="flex flex-col gap-4 w-full">
            <n-form v-bind:model="signinFormValue" v-bind:rules="signinFormRules" ref="signinForm" class="w-full!">
              <n-form-item label="이메일" path="user.email" >
                <n-input v-bind:input-props="{ type: 'email' }" v-model:value="signinFormValue.user.email" placeholder="이메일" />
              </n-form-item>

              <n-form-item label="비밀번호" path="user.password">
                <n-input v-model:value="signinFormValue.user.password" type="password" placeholder="비밀번호" show-password-on="click" />
              </n-form-item>

              <n-button v-on:click="handleLogin" v-bind:render-icon="() => { return h(NIcon, null, { default: () => h(Login) }) }" type="primary" class="w-full!">로그인</n-button>
            </n-form>

            <div class="flex justify-center items-center gap-4">
              <span class="text-(--ric-text-tertiary)">비밀번호를 잊으셨나요?</span>
              <n-button type="primary" title="회원가입" v-on:click="handleResetPW" text size="large">
                비밀번호 재설정
              </n-button>
            </div>
          </div>
        </div>

        <div class="flex items-center gap-2 w-full">
          <span class="h-px flex-1 bg-slate-700"></span>
          <span class="text-(--ric-text-tertiary) text-sm">또는</span>
          <span class="h-px flex-1 bg-slate-700"></span>
        </div>

        <!-- SOCIAL -->
        <ul class="grid grid-cols-2 gap-4 w-full">
          <li v-for="social in socialLoginList" v-bind:key="social.name">
            <n-button v-on:click="handleSocialLogin(social.name)" secondary class="w-full!">
              <template #icon>
                <NIcon>
                  <component v-bind:is="social.component" />
                </NIcon>
              </template>
              {{ social.title }}
            </n-button>
          </li>
        </ul>

        <!-- Newbie? -->
        <div class="flex justify-center items-center gap-4">
          <span class="text-(--ric-text-tertiary)">새로 오셨나요?</span>
          <n-button type="primary" title="회원가입" v-on:click="() => { router.push({ name: 'signup' })}" text size="large">
            회원가입
          </n-button>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import DefaultLayout from '@/components/layout/DefaultLayout.vue';
import router from '@/router';
import { type FormInst, NForm, NFormItem, NInput, NButton, NIcon, useMessage, useDialog } from 'naive-ui';
import { h, ref } from 'vue';
import { Login } from '@vicons/tabler';
import { login } from '@/api/authService';
import { HttpStatusCode } from 'axios';
import { useUserStore } from '@/stores/userStore';
import { socialLoginList } from '@/assets/ts/socialLoginList';
import { getMyInfo, requestResetPasswordLink } from '@/api/userService';

const userStore = useUserStore();
const message = useMessage();
const dialog = useDialog();

const signinForm = ref<FormInst | null>(null);
const signinFormValue = ref({
  user: {
    email: '',
    password: '',
  }
});
const signinFormRules = {
  user: {
    email: {
      required: true,
      message: '이메일을 입력해주세요'
    },
    password: {
      required: true,
      message: '비밀번호를 입력해주세요'
    }
  }
};
const handleLogin = async (e: MouseEvent) => {
  e.preventDefault();
  
  await signinForm.value?.validate().then(async () => {
    // Login
    try {
      const email = signinFormValue.value.user.email;
      const password = signinFormValue.value.user.password;

      const resp = await login(email, password);
      if (resp.statusCode === HttpStatusCode.Ok) {
        userStore.accessToken = resp.data;
        userStore.user = (await getMyInfo()).data;
        router.push({ name: 'main' });
        return;
      }
      message.warning(resp.message || '로그인에 실패했습니다. 다시 시도해주세요.');
    } catch (err: any) {
      const serverMessage = err?.response?.data?.message;
      message.warning(serverMessage || '로그인 중 오류가 발생했습니다. 다시 시도해주세요.');
    }
  }, (err) => {
    message.warning('로그인에 실패하였습니다. 다시 시도해주세요.');
  });
};
const handleSocialLogin = (socialType: string) => {
  const runtimeOrigin = typeof window !== 'undefined' ? `${window.location.origin}/` : '/';
  const apiBase = import.meta.env.VITE_API_BASE ?? runtimeOrigin;
  const normalizedBase = apiBase.endsWith('/') ? apiBase : `${apiBase}/`;

  switch (socialType) {
    case 'GITHUB': {
      window.location.href = `${normalizedBase}oauth2/authorization/github`;
    } break;
    case 'GOOGLE': {
      window.location.href = `${normalizedBase}oauth2/authorization/google`;
    } break;
    default: {
      message.warning('지원하지 않는 소셜 로그인입니다.');
    }
  }
};

const handleResetPW = (e: MouseEvent) => {
  e.preventDefault();

  const email = signinFormValue.value.user.email;
  if (!email) {
    message.info('비밀번호 재설정을 수행하려면 이메일이 필요합니다. 이메일을 입력해주세요.');
    return;
  }

  // 비밀번호 재설정 링크 발송
  dialog.info({
    title: '비밀번호 재설정 링크 전송',
    content: '비밀번호 재설정을 위한 링크를 입력하신 이메일로 발송합니다.',
    positiveText: '예',
    negativeText: '아니오',
    onPositiveClick: async () => {
      try {
        const resp = await requestResetPasswordLink(email);
        if (resp.statusCode === HttpStatusCode.Ok) {
          message.info('비밀번호 재설정 링크를 전송하였습니다. 메일함을 확인해주세요');
          return;
        }
      } catch (err: any) {
        const serverMessage = err?.response?.data?.message;
        message.warning(serverMessage || '비밀번호 재설정 요청 중 오류가 발생했습니다.');
      }
    }
  });
};
</script>