import { type MatchGameInfo } from "@/types/ws/match/MatchGameInfo";
import { type MatchUserInfo } from "@/types/ws/match/MatchUserInfo";
import { defineStore } from "pinia";
import { ref } from "vue";

export const useMatchStore = defineStore('match', () => {
  const gameInfo = ref<MatchGameInfo>({
    matchId: '',
    mode: undefined,
    phaseType: 'DESIGN',
    designTimePerSec: 0,
    remainingTimePerSec: 0,
    currentTurnUserId: -1,
    problemInfo: undefined,
    programmingLanguage: undefined,
  });

  const userList = ref<MatchUserInfo[]>([]);

  // 현재 표시할 이모지 (userId -> emoji) - 프로필 말풍선용 (미사용)
  const currentEmoji = ref<Record<string, string>>({});

  // 떠다니는 이모지들 (거품 애니메이션)
  interface FloatingEmoji {
    id: string;
    emoji: string;
    left: number;
    bottom: number;
  }
  const floatingEmojis = ref<FloatingEmoji[]>([]);

  // 힌트 보관 (sequence -> content)
  const hintContents = ref<Record<number, string | null>>({});
  const lastHintSequence = ref<number | null>(null);

  const voteCandidates = ref<string[]>([]);
  const lastJudgeResult = ref<{ success: boolean; message?: string; stdout?: string; execution?: { time?: string; memory?: string } } | null>(null);
  const lastGameEndMessage = ref<string | null>(null);
  const aiCodePlayback = ref<{ id: number; code: string; durationSec?: number } | null>(null);
  const aiReport = ref<{
    matchId: string;
    userId?: string;
    aiFeedback?: Record<string, unknown> | null;
    gameResult?: Record<string, unknown> | null;
    gradedResult?: Record<string, unknown> | null;
    raw?: unknown;
  } | null>(null);
  /** GAME_END 수신 후 AI_REPORT 대기 중일 때 true. 모든 모드에서 제출/힌트 잠금 및 "AI 리포트 대기 중" 표시용 */
  const waitingForAiReport = ref(false);
  const lastSubmittedCode = ref<string | null>(null);

  const setUserList = (users: MatchUserInfo[]) => {
    userList.value = users;
  };

  const resetStore = () => {
    // programmingLanguage는 보존 (사용자가 선택한 언어 유지)
    const preservedLanguage = gameInfo.value.programmingLanguage;

    gameInfo.value = {
      matchId: '',
      mode: undefined,
      phaseType: 'DESIGN',
      designTimePerSec: 0,
      remainingTimePerSec: 0,
      currentTurnUserId: -1,
      problemInfo: undefined,
      programmingLanguage: preservedLanguage, // 언어 정보 보존
    };
    userList.value = [];
    currentEmoji.value = {};
    floatingEmojis.value = [];
    hintContents.value = {};
    lastHintSequence.value = null;
    voteCandidates.value = [];
    lastJudgeResult.value = null;
    lastGameEndMessage.value = null;
    aiCodePlayback.value = null;
    aiReport.value = null;
    waitingForAiReport.value = false;
    lastSubmittedCode.value = null;
  };

  const setMatchId = (matchId: string) => {
    // 일부 데이터만 선별적 초기화 (매치 전환 시)
    gameInfo.value.matchId = matchId;
    gameInfo.value.problemInfo = undefined;
    voteCandidates.value = [];
    aiReport.value = null;
    waitingForAiReport.value = false;
    lastSubmittedCode.value = null;
    hintContents.value = {};
    lastHintSequence.value = null;
  };

  const setProgrammingLanguage = (lang: string) => {
    gameInfo.value.programmingLanguage = lang;
    // sessionStorage에 저장하여 페이지 새로고침/이동 시에도 언어 유지
    try {
      sessionStorage.setItem('programmingLanguage', lang);
    } catch (e) {
      console.warn('Failed to save programmingLanguage to sessionStorage:', e);
    }
  };

  const setMode = (mode: string) => {
    gameInfo.value.mode = mode;
  };

  const addFloatingEmoji = (emoji: string) => {
    // 한 번에 6개의 이모지를 다양한 위치에서 생성 (보글보글 효과)
    for (let i = 0; i < 6; i++) {
      const id = `${Date.now()}-${Math.random()}-${i}`;
      const left = Math.random() * 85 + 5; // 화면 가로 5% ~ 90% 사이
      const bottom = Math.random() * 25 - 10; // 화면 하단 -10% ~ 15% 사이 (더 넓은 범위)

      floatingEmojis.value.push({ id, emoji, left, bottom });

      // 4초 후 자동 제거 (CSS 애니메이션 시간과 동일)
      setTimeout(() => {
        floatingEmojis.value = floatingEmojis.value.filter(e => e.id !== id);
      }, 4000);
    }
  };

  const setHP = (userId: string | number, hp: number) => {
    if (userList.value?.length) {
      const user = userList.value.find(u => String(u.userId) === String(userId));
      if (user) {
        user.remainingHp = hp;
      }
    }
  };

  const removeUser = (userId: string | number) => {
    userList.value = userList.value.filter(u => String(u.userId) !== String(userId));
  };

  const setHintContent = (sequence: number, content: string | null) => {
    hintContents.value = {
      ...hintContents.value,
      [sequence]: content
    };
    lastHintSequence.value = sequence;
  };

  const setVoteCandidates = (candidates: string[]) => {
    voteCandidates.value = candidates;
  };

  const setJudgeResult = (result: { success: boolean; message?: string; stdout?: string; execution?: { time?: string; memory?: string } } | null) => {
    lastJudgeResult.value = result;
  };

  const setGameEndMessage = (message: string | null) => {
    lastGameEndMessage.value = message;
  };

  /** JUDGE_RESULT success + GAME_END 후 AI_REPORT 대기 상태. true면 버튼 잠금 + "AI 리포트 대기 중" 표시 */
  const setWaitingForAiReport = (value: boolean) => {
    waitingForAiReport.value = value;
  };

  const setAiCodePlayback = (code: string, durationSec?: number) => {
    aiCodePlayback.value = {
      id: Date.now(),
      code,
      durationSec
    };
  };

  const setAiReport = (report: {
    matchId: string;
    userId?: string;
    aiFeedback?: Record<string, unknown> | null;
    gameResult?: Record<string, unknown> | null;
    gradedResult?: Record<string, unknown> | null;
    raw?: unknown;
  } | null) => {
    aiReport.value = report;
    if (report) waitingForAiReport.value = false;
    // localStorage에 저장하여 페이지 새로고침 시에도 데이터 유지
    if (report) {
      try {
        sessionStorage.setItem('aiReport', JSON.stringify(report));
      } catch (e) {
        console.warn('Failed to save AI report to sessionStorage:', e);
      }
    }
  };

  const loadAiReportFromStorage = () => {
    try {
      const stored = sessionStorage.getItem('aiReport');
      if (stored) {
        const report = JSON.parse(stored);
        aiReport.value = report;
        return report;
      }
    } catch (e) {
      console.warn('Failed to load AI report from sessionStorage:', e);
    }
    return null;
  };

  const clearAiReport = () => {
    aiReport.value = null;
    try {
      sessionStorage.removeItem('aiReport');
    } catch (e) {
      console.warn('Failed to clear AI report from sessionStorage:', e);
    }
  };

  const setLastSubmittedCode = (code: string | null) => {
    lastSubmittedCode.value = code;
    // sessionStorage에 저장하여 페이지 새로고침 시 데이터 유지
    try {
      if (code) {
        sessionStorage.setItem('lastSubmittedCode', code);
      } else {
        sessionStorage.removeItem('lastSubmittedCode');
      }
    } catch (e) {
      console.warn('Failed to save lastSubmittedCode to sessionStorage:', e);
    }
  };

  const loadLastSubmittedCodeFromStorage = () => {
    try {
      const stored = sessionStorage.getItem('lastSubmittedCode');
      if (stored) {
        lastSubmittedCode.value = stored;
        return stored;
      }
    } catch (e) {
      console.warn('Failed to load lastSubmittedCode from sessionStorage:', e);
    }
    return null;
  };

  const loadProgrammingLanguageFromStorage = () => {
    try {
      const stored = sessionStorage.getItem('programmingLanguage');
      if (stored) {
        gameInfo.value.programmingLanguage = stored;
        return stored;
      }
    } catch (e) {
      console.warn('Failed to load programmingLanguage from sessionStorage:', e);
    }
    return null;
  };

  const clearProgrammingLanguage = () => {
    gameInfo.value.programmingLanguage = undefined;
    try {
      sessionStorage.removeItem('programmingLanguage');
    } catch (e) {
      console.warn('Failed to clear programmingLanguage from sessionStorage:', e);
    }
  };

  return {
    gameInfo,
    userList,
    currentEmoji,
    floatingEmojis,
    hintContents,
    lastHintSequence,
    voteCandidates,
    lastJudgeResult,
    lastGameEndMessage,
    aiCodePlayback,
    aiReport,
    waitingForAiReport,
    lastSubmittedCode,

    resetStore,
    setHP,
    setUserList,
    addFloatingEmoji,
    setHintContent,
    removeUser,
    setMatchId,
    setProgrammingLanguage,
    setMode,
    setVoteCandidates,
    setJudgeResult,
    setGameEndMessage,
    setWaitingForAiReport,
    setAiCodePlayback,
    setAiReport,
    loadAiReportFromStorage,
    clearAiReport,
    setLastSubmittedCode,
    loadLastSubmittedCodeFromStorage,
    loadProgrammingLanguageFromStorage,
    clearProgrammingLanguage
  }
});