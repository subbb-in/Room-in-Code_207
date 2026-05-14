import { createRouter, createWebHistory } from 'vue-router';
import { routes } from './routes';
import { routerGuards } from './guards';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: routes,
});

routerGuards(router);

export default router;