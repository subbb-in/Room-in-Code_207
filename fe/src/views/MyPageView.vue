<template>
  <DefaultLayout>
    <div class="max-w-5xl w-full flex flex-col gap-4">
      <!-- PROFILE -->
      <div class="flex flex-col items-center gap-8 bg-(--ric-bg-card) border border-slate-700 rounded-md p-8 sm:flex-row sm:items-start">
        <!-- IMAGE -->
        <div class="rounded-full border-4 border-(--ric-color-primary) size-48 p-1 shrink-0">
          <img v-bind:src="getProfileImageUrl(userStore.user?.profileImageUrl)" alt="PROFIME" loading="lazy" class="size-full rounded-full">
        </div>

        <!-- Container -->
        <div class="flex flex-col gap-4 w-full">
          <!-- NICKNAME & EMAIL -->
          <div class="flex flex-col justify-between items-center gap-4 sm:flex-row">
            <!-- NICKNAME -->
            <div class="flex flex-col items-center gap-1 sm:items-start">
              <h2 class="font-bold text-2xl">{{ userStore.user?.nickname }}</h2>
              <a v-bind:href="`mailto:${userStore.user?.email}`" class="text-sm text-(--ric-color-primary)">{{userStore.user?.email}}</a>
            </div>

            <!-- BUTTON -->
            <div class="flex items-center gap-4">
              <n-button type="primary" title="수정" v-on:click="() => { router.push({ name: 'mypageEdit' }) }">
                <template #icon>
                  <NIcon>
                    <Edit />
                  </NIcon>
                </template>
                <span>수정</span>
              </n-button>
              <n-button v-on:click="handleFetchProfile" secondary type="primary" title="갱신">
                <template #icon>
                  <NIcon>
                    <Refresh />
                  </NIcon>
                </template>
              </n-button>
            </div>
          </div>

          <!-- INTRO TEXT -->
          <p class="text-(--ric-text-secondary)">{{ userStore.user?.introText }}</p>

          <!-- XP -->
          <div class="flex flex-col gap-2">
            <!-- INFO -->
            <div class="flex justify-between items-center gap-4">
              <span class="text-xs text-(--ric-text-tertiary)">경험치(XP)</span>
              <span class="text-xs text-(--ric-color-primary)">{{ nextTierInfo.currentXP }}/{{ tierInfo.maxXP }}</span>
            </div>

            <!-- PROGRESS BAR -->
            <n-progress v-bind:percentage="nextTierInfo.progressPercent" v-bind:color="themeVars.primaryColor" indicator-placement="inside" type="line" indicator-text-color="var(--ric-text-primary)" processing />
            <span class="text-xs text-(--ric-text-tertiary) text-right">다음 티어까지 {{ nextTierInfo.XP2NextTier }}XP 남았습니다.</span>
          </div>

          <!-- SUB INFO -->
          <div class="flex flex-wrap gap-4">
            <!-- JOINED AT -->
            <span class="text-sm text-(--ric-text-secondary) flex items-center border border-slate-700 bg-(--ric-bg-popover) rounded-sm px-2 py-1">{{ new Date(userStore.user?.joinedAt!).toLocaleDateString('ko-kr') }}</span>

            <!-- RANK -->
            <span class="text-sm text-(--ric-text-secondary) flex items-center border border-slate-700 bg-(--ric-bg-popover) rounded-sm px-2 py-1">{{ myRankValue }}</span>

            <!-- TIER -->
            <span class="font-bold text-sm text-(--ric-color-primary) flex items-center gap-1 border border-(--ric-color-primary)/50 bg-(--ric-color-primary)/15 rounded-sm px-2 py-1">
              <NIcon size="20" v-bind:color="getTierColorFromTierType(tierInfo.tier)">
                <component v-bind:is="getTierIconComponentFromTierType(tierInfo.tier)" />
              </NIcon>
              <span>{{ tierInfo.tier }}</span>
            </span>
          </div>
        </div>
      </div>

      <!-- GRID -->
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div class="bg-(--ric-bg-card) border border-slate-700 rounded-md p-4">
          <!-- Algorithm Distributions -->
          <div class="flex flex-col gap-4">
            <!-- TITLE -->
            <div class="flex items-center gap-2 text-(--ric-color-primary)">
              <n-icon class="text-2xl">
                <HexagonOutlined />
              </n-icon>
              <h4 class="font-bold">알고리즘 분포</h4>
            </div>

            <!-- CHART -->
            <Radar v-bind:data="algoDistChartData" v-bind:options="algoDistChartOpts" />

            <!-- TABLE -->
            <n-table v-bind:bordered="false">
              <thead>
                <tr>
                  <th>알고리즘 유형</th>
                  <th>문제 수</th>
                  <th>점유율</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="algo in algoDummyData" v-bind:key="algo.type">
                  <td>{{ algo.type }}</td>
                  <td>{{ algo.solvedCount }}</td>
                  <td>{{ algo.percentage }}%</td>
                </tr>
              </tbody>
            </n-table>
          </div>
        </div>

        <div class="bg-(--ric-bg-card) border border-slate-700 rounded-md p-4">
          <!-- History -->
          <div class="flex flex-col gap-4">
            <div class="flex justify-between items-center gap-4">
              <div class="shrink-0 flex items-center gap-2 text-(--ric-color-primary)">
                <n-icon class="text-2xl">
                  <History />
                </n-icon>
                <h4 class="font-bold">풀이 기록</h4>
              </div>

              <n-select v-model:value="selectedAlgorithmType" v-bind:options="selectAlgorithmOpts" placeholder="알고리즘 유형" clearable />
            </div>

            <!-- TABLE -->
            <n-table>
              <thead>
                <tr>
                  <th>문제 번호</th>
                  <th>문제 제목</th>
                  <th>풀이 날짜</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in paginatedProblemHistory" v-bind:key="p.id">
                  <td>{{ p.id }}</td>
                  <td>{{ p.title }}</td>
                  <td>{{ p.date.toLocaleDateString('ko-KR') }}</td>
                </tr>
              </tbody>
            </n-table>

            <!-- PAGINATION -->
            <n-pagination v-model:page="currentPage" v-bind:page-count="totalPage" class="self-center" />
          </div>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import DefaultLayout from '@/components/layout/DefaultLayout.vue';
