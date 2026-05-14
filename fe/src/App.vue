<template>
  <n-config-provider v-bind:theme="darkTheme" v-bind:theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <RouterView v-bind:key="$route.fullPath" />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { darkTheme } from 'naive-ui';
import { NConfigProvider, NMessageProvider, NDialogProvider } from 'naive-ui';

const themeOverrides = {
  common: {
    bodyColor:            '#1b2542',
    cardColor:            '#243758',
    popoverColor:         '#1e2f4b',
    borderColor:          '#4b5d7a',
    textColor1:           '#ffffff',
    textColor2:           '#f1f5f9',
    textColor3:           '#e2e8f0',
    primaryColor:         '#38e1ff',
    primaryColorHover:    '#8af0ff',
    primaryColorPressed:  '#12cde6',
    primaryColorSuppl:    '#38e1ff',
  },
  Form: {
    labelTextColor:       '#5fe6ff'
  },
  Dialog: {
    color:                '#1e2f4b',  // 배경색
  }
};
</script>