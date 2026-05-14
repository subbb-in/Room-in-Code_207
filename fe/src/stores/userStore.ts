import { type UserProfileResponse } from '@/types/api/UserProfileResponse';
import { defineStore } from 'pinia'
import { ref } from 'vue';

export const useUserStore = defineStore('user', () => {
  const accessToken = ref('');
  const user = ref<UserProfileResponse | null>();

  return {
    accessToken,
    user
  }
}, {
  persist: {
    storage: sessionStorage,
    pick: [ 'accessToken', 'user' ]
  }
});