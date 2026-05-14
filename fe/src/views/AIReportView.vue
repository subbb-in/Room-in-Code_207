<template>
  <DefaultLayout>
    <div class="flex flex-col gap-6 w-full">
      <!-- 헤더 -->
      <div class="flex items-center justify-between">
        <div class="flex flex-col gap-2">
          <h1 class="text-4xl font-bold text-white">AI 분석 리포트</h1>
          <p class="text-sm text-(--ric-text-secondary)">AI가 제출 코드를 분석하고 개선 방안을 제시합니다.</p>
        </div>
        <div class="flex gap-2">
          <!-- <n-button @click="handleGoMain" type="default">문제로 돌아가기</n-button> -->
          <n-button @click="handleGoHome" type="primary">메인으로</n-button>
        </div>
      </div>

      <!-- 로딩 상태 -->
      <div v-if="!report" class="rounded-lg border border-slate-700 bg-(--ric-bg-card) p-8">
        <div class="flex flex-col items-center justify-center gap-4">
          <n-spin :show="true" size="large" />
          <p class="text-(--ric-text-secondary)">AI 분석 결과를 기다리는 중입니다...</p>
        </div>
      </div>

      <!-- 메인 컨텐츠 -->
      <div v-else class="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <!-- 왼쪽: 코드 비교 -->
        <div class="lg:col-span-2 flex flex-col gap-6">
          <!-- 코드 Diff -->
          <div class="rounded-lg border border-slate-700 bg-(--ric-bg-card) p-6">
            <h2 class="text-lg font-bold text-white mb-4">코드 개선 비교</h2>
            <div class="flex items-center gap-8 mb-4">
              <div class="flex-1">
                <p class="text-sm font-semibold text-(--ric-text-secondary)">원본 제출 코드</p>
              </div>
              <svg class="w-6 h-6 text-(--ric-text-secondary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
              <div class="flex-1">
                <p class="text-sm font-semibold text-(--ric-text-secondary)">AI 개선 코드</p>
              </div>
            </div>
            <div class="h-[450px] rounded border border-slate-600 overflow-hidden">
              <MonacoDiffEditor
                v-if="originalCode || refactoredCode"
                :original="originalCode || '/* 코드가 없습니다 */'"
                :modified="refactoredCode || '/* 개선 코드가 없습니다 */'"
                :language="programmingLanguage"
              />
              <div v-else class="flex items-center justify-center h-full text-(--ric-text-tertiary)">
                코드 비교 데이터가 없습니다.
              </div>
            </div>
          </div>
        </div>

        <!-- 오른쪽: 정보 패널 -->
        <div class="flex flex-col gap-6">
          <!-- 실행 결과 -->
          <div class="rounded-lg border border-slate-700 bg-(--ric-bg-card) p-6">
            <h2 class="text-lg font-bold text-white mb-4">⚡ 실행 결과</h2>
            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <span class="text-sm text-(--ric-text-secondary)">실행 시간</span>
                <span class="font-semibold text-white">{{ gradedTime }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-(--ric-text-secondary)">메모리 사용량</span>
                <span class="font-semibold text-white">{{ gradedMemory }}</span>
              </div>
            </div>
          </div>

          <!-- AI 분석 코멘트 -->
          <div class="rounded-lg border border-slate-700 bg-(--ric-bg-card) p-6">
            <h2 class="text-lg font-bold text-white mb-4">💬 AI 분석 코멘트</h2>
            <div v-if="aiComment" class="space-y-3">
              <p class="text-sm text-white leading-relaxed whitespace-pre-wrap">{{ aiComment }}</p>
            </div>
            <div v-else class="text-sm text-(--ric-text-tertiary) italic">
              AI 코멘트가 없습니다.
            </div>
          </div>

          <!-- 추천 학습 자료 -->
          <div class="rounded-lg border border-slate-700 bg-(--ric-bg-card) p-6">
            <h2 class="text-lg font-bold text-white mb-4">📚 추천 학습 자료</h2>
            <div v-if="recommendations.length" class="space-y-2 max-h-[200px] overflow-y-auto">
              <a
                v-for="(item, index) in recommendations"
                :key="index"
                :href="item.url"
                target="_blank"
                rel="noreferrer"
                class="block p-2 rounded bg-slate-700/50 hover:bg-slate-700 transition-colors"
              >
                <p class="text-sm text-(--ric-color-primary) hover:text-blue-400 truncate">
                  🔗 {{ item.title }}
                </p>
              </a>
            </div>
            <div v-else class="text-sm text-(--ric-text-tertiary) italic">
              추천 학습 자료가 없습니다.
            </div>
          </div>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import DefaultLayout from '@/components/layout/DefaultLayout.vue';
import { useMatchStore } from '@/stores/matchStore';
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { NButton, NSpin } from 'naive-ui';
import MonacoDiffEditor from '@/components/MonacoDiffEditor.vue';

const router = useRouter();
const matchStore = useMatchStore();

// 이 페이지에서는 sessionStorage 복원하지 않음 → 직접 /ai/report/ 접속 시 데이터 없음(로딩만 표시)
// AI_REPORT 수신 후 watch로 넘어올 때만 Pinia에 있는 데이터로 표시
onMounted(() => {});

// 데이터 흐름: AI(ai/app) → BE(AiClientService) → WS(AI_REPORT) → matchStore.aiReport
// 표시: aiReport(수신) + lastSubmittedCode(제출 시 저장). AI 응답 필드: comment, refactored_code, recommendation
const report = computed(() => matchStore.aiReport);

const aiFeedback = computed(() => {
  const feedback = report.value?.aiFeedback;
  return typeof feedback === 'object' ? feedback : null;
});

const aiComment = computed(() => {
  const comment = aiFeedback.value?.comment;
  return typeof comment === 'string' ? comment : '';
});

const originalCode = computed(() => matchStore.lastSubmittedCode ?? '');

const refactoredCode = computed(() => {
  const code = aiFeedback.value?.refactored_code;
  return typeof code === 'string' ? code : '';
});

// AI_REPORT의 gameResult에는 language가 없을 수 있음 → matchStore 언어로 보완
const programmingLanguage = computed(() => {
  const gameResult = report.value?.gameResult;
  if (typeof gameResult === 'object' && gameResult) {
    const lang = (gameResult as Record<string, unknown>)?.language;
    if (typeof lang === 'string') {
      const lower = lang.toLowerCase();
      return lower === 'cpp' ? 'cpp' : lower === 'java' ? 'java' : lower === 'python' ? 'python' : lower === 'javascript' ? 'javascript' : 'python';
    }
  }
  const storeLang = matchStore.gameInfo.programmingLanguage;
  if (typeof storeLang === 'string') {
    const lower = storeLang.toLowerCase();
    return lower === 'cpp' ? 'cpp' : lower === 'java' ? 'java' : lower === 'python' ? 'python' : lower === 'javascript' ? 'javascript' : 'python';
  }
  return 'python';
});

const gradedTime = computed(() => {
  const graded = report.value?.gradedResult;
  if (typeof graded === 'object' && graded) {
    const time = (graded as Record<string, unknown>)?.time;
    return typeof time === 'string' || typeof time === 'number' ? `${time}ms` : '-';
  }
  return '-';
});

const gradedMemory = computed(() => {
  const graded = report.value?.gradedResult;
  if (typeof graded === 'object' && graded) {
    const memory = (graded as Record<string, unknown>)?.memory;
    return typeof memory === 'string' || typeof memory === 'number' ? `${memory}MB` : '-';
  }
  return '-';
});

// AI 서버: recommendation = Dict[str, str] (예: {"추천 문제": "url"})
const recommendations = computed(() => {
  const rec = aiFeedback.value?.recommendation;
  if (!rec || typeof rec !== 'object') return [] as { title: string; url: string }[];
  return Object.entries(rec as Record<string, unknown>)
    .filter(([_, url]) => typeof url === 'string')
    .map(([title, url]) => ({ title, url: url as string }));
});

const handleGoMain = () => {
  router.push({ name: 'main' });
};

const handleGoHome = () => {
  router.push({ name: 'main' });
};
</script>

<style scoped>
.monaco-diff-editor {
  font-family: 'Fira Code', monospace;
}

</style>