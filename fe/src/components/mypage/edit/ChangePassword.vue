<template>
  <n-form v-bind:model="changePasswordFormValue" v-bind:rules="changePasswordFormRules" ref="changePasswordForm">
    <n-form-item label="현재 비밀번호" path="password.current">
      <n-input v-model:value="changePasswordFormValue.password.current" type="password" placeholder="현재 비밀번호" />
    </n-form-item>

    <n-form-item label="새 비밀번호" path="password.new">
      <n-input v-model:value="changePasswordFormValue.password.new" type="password" placeholder="새 비밀번호" />
    </n-form-item>

    <n-form-item label="새 비밀번호 확인" path="password.newConfirm">
      <n-input v-model:value="changePasswordFormValue.password.newConfirm" type="password" placeholder="새 비밀번호 확인" />
    </n-form-item>

    <div class="flex gap-2 justify-end">
      <n-button @click="handleChangePassword" type="primary">변경하기</n-button>
    </div>
  </n-form>
</template>

<script setup lang="ts">
import { NForm, NFormItem, NInput, NButton, type FormInst, useMessage } from 'naive-ui';
import { ref } from 'vue';
import { useUserStore } from '@/stores/userStore';
import { changePassword } from '@/api/userService';
import { HttpStatusCode } from 'axios';

const userStore = useUserStore();
const message = useMessage();

const changePasswordForm = ref<FormInst | null>(null);
const changePasswordFormValue = ref({
  password: {
    current: '',
    new: '',
    newConfirm: ''
  }
});
const changePasswordFormRules = {
  password: {
    current: {
      required: true,
      message: '현재 비밀번호를 입력해주세요'
    },
    new: {
      required: true,
      message: '새 비밀번호를 입력해주세요'
    },
    newConfirm: {
      required: true,
      message: '새 비밀번호와 똑같이 입력해주세요'
    }
  }
};

const handleChangePassword = async () => {
  await changePasswordForm.value?.validate().then(async () => {
    try {
      const current = changePasswordFormValue.value.password.current;
      const newPw = changePasswordFormValue.value.password.new;
      const newConfirm = changePasswordFormValue.value.password.newConfirm;

      if (newPw !== newConfirm) {
        message.info('새 비밀번호와 새 비밀번호 확인이 서로 다릅니다. 똑같이 입력해주세요.');
        return;
      }

      const resp = await changePassword(current, newPw);
      if (resp.statusCode === HttpStatusCode.Ok) {
        message.success('비밀번호를 성공적으로 변경하였습니다.');
      }
    } catch (err: any) {
      message.warning(err.response.data.message);
    }
  });
};
</script>