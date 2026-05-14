<template>
  <DefaultLayout>
    <div class="relative max-w-lg w-full bg-(--ric-bg-card) border border-slate-700 rounded-md px-4 py-8 sm:px-8 sm:py-12">
      <!-- GRADIENT -->
      <div class="absolute top-0 left-1/2 -translate-x-1/2 w-24 h-px bg-linear-to-r from-transparent via-cyan-400/50 to-transparent"></div>

      <n-tabs type="line" size="large" animated>
        <n-tab-pane name="profileEdit" tab="프로필 수정">
          <ProfileEdit />
        </n-tab-pane>

        <n-tab-pane name="changePassword" tab="비밀번호 변경">
          <ChangePassword />
        </n-tab-pane>

        <n-tab-pane name="deleteAccount" tab="회원탈퇴">
          <DeleteAccount />
        </n-tab-pane>
      </n-tabs>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import DefaultLayout from '@/components/layout/DefaultLayout.vue';
import { NTabs, NTabPane } from 'naive-ui';
import ProfileEdit from '@/components/mypage/edit/ProfileEdit.vue';
import ChangePassword from '@/components/mypage/edit/ChangePassword.vue';
import DeleteAccount from '@/components/mypage/edit/DeleteAccount.vue';
</script>