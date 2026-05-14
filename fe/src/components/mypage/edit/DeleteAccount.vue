<template>
  <div class="flex flex-col gap-4">
    <p class="text-red-400 p-4 border border-slate-700 bg-(--ric-bg-popover) rounded-sm">회원탈퇴 시 관련된 데이터가 영구적으로 삭제되며, 복구할 수 없습니다. 계속 진행하려면 현재 비밀번호를 입력해주세요.</p>

    <n-form v-bind:model="deleteAccountFormValue" v-bind:rules="deleteAccountFormRules" ref="deleteAccountForm">
      <n-form-item label="현재 비밀번호" path="account.password">
        <n-input v-model:value="deleteAccountFormValue.account.password" type="password" placeholder="현재 비밀번호" />
      </n-form-item>

      <n-button type="primary" v-on:click="handleDeleteAccount">
        <template #icon>
          <NIcon>
            <DeleteRound />
          </NIcon>
        </template>
        <span>회원탈퇴</span>
      </n-button>
    </n-form>
  </div>
</template>

<script setup lang="ts">
import { deleteAccount } from '@/api/userService';
import router from '@/router';
import { useUserStore } from '@/stores/userStore';
import { DeleteRound } from '@vicons/material';
import { HttpStatusCode } from 'axios';
import { type FormInst, NForm, NFormItem, NInput, NButton, NIcon, useDialog, useMessage } from 'naive-ui';
import { ref } from 'vue';

const message = useMessage();
const dialog = useDialog();
const userStore = useUserStore();

const deleteAccountForm = ref<FormInst | null>(null);
const deleteAccountFormValue = ref({
  account: {
    password: ''
  }
});
const deleteAccountFormRules = {
  account: {
    password: {
      required: true,
      message: '현재 비밀번호를 입력해주세요'
    }
  }
};

const handleDeleteAccount = async () => {
  await deleteAccountForm.value?.validate().then(async () => {
    try {
      dialog.error({
        title: '정말로 탈퇴하시겠습니까?',
        content: '회원탈퇴 시 정보를 복구할 수 없습니다. 정말로 회원탈퇴를 진행할까요?',
        negativeText: '아니오', 
        positiveText: '예',
        onPositiveClick: async () => {
          try {
            const resp = await deleteAccount(userStore.user?.userId as number, deleteAccountFormValue.value.account.password);
            if (resp.statusCode === HttpStatusCode.Ok) {
              message.success('성공적으로 회원탈퇴 되었습니다. 그동안 이용해주셔서 감사합니다.');
              userStore.user = null;
              userStore.accessToken = '';
              router.push({ name: 'main' });
            }
          } catch (err: any) {
            message.warning(err.response.data.message);
          }
        },
      });
    } catch (err: any) {
      message.warning(err);
    }
  });
};
</script>