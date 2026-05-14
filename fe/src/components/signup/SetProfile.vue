<template>
  <div class="flex flex-col gap-4">
    <n-form v-bind:model="setProfileFormValue" v-bind:rules="setProfileFormRules" ref="setProfileForm" class="w-full! mt-4!">
      <n-form-item label="닉네임" path="user.nickname">
        <n-input v-model:value="setProfileFormValue.user.nickname" placeholder="닉네임" />
      </n-form-item>

      <n-button v-on:click="handleSignup" type="primary" class="w-full!">
        <template #icon>
          <NIcon>
            <Check />
          </NIcon>
        </template>
        <span>가입 완료</span>
      </n-button>
    </n-form>
  </div>
</template>

<script setup lang="ts">
import router from '@/router';
import { editProfile, signup } from '@/api/userService';
import { HttpStatusCode } from 'axios';
import { type FormInst, useMessage, NForm, NFormItem, NIcon, NInput, NButton } from 'naive-ui';
import { inject, ref } from 'vue';
import { useUserStore } from '@/stores/userStore';
import { Check } from '@vicons/fa';

const signupData = inject<any>('signupData');
const message = useMessage();
const userStore = useUserStore();

const setProfileForm = ref<FormInst | null>(null);
const setProfileFormValue = ref({
  user: {
    nickname: ''
  }
});
const setProfileFormRules = {
  user: {
    nickname: {
      required: true,
      message: '닉네임을 입력해주세요'
    }
  }
};

const handleSignup = async (e: MouseEvent) => {
  e.preventDefault();

  await setProfileForm.value?.validate().then(async () => {
    const email = signupData.email;
    const password = signupData.password;
    const nickname = setProfileFormValue.value.user.nickname;

    if (!nickname?.length) return;

    const hasOauth = !!signupData.oauthAccessToken || !!userStore.accessToken;
    if (hasOauth) {
      try {
        const resp = await editProfile({ nickname });
        if (resp.statusCode === HttpStatusCode.Ok) {
          userStore.user = resp.data;
          message.success('프로필 설정이 완료되었습니다!');
          router.push({ name: 'main' });
          return;
        }
      } catch (err: any) {
        message.warning(err?.response?.data?.message || '프로필 설정 중 오류가 발생했습니다.');
      }
      return;
    }

    if (email?.length && password?.length) {
      try {
        const resp = await signup(email, password, nickname);
        if (resp.statusCode === HttpStatusCode.Ok) {
          message.success('가입해주셔서 감사합니다!');
          router.push({ name: 'main' });
          return;
        }
      } catch (err: any) {
        message.warning(err?.response?.data?.message || '회원가입 중 오류가 발생했습니다.');
      }
    } else {
      message.warning('이메일 또는 비밀번호 정보가 없습니다.');
    }
  }, (err) => {
    message.warning(err);
  });
};
</script>