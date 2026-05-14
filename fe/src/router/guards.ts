import { useUserStore } from "@/stores/userStore";
import type { Router } from "vue-router";

export const routerGuards = (router: Router) => {
  router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore();
    const oauthSignupAllowed = sessionStorage.getItem('oauthSignup') === '1' || to.query.oauth === '1';

    // 로그인 상태면 로그인 및 회원가입 페이지 접근 금지
    if (userStore.accessToken?.length > 0) {
      if (to.name === 'signin' || to.name === 'signup') {
        if (!oauthSignupAllowed) {
          next({ name: 'main' });
          return;
        }
      }
    }

    // requiredAuth
    if (to.meta.requiredAuth) {
      if (userStore.accessToken?.length === 0) {
        next({ name: 'signin' });
        return;
      }
    }

    next();
  });
};