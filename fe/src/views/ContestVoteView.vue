<template>
    <DefaultLayout>
        <div class="flex flex-col gap-4">

            <!-- 헤더 -->
            <div class="flex flex-col w-full rounded-lg p-6 gap-1 border border-slate-700 bg-slate-800">
                <span class="text-sm font-bold text-cyan-400">매칭완료</span>
                <span class="text-3xl font-bold text-white">알고리즘을 투표하세요</span>
                <span class="text-sm text-slate-400 pt-1">팀원들과 함께 라운드에서 풀 알고리즘을 선택합니다. 다수결로
                    결정됩니다.</span>
            </div>
                
                <!-- 알고 투표 독려 -->
            <div class="flex w-full items-center gap-2">
                    <!-- <div class="flex gap-2 items-center"> -->
                        <n-icon class="text-cyan-400 text-2xl font-bold">
                            <CheckBoxOutlined />
                        </n-icon>
                        <span class="font-bold text-xl ml-2">알고리즘 선택</span>
                        <div class="flex gap-1 pl-2 items-center">
                            <Icon class="text-cyan-400 size-3">
                                <Stopwatch />
                            </Icon>
                            <span class="text-sm text-cyan-400 ">{{ time }}초 내에 투표해주세요</span>
                        </div>
                    <!-- </div> -->
                    <div class="text-2xl text-red-500 ml-auto">00:{{ time }}</div>
                </div>
                
                <!-- 알고리즘 투표 창 -->
                <div class="flex w-full gap-6">
                    <div v-for="candidate in matchStore.voteCandidates" v-bind:key="candidate" v-on:click="handleVote(candidate)"
                    class="bg-slate-800 border-slate-700 w-1/2 h-20 flex flex-col items-start justify-center p-4 rounded-lg transition-all"
                    v-bind:class="{ 
                        'shadow-[0_0_10px_5px_#67e8f9]': selectAlgo === candidate,
                        'cursor-pointer hover:bg-slate-700': time > 0,
                        'opacity-60 cursor-not-allowed': time <= 0
                    }">
                    <div class="text-white text-lg font-bold">{{ candidate }}</div>
                    <div class="text-sm text-slate-400">이 알고리즘을 선택합니다.</div>
                </div>
            </div>
            
            <!-- 당신의 선택은?? -->
            <div
            class="flex flex-row w-full items-center border bg-slate-800  border-slate-700 rounded px-6 py-4 h-15">
                <div v-show="selectAlgo" class="flex gap-2 items-center text-md">
                    <n-icon class="text-cyan-400 ">
                        <Checks />
                    </n-icon>
                    <span>당신의 선택 :</span>
                    <span class="font-bold text-white">{{ selectAlgo }}</span>
            </div>
            </div>
        </div>
    </DefaultLayout>
</template>

<script setup lang="ts">
import DefaultLayout from '@/components/layout/DefaultLayout.vue';
import { CheckSquare, CheckSquareRegular, Stopwatch } from '@vicons/fa';
import { CheckBoxOutlined } from '@vicons/material';
import { Checks } from '@vicons/tabler';
import { NButton, NIcon } from 'naive-ui';
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/userStore';
import { useMatchStore } from '@/stores/matchStore';
import { useContestMatchWebSocket } from '@/ws/useContestMatchWebSocket';

const time = ref(30)
const timerInterval = ref<number | null>(null);
const selectAlgo = ref<string | null>(null);

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const matchStore = useMatchStore();

const { connect, disconnect, connected, sendLeaveRequest, sendVoteRequest } = useContestMatchWebSocket();

const handleVote = (algo: string) => {
    if (time.value <= 0) return; // 시간 종료 시 투표 변경 불가
    selectAlgo.value = algo;
};

// voteCandidates 변경 감시 (즉시 및 이후 변경 시)
watch(() => matchStore.voteCandidates, (newCandidates) => {
    console.log('[ContestVoteView] voteCandidates 갱신:', newCandidates);
}, { immediate: true, deep: true });

const startCountdown = () => {
    if (timerInterval.value) return;
    
    timerInterval.value = window.setInterval(() => {
        if (time.value > 0) {
            time.value--;
        } else {
            // 시간 종료 시 투표 자동 전송
            if (timerInterval.value) {
                window.clearInterval(timerInterval.value);
                timerInterval.value = null;
            }
            
            if (!selectAlgo.value && matchStore.voteCandidates.length > 0) {
                // 선택된 알고리즘이 없으면 랜덤으로 선택
                const randomIndex = Math.floor(Math.random() * matchStore.voteCandidates.length);
                selectAlgo.value = matchStore.voteCandidates[randomIndex] || null;
                console.info('[Vote] 시간 종료: 랜덤 알고리즘 선택됨 ->', selectAlgo.value);
            }

            if (selectAlgo.value) {
                const matchId = route.params.matchId as string;
                if (matchId) {
                    sendVoteRequest(matchId, selectAlgo.value);
                }
            } else {
                console.warn('[Vote] 시간 종료: 선택 가능한 알고리즘 후보가 없습니다.');
            }
        }
    }, 1000);
};

onMounted(() => {
    const matchId = route.params.matchId as string;
    if (!matchId) {
        router.push({ name: 'main' });
        return;
    }

    // 현재 store 상태 확인
    console.log('[ContestVoteView] onMounted - 현재 voteCandidates:', matchStore.voteCandidates);

    // WebSocket 연결
    if (!connected.value) {
        connect(matchId, userStore.user?.userId.toString()!, userStore.accessToken);
    }

    // 카운트다운 시작
    startCountdown();
});

onUnmounted(() => {
    if (timerInterval.value) {
        window.clearInterval(timerInterval.value);
    }
    
    if (connected.value) {
        const matchId = route.params.matchId as string;
        // LEAVE 메시지 전송 후 약간의 지연 후 연결 해제
        sendLeaveRequest(matchId || matchStore.gameInfo.matchId);
        setTimeout(() => {
            disconnect();
        }, 100);
    }
});
</script>

<style scoped></style>