import { useUserStore } from '@/stores/userStore';
import { getNextTierFromTotalXP, getTierColorFromTierType, getTierIconComponentFromTierType, getTierInfoByTotalXP } from '@/utils/game/TierUtil';
import { Edit } from '@vicons/fa';
import { NButton, NIcon, NProgress, NTable, NSelect, NPagination, useThemeVars, useMessage } from 'naive-ui';
import { HexagonOutlined } from '@vicons/material';
import { Chart as ChartJS, RadialLinearScale, PointElement, LineElement, Filler, Tooltip, Legend } from 'chart.js';
import { Radar } from 'vue-chartjs';
import { History, Refresh } from '@vicons/tabler';
import { computed, onMounted, ref } from 'vue';
import { getMyInfo } from '@/api/userService';
import { HttpStatusCode } from 'axios';
import { getProfileImageUrl } from '@/utils/imageUtil';
import router from '@/router';
import { getUserRanking } from '@/api/contestService';

const userStore = useUserStore();
const themeVars = useThemeVars();
const message = useMessage();
const tierInfo = getTierInfoByTotalXP(userStore.user?.totalXp!);
const nextTierInfo = getNextTierFromTotalXP(userStore.user?.totalXp!);

const handleFetchProfile = async (e: MouseEvent) => {
  e.preventDefault();

  try {
    const resp = await getMyInfo();
    if (resp.statusCode === HttpStatusCode.Ok) {
      userStore.user = resp.data;
      message.success('사용자 정보를 갱신했습니다.');
    }
  } catch (err: any) {
    message.error(`${err.response.data.message}`)
  }
};

