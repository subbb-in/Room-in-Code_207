<template>
  <DefaultLayout>
    <div class="flex flex-col items-center gap-3 text-sm text-(--ric-text-secondary)">
      <span>OAuth 처리 중입니다...</span>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import DefaultLayout from '@/components/layout/DefaultLayout.vue';
import { onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/userStore';
import { getMyInfo } from '@/api/userService';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const getAccessToken = () => {
  const raw = route.query.accessToken ?? route.query.accesstoken ?? route.query.token;
  return typeof raw === 'string' ? raw : '';
};

onMounted(async () => {
  const accessToken = getAccessToken();
  if (!accessToken) {
    router.replace({ name: 'signin' });
    return;
  }

  userStore.accessToken = accessToken;

  try {
    const resp = await getMyInfo();
    userStore.user = resp.data;
    console.log(resp);

    if (resp?.data?.email) {
      sessionStorage.setItem('oauthEmail', resp.data.email);
    }
  } catch {
    // ignore profile fetch error; continue to signup
  }

  router.replace({ name: 'main' });
});
</script>
