import { createApp } from 'vue';
import { createPinia } from 'pinia';

import App from './App.vue';
import router from './router';
import piniaPersist from "pinia-plugin-persistedstate";

import '@/assets/css/global.css';

const pinia = createPinia();
pinia.use(piniaPersist);

const app = createApp(App);
app.use(pinia);
app.use(router);

router.isReady().then(() => {
	app.mount('#app');
});