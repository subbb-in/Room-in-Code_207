// src/stores/matchStore.ts
import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { MatchStatus, TurnInfo } from './MatchMessage';

export const useMatchStore = defineStore('match', () => {
    const connected = ref(false);
    const phase = ref<MatchStatus>('DESIGN');
    const turnInfo = ref<TurnInfo | null>(null);
    const hpMap = ref<Record<number, number>>({}); // userId가 number이므로 number로 변경
    const hintMap = ref<Record<number, string>>({});
    const currentCode = ref('');
    const logs = ref<any[]>([]);
    const matchId = ref<string | null>(null);

    //테스트용 userId설정---------------------------------------
    const userId = ref<number | null>(null);
    //--------------------------------------------------------

    const setUserId = (id: number) => { userId.value = id; };
    // 상태 업데이트 메서드 (Pure Actions)
    const updatePhase = (newPhase: MatchStatus) => { phase.value = newPhase; };
    const updateHp = (userId: number, hp: number) => { hpMap.value[userId] = hp; };
    const updateHint = (seq: number, content: string) => { hintMap.value[seq] = content; };
    const updateCode = (code: string) => { currentCode.value = code; };
    const updateTimer = (timer: TurnInfo) => { turnInfo.value = timer; };

    const addLog = (direction: 'SENT' | 'RECEIVED' | 'ERROR', type: string, data: any) => {
        logs.value.push({ id: Date.now(), time: new Date().toLocaleTimeString(), direction, type, data });
        if (logs.value.length > 100) logs.value.shift();
    };

    const resetStatus = () => {
        connected.value = false;
        phase.value = 'DESIGN';
        hpMap.value = {};
        hintMap.value = {};
        currentCode.value = '';
        matchId.value = null;
        userId.value = null;
    };

    return {
        connected, phase, turnInfo, hpMap, hintMap, currentCode, logs, matchId, userId,
        addLog, resetStatus, updatePhase, updateHp, updateHint, updateCode, updateTimer
    };
}, {
    persist: {
        storage: sessionStorage
    }
});