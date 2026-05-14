<template>
  <div class="flex flex-col gap-4">
    <n-form v-bind:model="createAccountFormValue" v-bind:rules="createAccountFormRules" ref="createAccountForm" class="w-full! mt-6!">
      <n-form-item label="이메일" path="account.email">
        <n-input v-bind:input-props="{ type: 'email' }" v-model:value="createAccountFormValue.account.email" placeholder="이메일" />
      </n-form-item>

      <n-form-item label="비밀번호" path="account.password">
        <n-input v-model:value="createAccountFormValue.account.password" type="password" placeholder="비밀번호" />
      </n-form-item>

      <n-form-item label="비밀번호 확인" path="account.passwordConfirm">
        <n-input v-model:value="createAccountFormValue.account.passwordConfirm" type="password" placeholder="비밀번호 확인" />
      </n-form-item>

      <n-button v-on:click="handleNext" type="primary" class="w-full!">
        <template #icon>
          <NIcon>
            <ArrowRight />
          </NIcon>
        </template>
        <span>다음</span>
      </n-button>
    </n-form>
    
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

    <!-- Old? -->
    <div class="flex justify-center items-center gap-4">
      <span class="text-(--ric-text-tertiary)">이미 계정이 있으신가요??</span>
      <n-button type="primary" title="로그인" v-on:click="() => { router.push({ name: 'signin' })}" text size="large">
        로그인
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { socialLoginList } from '@/assets/ts/socialLoginList';
import router from '@/router';
import { ArrowRight } from '@vicons/fa';
import { type FormInst, NForm, NFormItem, NInput, NButton, NIcon, useMessage } from 'naive-ui';
import { inject, ref } from 'vue';

const props = defineProps<{ currentStep: number }>();
const emit = defineEmits<{
  next: [],
  next2: [],
}>();

const signupData = inject<any>('signupData');
const message = useMessage();

const createAccountForm = ref<FormInst | null>(null);
const createAccountFormValue = ref({
  account: {
    email: '',
    password: '',
    passwordConfirm: '',
  }
});
const createAccountFormRules = {
  account: {
    email: {
      required: true,
      message: '이메일을 입력해주세요'
    },
    password: {
      required: true,
      message: '비밀번호를 입력해주세요'
    },
    passwordConfirm: {
      required: true,
      message: '상단에서 입력한 비밀번호와 같게 입력해주세요'
    }
  }
};

const handleNext = async (e: MouseEvent) => {
  e.preventDefault();

  await createAccountForm.value?.validate().then(async () => {
    const password = createAccountFormValue.value.account.password;
    const passwordConfirm = createAccountFormValue.value.account.passwordConfirm;

    if (password !== passwordConfirm) {
      message.warning('비밀번호가 서로 다릅니다. 다시 입력해주세요.');
      return;
    }

    // 임시 데이터에 저장
    signupData.email = createAccountFormValue.value.account.email;
    signupData.password = createAccountFormValue.value.account.password;

    emit('next');    
  }, (err) => {
    message.warning(`다음 단계로 넘어갈 수 없습니다`);
  });
};

const handleSocialLogin = async (socialType: string) => {
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
</script>