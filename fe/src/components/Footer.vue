<template>
  <footer class="p-4">
    <p class="text-sm text-center text-(--ric-text-tertiary)">&copy; {{ currentYear }}. RoomInCode. All rights reserved</p>
  </footer>
</template>

<script setup lang="ts">
const currentYear = new Date().getFullYear();
</script>