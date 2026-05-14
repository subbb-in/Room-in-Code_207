import { fileURLToPath, URL } from 'node:url';

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueDevTools from 'vite-plugin-vue-devtools';
import tailwindcss from '@tailwindcss/vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    tailwindcss(),
  ],
  define: {
    global: 'globalThis',
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  // server: {
  //   proxy: {
  //     '/api': {
  //       target: 'http://localhost:8081',
  //       changeOrigin: true,
  //     }
  //   }
  // },
  build: {
    sourcemap: true,
  }
  // build: {
  //   rollupOptions: {
  //     output: {
  //       manualChunks(id) {
  //         if (id.includes('node_modules')) {
  //           if (id.includes('vue'))         return 'vue';
  //           if (id.includes('pinia'))       return 'pinia';
  //           if (id.includes('vue-router'))  return 'vue-router';
            
  //           return 'vendor';
  //         }
  //       }
  //     }
  //   }
  // }
});