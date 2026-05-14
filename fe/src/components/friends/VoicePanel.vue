<template>
  <div>
    <n-button class="flex! items-center! text-base!" type="primary" text>
      <template #icon>
        <KeyboardVoiceRound />
      </template>
      <span>음성 채팅</span>
    </n-button>
  </div>
</template>

<script setup lang="ts">
import { KeyboardVoiceRound } from '@vicons/material';
import { NButton } from 'naive-ui';
</script>