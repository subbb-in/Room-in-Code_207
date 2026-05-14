<template>
  <div class="relative">
    <n-button v-on:click="handleTogglePanel" class="flex! items-center! text-base!" type="primary" text>
      <template #icon>
        <EmojiEmotionsOutlined />
      </template>
      <span>이모지</span>
    </n-button>

    <!-- EMOJI PANEL -->
    <ul class="absolute bg-(--ric-bg-card) rounded-4xl px-4 py-2 border border-slate-700 flex items-center gap-2 transition-opacity duration-300" v-bind:class="isShow ? 'opacity-100' : 'opacity-0 pointer-events-none'">
      <li v-for="emoji in emojiList" v-bind:key="emoji.name">
        <n-button text class="text-2xl! transition-transform! duration-300! hover:scale-110!">
          {{ emoji.emoji }}
        </n-button>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { EmojiEmotionsOutlined } from '@vicons/material';
import { NButton } from 'naive-ui';
import { emojiList } from '@/assets/ts/emojiList';
import { ref } from 'vue';

const isShow = ref(false);

const handleTogglePanel = () => {
  isShow.value = !isShow.value;
};
</script>