ChartJS.register(RadialLinearScale, PointElement, LineElement, Filler, Tooltip, Legend);
const algoDummyData = [
  { type: 'Amortized analysis', solvedCount: 15, percentage: 8 },
  { type: 'Bit manipulation', solvedCount: 28, percentage: 12 },
  { type: 'Complete search', solvedCount: 35, percentage: 14 },
  { type: 'Data structures', solvedCount: 42, percentage: 18 },
  { type: 'Dynamic programming', solvedCount: 38, percentage: 16 },
  { type: 'Greedy algorithms', solvedCount: 32, percentage: 11 },
  { type: 'Range queries', solvedCount: 22, percentage: 9 },
  { type: 'Sorting', solvedCount: 36, percentage: 12 },
];
const algoDistChartData = {
  labels: algoDummyData.map(algo => algo.type),
  datasets: [
    {
      label: '문제 수',
      data: algoDummyData.map(algo => algo.solvedCount),
      backgroundColor: 'rgba(34, 211, 238, 0.2)',
      borderColor: 'rgba(34, 211, 238, 1)',
      borderWidth: 2,
      pointBackgroundColor: 'rgba(34, 211, 238, 1)',
      pointBorderColor: '#fff',
      pointHoverBackgroundColor: '#fff',
      pointHoverBorderColor: 'rgba(34, 211, 238, 1)'
    }
  ]
};
const algoDistChartOpts = {
  responsive: true,
  maintainAspectRatio: true,
  scales: {
    r: {
      beginAtZero: true,
      ticks: {
        display: false,
        stepSize: 10
      },
      grid: {
        color: 'rgba(148, 163, 184, 0.2)'
      },
      pointLabels: {
        color: '#94a3b8',
        font: {
          size: 12
        }
      }
    }
  },
  plugins: {
    legend: {
      display: false
    },
    tooltip: {
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      titleColor: '#fff',
      bodyColor: '#fff',
      borderColor: 'rgba(34, 211, 238, 1)',
      borderWidth: 1
    }
  }
};

// ---------

const problemHistory = [
  { id: 1234, title: '이분 탐색 문제', date: new Date('2025-01-25'), algorithm: 'Complete search' },
  { id: 1235, title: '최단 경로', date: new Date('2025-01-24'), algorithm: 'Data structures' },
  { id: 1236, title: '동적 프로그래밍', date: new Date('2025-01-23'), algorithm: 'Dynamic programming' },
  { id: 1237, title: '정렬 알고리즘', date: new Date('2025-01-22'), algorithm: 'Sorting' },
  { id: 1238, title: '구현 문제', date: new Date('2025-01-21'), algorithm: 'Bit manipulation' },
  { id: 1239, title: '그리디 알고리즘', date: new Date('2025-01-20'), algorithm: 'Greedy algorithms' },
  { id: 1240, title: 'DFS/BFS', date: new Date('2025-01-19'), algorithm: 'Data structures' },
  { id: 1241, title: '배낭 문제', date: new Date('2025-01-18'), algorithm: 'Dynamic programming' },
  { id: 1242, title: '퀵소트', date: new Date('2025-01-17'), algorithm: 'Sorting' },
  { id: 1243, title: '시뮬레이션', date: new Date('2025-01-16'), algorithm: 'Amortized analysis' },
  { id: 1244, title: '범위 쿼리', date: new Date('2025-01-15'), algorithm: 'Range queries' }
];
const selectedAlgorithmType = ref<string | null>(null);
const selectAlgorithmOpts = algoDummyData.map(algo => ({ label: algo.type, value: algo.type }));
const filtertedProblemHistory = computed(() => {
  if (!selectedAlgorithmType.value) return problemHistory;
  return problemHistory.filter(ph => ph.algorithm === selectedAlgorithmType.value);
});

// ----------

const currentPage = ref(1);
const totalPage = computed(() => {
  return Math.ceil(filtertedProblemHistory.value.length / 10);
});
const paginatedProblemHistory = computed(() => {
  return filtertedProblemHistory.value.slice(
    (currentPage.value - 1) * 10,
    (currentPage.value - 1) * 10 + 10
  );
});

// ----------

const myRankValue = ref<string | number>();
onMounted(async () => {
  if (userStore.user?.userId) {
    try {
      const resp = await getUserRanking(userStore.user.userId);
      if (resp.statusCode === HttpStatusCode.Ok && resp.data?.rank) {
        myRankValue.value = resp.data.rank;
      }
    } catch (err) {
      myRankValue.value = '알 수 없음';
    }
  }
});
</script>