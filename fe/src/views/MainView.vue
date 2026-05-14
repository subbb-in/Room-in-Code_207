<template>
  <DefaultLayout>
    <ul class="grid grid-cols-1 gap-8 w-full max-w-5xl sm:grid-cols-3">
      <li v-for="mode in modeList" v-bind:key="mode.name" class="relative group flex flex-col items-center gap-4 w-full text-center bg-(--ric-bg-card) border border-slate-700 rounded-2xl p-8 transition duration-300 hover:border-(--ric-color-primary)/40 hover:-translate-y-1">
        <!-- GRADIENT -->
        <div class="absolute top-0 left-1/2 -translate-x-1/2 w-24 h-px bg-linear-to-r from-transparent via-cyan-400/50 to-transparent"></div>

        <!-- ICON -->
        <div class="size-24 flex justify-center items-center bg-(--ric-bg-popover) border border-slate-700/50 rounded-full transition-colors duration-300 group-hover:border-(--ric-color-primary)/20">
          <component v-bind:is="mode.component" class="size-14 text-(--ric-color-primary)" />
        </div>

        <!-- INFO -->
        <div class="flex flex-col gap-4">
          <h4 class="font-bold text-3xl">{{ mode.title }}</h4>
          <p class="invisible text-lg text-(--ric-text-secondary) opacity-0 transition-opacity duration-300 group-hover:visible group-hover:opacity-100">{{ mode.desc }}</p>
        </div>

        <!-- BUTTON -->
        <n-button v-on:click="handleGameStart(mode.name)" v-bind:title="mode.btnName" type="primary" size="large" class="w-full!">
          {{ mode.btnName }}
        </n-button>
      </li>
    </ul>
  </DefaultLayout>

  <AICreateRoomModal v-model:show="showAiModal" />
</template>

<script setup lang="ts">
import { modeList } from '@/assets/ts/modeList';
import { NButton } from 'naive-ui';
import DefaultLayout from '@/components/layout/DefaultLayout.vue';
import router from '@/router';
import AICreateRoomModal from '@/components/ai/AICreateRoomModal.vue';
import { ref } from 'vue';
import { useUserStore } from '@/stores/userStore';

const showAiModal = ref(false);
const userStore = useUserStore();

const handleGameStart = (mode: string) => {
  switch (mode) {
    case 'AI':
      if (!userStore.accessToken?.length) {
        router.push({ name: 'signin' });
        return;
      }
      showAiModal.value = true;
      break;
    case 'FRIENDS': router.push({ name: 'lobby' }); break;
    case 'CONTEST': router.push({ name: 'leaderboard' }); break;
  }
};
</script>