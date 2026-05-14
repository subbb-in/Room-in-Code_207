<template>
  <div class="min-h-screen flex flex-col">
    <Header />

    <main class="flex-1 max-w-7xl w-full mx-auto px-10 py-8 flex flex-col justify-center items-center">
      <slot />
    </main>

    <!-- GRADIENT -->
    <div class="fixed inset-0 z-0 pointer-events-none overflow-hidden">
      <div class="absolute top-1/4 -left-1/4 w-[50%] h-[50%] bg-cyan-400/10 blur-[10rem] rounded-full"></div>
      <div class="absolute bottom-1/4 -right-1/4 w-[50%] h-[50%] bg-cyan-400/5 blur-[10rem] rounded-full"></div>
    </div>
    
    <Footer />
  </div>
</template>

<script setup lang="ts">
import Header from '@/components/Header.vue';
import Footer from '@/components/Footer.vue';
</script>