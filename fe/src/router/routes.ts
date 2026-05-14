import type { RouteRecordRaw } from "vue-router";

export const routes: Array<RouteRecordRaw> = [
  // DASHBOARD
  {
    path: '/',
    name: 'main',
    component: () => import('@/views/MainView.vue')
  },

  // AI Mode
  {
    path: '/ai',
    name: 'ai',
    component: () => import('@/views/AIModeView.vue'),
    meta: {
      requiredAuth: true
    }
  },
  {
    path: '/ai/:matchId',
    name: 'aiMatch',
    component: () => import('@/views/AIMatchView.vue'),
    meta: {
      requiredAuth: true
    }
  },
  {
    path: '/ai/report/:matchId',
    name: 'aiReport',
    component: () => import('@/views/AIReportView.vue'),
    meta: {
      requiredAuth: true
    }
  },

  // SignIn
  {
    path: '/signin',
    name: 'signin',
    component: () => import('@/views/SignInView.vue')
  },

  // OAuth Callback
  {
    path: '/oauth/callback',
    name: 'oauthCallback',
    component: () => import('@/views/OAuthCallbackView.vue')
  },

  // SignUp
  {
    path: '/signup',
    name: 'signup',
    component: () => import('@/views/SignUpView.vue')
  },

  // MyPage
  {
    path: '/mypage',
    name: 'mypage',
    component: () => import('@/views/MyPageView.vue'),
    meta: {
      requiredAuth: true
    }
  },

  // MyPageEdit
  {
    path: '/mypage/edit',
    name: 'mypageEdit',
    component: () => import('@/views/MyPageEditView.vue'),
    meta: {
      requiredAuth: true
    }
  },

  // Reset Password
  {
    path: '/reset/password',
    name: 'resetPassword',
    component: () => import('@/views/ResetPasswordView.vue')
  },

  // Friends Lobby
  {
    path: '/friends/lobby',
    name: 'lobby',
    component: () => import('@/views/FriendsLobbyView.vue'),
    meta: {
      requiredAuth: true
    }
  },

  // Friends
  {
    path: '/friends/:matchId',
    name: 'friends',
    component: () => import('@/views/FriendsView.vue'),
    meta: {
      requiredAuth: true
    }
  },

  // Contest Leaderboard
  {
    path: '/contest/leaderboard/',
    name: 'leaderboard',
    component: () => import('@/views/ContestLeaderboardView.vue'),
    meta: {
      requiredAuth: true
    }
  },
  // Contest Vote
  {
    path: '/contest/vote/:matchId',
    name: 'vote',
    component: () => import('@/views/ContestVoteView.vue'),
    meta: {
      requiredAuth: true
    }
  },
  // Contest
  {
    path: '/contest/:matchId',
    name: 'contest',
    component: () => import('@/views/ContestView.vue'),
    meta: {
      requiredAuth: true
    }
  },

];