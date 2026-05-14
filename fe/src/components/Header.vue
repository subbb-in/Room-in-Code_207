<template>
  <header class="sticky top-0 left-0 z-10 w-full h-20 flex items-center border-b border-b-slate-700 backdrop-blur-sm px-8 py-4">
    <nav class="flex justify-between items-center gap-4 w-full">
      <!-- LOGO -->
      <RouterLink v-bind:to="{ name: 'main' }" title="RoomInCode" class="flex items-center gap-2">
        <img src="/logo.png" alt="LOGO" class="size-8">
        <h1 class="font-bold text-2xl">RoomInCode</h1>
      </RouterLink>

      <!-- Not LoggedIn -->
      <div v-if="!userStore.accessToken?.length">
        <n-button type="primary" title="로그인" v-on:click="() => { router.push({ name: 'signin' })}" text>
          로그인
        </n-button>
      </div>

      <!-- LoggedIn -->
      <div v-else class="flex items-center gap-2">
        <!-- PROFILE IMAGE -->
        <img v-bind:src="getProfileImageUrl(userStore.user?.profileImageUrl)" alt="PROFIME" loading="lazy" class="size-10 rounded-full">

        <!-- INFO -->
        <div class="flex flex-col gap-1">
          <h2 class="font-bold text-sm">{{ userStore.user?.nickname || 'CodeMaster_99' }}</h2>
          <span class="text-xs text-(--ric-color-primary)">#{{ myRankValue }}</span>
        </div>

        <!-- DROPDOWN -->
        <n-dropdown size="large" v-bind:options="profileDropdownOpts" v-on:select="handleProfileDropdownSelect" trigger="hover">
          <n-button v-bind:bordered="false" class="p-0!">
            <n-icon size="16"><AngleDown /></n-icon>
          </n-button>
        </n-dropdown>
      </div>
    </nav>
  </header>
</template>

<script setup lang="ts">
import router from '@/router';
import { useUserStore } from '@/stores/userStore';
import { NButton, NDropdown, NIcon } from 'naive-ui';
import { AngleDown, SignOutAlt, User } from '@vicons/fa';
import { h, onMounted, ref } from 'vue';
import { logout } from '@/api/authService';
import { HttpStatusCode } from 'axios';
import { getProfileImageUrl } from '@/utils/imageUtil';
import { getUserRanking } from '@/api/contestService';

const userStore = useUserStore();
const myRankValue = ref<string | number>('');

const profileDropdownOpts = [
  { label: '마이페이지', key: 'mypage', icon: () => h(NIcon, null, { default: () => h(User) }) },
  { label: '로그아웃', key: 'logout', icon: () => h(NIcon, null, { default: () => h(SignOutAlt) }) },
];

const handleProfileDropdownSelect = async (key: string | number) => {
  switch (key) {
    case 'mypage': router.push({ name: 'mypage' }); break;
    case 'logout': {
      try {
        const resp = await logout();
        if (resp.statusCode === HttpStatusCode.Ok) {
          
        }
      } finally {
        userStore.user = null;
        userStore.accessToken = '';
        router.push({ name: 'main' });
      }
    } break;
  }
};

// 페이지 로드 시 랭킹 정보 조회
onMounted(async () => {
  if (userStore.user?.userId) {
    try {
      const resp = await getUserRanking(userStore.user.userId);
      if (resp.statusCode === HttpStatusCode.Ok && resp.data?.rank) {
        myRankValue.value = resp.data.rank;
      }
    } catch (err) {
      myRankValue.value = '알 수 없음';
    }
  }
});
</script>