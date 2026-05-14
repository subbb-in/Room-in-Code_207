<template>
  <div class="h-screen flex flex-col overflow-hidden relative">
    <!-- 떠다니는 이모지 거품 -->
    <div v-if="props.emoji && props.mode !== 'AI'" v-for="emoji in matchStore.floatingEmojis" :key="emoji.id"
      class="fixed text-4xl pointer-events-none floating-emoji" :style="{
        left: emoji.left + '%',
        bottom: emoji.bottom + 'vh'
      }">
      {{ emoji.emoji }}
    </div>

    <!-- HINT MODAL -->
    <n-modal v-model:show="hintModalOpen">
      <n-card class="max-w-md w-full" :title="hintModalTitle" size="large" bordered>
        <p class="text-(--ric-text-tertiary) whitespace-pre-line">
          {{ hintModalContent }}
        </p>
        <template #footer>
          <div class="flex justify-end">
            <n-button @click="hintModalOpen = false">확인</n-button>
          </div>
        </template>
      </n-card>
    </n-modal>

    <!-- GRID -->
    <div class="flex-1 grid grid-cols-1 overflow-hidden sm:grid-cols-[320px_1fr_320px]">
      <!-- COL 1 -->
      <div class="flex flex-col gap-4 p-4 border-r border-r-slate-700 bg-(--ric-bg-body) overflow-y-scroll">
        <!-- TITLE: 아직 서버에서 데이터 안 넘김 -->
        <h4 class="font-bold text-2xl">문제 제목</h4>

        <!-- CONDITIONS -->
        <div class="grid grid-cols-3 gap-4 rounded-md p-4 bg-(--ric-bg-card-mid)">
          <!-- TIME -->
          <div class="flex flex-col gap-1">
            <span class="font-bold text-sm">시간 제한</span>
            <span class="text-(--ric-text-tertiary) wrap-break-word">{{ matchStore.gameInfo.problemInfo?.timeLimit
            }}</span>
          </div>

          <!-- MEMORY -->
          <div class="flex flex-col gap-1">
            <span class="font-bold text-sm">메모리 제한</span>
            <span class="text-(--ric-text-tertiary) wrap-break-word">{{ matchStore.gameInfo.problemInfo?.memoryLimit
            }}</span>
          </div>

          <!-- LEVEL -->
          <div class="flex flex-col gap-1">
            <span class="font-bold text-sm">난이도</span>
            <span class="text-(--ric-text-tertiary) wrap-break-word">{{ matchStore.gameInfo.problemInfo?.difficulty
            }}</span>
          </div>
        </div>

        <!-- CONTENT -->
        <div class="flex flex-col gap-4 rounded-md p-4 bg-(--ric-bg-card-mid)">
          <div class="flex justify-between items-center gap-2">
            <span class="font-bold">문제 내용</span>

            <n-button v-on:click="handleTranslate" size="small" tertiary>
              <template #icon>
                <NIcon>
                  <Language />
                </NIcon>
              </template>
            </n-button>
          </div>

          <p class="text-(--ric-text-tertiary) whitespace-pre-line">{{ normalizedProblemContent }}</p>
        </div>

        <!-- SAMPLE -->
        <div class="flex flex-col gap-4 rounded-md p-4 bg-(--ric-bg-card-mid)">
          <span class="font-bold">입출력 예제</span>
          <p class="text-(--ric-text-tertiary) whitespace-pre-line">{{ normalizedInputOutput }}</p>
        </div>

        <!-- HINT BUTTON -->
        <div class="flex flex-col gap-4 rounded-md p-4 bg-(--ric-bg-card-mid)">
          <span class="font-bold">힌트</span>

          <ul class="grid grid-cols-1 gap-4">
            <li v-for="(btn, idx) in hintButtonList" v-bind:key="btn.name" class="flex-1">
              <n-button v-on:click="() => handleHintButton(idx + 1)"
                :disabled="matchStore.gameInfo.phaseType !== 'PLAYING' || matchStore.waitingForAiReport" secondary size="large" class="w-full! text-lg!">
                <template #icon>
                  <NIcon>
                    <component v-bind:class="btn.iconColor" v-bind:is="btn.iconComponent" />
                  </NIcon>
                </template>
                <span>{{ btn.label }}</span>
              </n-button>
            </li>
          </ul>
        </div>
      </div>

      <!-- COL 2 -->
      <div class="flex flex-col items-center w-full bg-(--ric-bg-body)">
        <div class="flex flex-row gap-4 items-center p-4 w-full">
          <!-- TIMER -->
          <div class="shrink-0 flex flex-col items-center gap-2 px-4 py-3 rounded-xl border transition duration-300"
            :class="timerWrapClass">
            <div class="flex items-center gap-2">
              <NIcon size="28" :class="timerIconClass">
                <TimerRound />
              </NIcon>

              <span class="font-bold text-3xl" :class="timerClass">{{ formattingTime }}</span>
            </div>

            <div class="flex items-center gap-2 px-3 py-1 text-xs font-semibold rounded-full border"
              :class="turnStatePillClass">
              <span class="inline-block size-2 rounded-full" :class="turnStateDotClass"></span>
              <span>{{ turnStateLabel }}</span>
            </div>
          </div>

          <div class="flex flex-col gap-4 w-full">
            <div class="w-full" v-show="isPlaying || isDesign">
              <div class="flex justify-between text-xs text-(--ric-text-tertiary) mb-1">
                <span>{{ progressLabel }}</span>
              </div>
              <n-progress type="line" :percentage="progressPercent" :height="8" :border-radius="6"
                :show-indicator="false" :color="progressColor" :rail-color="'rgba(255,255,255,0.16)'" />
            </div>

            <!-- PROFILE -->
            <ul class="flex justify-start items-center gap-8">
              <li v-for="user in matchStore.userList" v-bind:key="user.userId">
                <div class="flex items-center gap-2 relative">
                  <!-- IMAGE & BTN -->
                  <div class="relative group shrink-0">
                    <img v-bind:src="getProfileImageUrl(user.profileImageUrl) || '/defaultProfile.png'" :class="{
                      'border-4 border-(--ric-color-primary)': matchStore.gameInfo.currentTurnUserId.toString() === user.userId || matchStore.gameInfo.currentTurnUserId === Number(user.userId),
                      'border-4 border-slate-600': matchStore.gameInfo.currentTurnUserId.toString() !== user.userId && matchStore.gameInfo.currentTurnUserId !== Number(user.userId)
                    }" class="rounded-full size-12 transition duration-300 group-hover:brightness-50" />

                    <!-- 이모지 말풍선 제거됨 - 화면 아래에서 거품으로 표시됨 -->

                    <!-- <n-button 
                      v-if="String(user.userId) !== String(userStore.user?.userId) && voiceMutedUsers[String(user.userId)]" 
                      quaternary 
                      @click="toggleVoiceMute(String(user.userId))"
                      class="absolute! top-0! left-0! size-full! rounded-full! group-hover:text-(--ric-color-primary)!"
                    >
                      <template #icon>
                        <NIcon>
                          <VolumeMute />
                        </NIcon>
                      </template>
                    </n-button>

                    <n-button 
                      v-else-if="String(user.userId) !== String(userStore.user?.userId)"
                      quaternary 
                      @click="toggleVoiceMute(String(user.userId))"
                      class="absolute! top-0! left-0! size-full! rounded-full! group-hover:text-(--ric-color-primary)!"
                    >
                      <template #icon>
                        <NIcon>
                          <VolumeUp />
                        </NIcon>
                      </template>
                    </n-button> -->
                  </div>

                  <!-- HP & NICKNAME -->
                  <div class="flex flex-col gap-1">
                    <span class="font-bold text-xs">{{ user.nickname }}</span>
                    <div v-if="props.mode !== 'AI'" class="flex items-center gap-1 text-sm">
                      <span>HP:</span>
                      <span class="font-bold">{{ user.remainingHp }}</span>
                    </div>
                  </div>
                </div>
              </li>
            </ul>

          </div>
        </div>

        <!-- MONACO EDITOR -->
        <MonacoEditor />
      </div>

      <!-- COL 3 -->
      <div class="flex flex-col gap-4 bg-(--ric-bg-body) border-l border-l-slate-700 p-4">
        <!-- MEMO -->
        <div class="flex flex-col gap-4 flex-1">
          <div class="flex items-center gap-2">
            <NIcon>
              <EventNoteRound />
            </NIcon>
            <span>메모</span>
          </div>

          <div class="flex-1 min-h-[240px]">
            <n-input v-model:value="memo" type="textarea" placeholder="메모를 남겨주세요" v-bind:resizable="false"
              :autosize="{ minRows: 8, maxRows: 16 }" class="h-full" />
          </div>
        </div>

        <!-- FEEDBACK -->
        <div class="flex flex-col gap-4">
          <div class="flex items-center gap-2">
            <NIcon>
              <FeedbackRound />
            </NIcon>
            <span>피드백</span>
          </div>

          <p class="p-4 rounded-md bg-(--ric-bg-card-mid) text-(--ric-text-tertiary) text-sm">{{ feedbackDisplay }}</p>
        </div>
      </div>
    </div>

    <!-- AI 리포트 대기 중 배너 (모든 참가자 버튼 잠금 시 표시) -->
    <div v-if="matchStore.waitingForAiReport"
      class="flex items-center justify-center gap-2 py-3 px-4 bg-amber-500/15 border-y border-amber-400/40 text-amber-200">
      <n-spin size="small" />
      <span class="font-semibold">AI 리포트 대기 중입니다. 잠시만 기다려 주세요.</span>
    </div>

    <!-- FOOTER -->
    <div class="flex justify-between items-center gap-4 p-4 bg-(--ric-bg-body) border-t border-t-slate-700">
      <div class="flex items-center gap-4">
        <!-- EXIT -->
        <n-button @click="handleLeave" text>
          <template #icon>
            <NIcon>
              <ArrowLeft />
            </NIcon>
          </template>
          <span>나가기</span>
        </n-button>

        <!-- EMOJI -->
        <div v-if="props.emoji && props.mode !== 'AI'" class="relative z-50">
          <button @click="showEmojiPicker" class="px-2 py-1 text-sm">
            😊 이모지
          </button>

          <!-- EMOJI PANEL -->
          <div v-if="showEmojiPanel"
            class="absolute bottom-full left-0 mb-2 bg-(--ric-bg-card) rounded-lg px-4 py-2 border border-slate-700 flex items-center gap-2 z-50">
            <button v-for="emoji in ['😊', '👍', '🎉', '❤️', '😂', '🤔']" :key="emoji"
              @click="sendEmojiFromPanel(emoji)"
              class="text-2xl hover:scale-110 transition-transform duration-200 cursor-pointer">
              {{ emoji }}
            </button>
          </div>
        </div>

        <!-- VOICE -->
        <!-- <n-button v-if="props.voice" @click="toggleLocalAudio" :type="localAudioEnabled ? 'default' : 'error'" text>
          <template #icon>
            <NIcon>
              <component v-bind:is="localAudioEnabled ? VolumeUp : VolumeMute" />
            </NIcon>
          </template>
          <span>{{ localAudioEnabled ? '음성' : '음성 끔' }}</span>
        </n-button> -->
      </div>

      <n-button v-if="props.mode === 'AI' && isDesign" type="primary" @click="handleAiReady" :disabled="aiReadySent">
        <span>{{ aiReadySent ? '대기 중...' : '설계 완료' }}</span>
      </n-button>

      <n-button v-else type="primary" @click="handleSubmit" :disabled="matchStore.waitingForAiReport">
        <template #icon>
          <NIcon>
            <SendRound />
          </NIcon>
        </template>
        <span>{{ matchStore.waitingForAiReport ? 'AI 리포트 대기 중...' : '제출' }}</span>
      </n-button>
    </div>

    <!-- Hidden audio container for OpenVidu subscribers -->
    <!-- <div ref="audioContainer" class="hidden"></div> -->
  </div>
</template>

<script setup lang="ts">
import { hintButtonList } from '@/assets/ts/hintButtonList';
import { ArrowLeft /*, VolumeMute, VolumeUp*/ } from '@vicons/fa';
import { EventNoteRound, FeedbackRound, SendRound, TimerRound } from '@vicons/material';
import { NButton, NCard, NIcon, NInput, NModal, NProgress, NSpin, useMessage } from 'naive-ui';
import MonacoEditor from '../MonacoEditor.vue';
import { Language } from '@vicons/tabler';
import { useMatchWebSocket } from '@/ws/useMatchWebSocket';
import { useAIWebSocket } from '@/ws/useAIWebSocket';
import { useContestMatchWebSocket } from '@/ws/useContestMatchWebSocket';
import { computed, onMounted, onUnmounted, provide, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/userStore';
import { useMatchStore } from '@/stores/matchStore';
import { getProfileImageUrl } from '@/utils/imageUtil';
// import { getMatchOpenViduToken } from '@/api/openviduService';
import { submitCode } from '@/api/matchService';
// import { OpenVidu, type Publisher, type Session, type Subscriber } from 'openvidu-browser';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const matchStore = useMatchStore();
const message = useMessage();

const props = defineProps<{
  mode: string,       // 모드 (AI, FRIENDS, CONTEST)
  emoji: boolean,      // 이모지
  // voice:  boolean,      // 음성 채팅
}>();

// Web Socket
// ----------
const getSocketHook = () => {
  if (props.mode === 'AI') return useAIWebSocket();
  if (props.mode === 'CONTEST') return useContestMatchWebSocket();
  return useMatchWebSocket();
};

const socketResult = getSocketHook() as any;
const {
  connect, disconnect, connected,
  sendEmojiRequest, sendHintRequest, sendLeaveRequest, sendVoteRequest, sendReadyRequest,
  ydoc
} = socketResult;

// AI 모드 전용 함수는 안전하게 추출
const sendAIRequest = (socketResult as any).sendAIRequest;

// 모드 설정 (공통 스토어 사용)
matchStore.setMode(props.mode as any);
if (props.mode === 'CONTEST') {
  matchStore.gameInfo.mode = 'CONTEST';
}

// Emoji Panel
// ----------
const showEmojiPanel = ref(false);
const aiReadySent = ref(false);

const normalizeText = (value?: string | null) => {
  if (!value) return '';
  return value
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\t/g, '\t');
};

const normalizedProblemContent = computed(() => normalizeText(problemContent.value));
const normalizedInputOutput = computed(() => normalizeText(matchStore.gameInfo.problemInfo?.inputOutput));

const ensureAiUserList = () => {
  if (props.mode !== 'AI') return;

  const currentUsers = matchStore.userList ?? [];
  const nextUsers = [...currentUsers];

  const hasAiBot = nextUsers.some((user) => String(user.userId).toUpperCase() === 'AI_BOT');
  if (!hasAiBot) {
    nextUsers.push({
      userId: 'AI_BOT',
      nickname: 'AI Bot',
      remainingHp: 100,
      profileImageUrl: ''
    });
  }

  const me = userStore.user;
  if (me) {
    const hasMe = nextUsers.some((user) => String(user.userId) === String(me.userId));
    if (!hasMe) {
      nextUsers.unshift({
        userId: String(me.userId),
        nickname: me.nickname,
        remainingHp: 100,
        profileImageUrl: me.profileImageUrl
      });
    }
  }

  if (nextUsers.length !== currentUsers.length) {
    matchStore.setUserList(nextUsers);
  }
};

// Hint Modal
// ----------
const hintModalOpen = ref(false);
const currentHintSeq = ref<number | null>(null);
const hintModalTitle = computed(() => {
  if (currentHintSeq.value === 0) return 'AI 힌트';
  return currentHintSeq.value ? `힌트 ${currentHintSeq.value}` : '힌트';
});
const hintModalContent = computed(() => {
  if (currentHintSeq.value === null || currentHintSeq.value === undefined) {
    return '힌트가 없습니다.';
  }
  const hintContents = matchStore.hintContents ?? {};
  const content = hintContents[currentHintSeq.value];
  return content ?? '힌트 내용이 아직 준비되지 않았습니다.';
});

const openHintModal = (sequence: number) => {
  currentHintSeq.value = sequence;
  hintModalOpen.value = true;
};

const handleHintButton = (sequence: number) => {
  const hintContents = matchStore.hintContents ?? {};
  const cached = hintContents[sequence];
  if (cached !== undefined) {
    openHintModal(sequence);
    return;
  }

  sendHintRequest(matchStore.gameInfo.matchId, userStore.user?.userId.toString()!, sequence);
};

const handleAiReady = () => {
  if (aiReadySent.value) return;
  if (!matchStore.gameInfo.matchId) {
    message.error('매치 정보가 없습니다.');
    return;
  }
  aiReadySent.value = true;
  sendReadyRequest(matchStore.gameInfo.matchId);
};

const sendEmojiFromPanel = (emoji: string) => {
  if (props.mode === 'AI') return;
  sendEmojiRequest(matchStore.gameInfo.matchId, emoji);
  showEmojiPanel.value = false;
};

watch(() => matchStore.lastHintSequence, (seq) => {
  if (seq !== null) {
    openHintModal(seq);
  }
});

onMounted(() => {
  // matchId
  const matchId = route.params.matchId as string;
  if (!matchId || matchId.length === 0) {
    router.push({ name: 'main' });
    return;
  }

  matchStore.setMode(props.mode as any);
  if (props.mode === 'CONTEST') {
    matchStore.gameInfo.mode = 'CONTEST';
    matchStore.loadProgrammingLanguageFromStorage(); // 저장된 언어 정보 복원
  }

  ensureAiUserList();

  // WebSocket 연결
  if (!connected.value) {
    if (props.mode === 'CONTEST') {
      // CONTEST 모드는 userId가 추가로 필요함
      const userId = userStore.user?.userId;
      if (userId) {
        (connect as (m: string, u: string, t: string) => void)(matchId, String(userId), userStore.accessToken);
      }
    } else {
      connect(matchId, userStore.accessToken);
    }
  }

  // if (props.voice) {
  //   connectVoice(matchId);
  // }
});

watch(
  () => [userStore.user?.userId, matchStore.userList.map((user) => String(user.userId)).join('|')],
  () => {
    ensureAiUserList();
  },
  { immediate: true }
);

onUnmounted(() => {
  if (connected.value) {
    // LEAVE 메시지 전송 후 연결 해제
    sendLeaveRequest(matchStore.gameInfo.matchId);
    // 약간의 지연 후 연결 해제 (메시지가 도착할 시간 제공)
    setTimeout(() => {
      disconnect();
    }, 100);
  }

  // disconnectVoice();
});

// ydoc
// ----------
provide('ydoc', ydoc);

// Translated
// ----------
const problemContent = ref('');
const isTranslated = ref(false);
const handleTranslate = () => {
  if (matchStore.gameInfo.problemInfo?.translatedProblem?.length) {
    // 번역문이 있는 경우 토글
    isTranslated.value = !isTranslated.value;
    problemContent.value = isTranslated.value
      ? matchStore.gameInfo.problemInfo.translatedProblem
      : matchStore.gameInfo.problemInfo?.question || '';
  } else {
    // 번역문이 없는 경우 원문 유지
    problemContent.value = matchStore.gameInfo.problemInfo?.question || '';
  }
};

// 문제 정보 변경 시 problemContent 업데이트
watch(() => matchStore.gameInfo.problemInfo?.question, (newVal) => {
  if (newVal) {
    isTranslated.value = false; // 새 문제가 로드되면 원문으로 초기화
    problemContent.value = newVal;
  }
});

// Memo & Feedback
// ----------
const memo = ref('');
const feedback = ref('');
/** 채점 실패 시 AI_HINT(sequence 0)가 피드백 영역에 표시됨 */
const feedbackDisplay = computed(() => (matchStore.hintContents?.[0] ?? feedback.value) || '피드백이 없습니다');

// Audio
// ----------
// const localAudioEnabled = ref(true);
// const desiredLocalAudioState = ref(true); // 사용자가 선택한 상태를 유지
// const voiceMutedUsers = ref<Record<string, boolean>>({});
// const ov = ref<OpenVidu | null>(null);
// const ovSession = ref<Session | null>(null);
// const publisher = ref<Publisher | null>(null);
// const subscribers = ref<Record<string, Subscriber>>({});
// const audioContainer = ref<HTMLDivElement | null>(null);
// const subscriberAudioElements = ref<Record<string, HTMLVideoElement>>({});
// const userIdToConnectionId = ref<Record<string, string>>({});
// const subscribersByConnectionId = ref<Record<string, Subscriber>>({});

// const parseConnectionUserId = (data?: string | null) => {
//   if (!data) return null;

//   try {
//     // data는 문자열이므로 먼저 파싱
//     let parsed = JSON.parse(data);

//     // clientData 필드가 있으면 그것도 파싱
//     if (parsed.clientData && typeof parsed.clientData === 'string') {
//       parsed = JSON.parse(parsed.clientData);
//     }

//     return parsed?.userId ? String(parsed.userId) : null;
//   } catch (err) {
//     console.warn('[Voice] Failed to parse connectionData:', data, err);
//     return null;
//   }
// };

// const connectVoice = async (matchId: string) => {
//   try {
//     if (!props.voice || ovSession.value) return;

//     ov.value = new OpenVidu();
//     ovSession.value = ov.value.initSession();

//     ovSession.value.on('streamCreated', (event) => {
//       const subscriber = ovSession.value!.subscribe(event.stream, undefined);
//       const connectionData = event.stream.connection?.data;
//       const userId = parseConnectionUserId(connectionData) || event.stream.connection?.connectionId;
//       const connectionId = event.stream.connection?.connectionId;

//       const audioEl = document.createElement('video');
//       audioEl.autoplay = true;
//       audioEl.setAttribute('playsinline', '');
//       audioEl.muted = false;
//       audioEl.style.display = 'none';
//       if (audioContainer.value) {
//         audioContainer.value.appendChild(audioEl);
//       }
//       subscriber.addVideoElement(audioEl);

//       if (userId) {
//         subscribers.value = {
//           ...subscribers.value,
//           [userId]: subscriber
//         };

//         subscriberAudioElements.value = {
//           ...subscriberAudioElements.value,
//           [userId]: audioEl
//         };

//         const muted = voiceMutedUsers.value[userId];
//         if (muted) {
//           subscriber.subscribeToAudio(false);
//         }
//       }

//       if (userId && connectionId) {
//         userIdToConnectionId.value = {
//           ...userIdToConnectionId.value,
//           [userId]: connectionId
//         };
//       }

//       if (connectionId) {
//         subscribersByConnectionId.value = {
//           ...subscribersByConnectionId.value,
//           [connectionId]: subscriber
//         };
//       }
//     });

//     ovSession.value.on('streamDestroyed', (event) => {
//       const connectionData = event.stream.connection?.data;
//       const userId = parseConnectionUserId(connectionData) || event.stream.connection?.connectionId;
//       const connectionId = event.stream.connection?.connectionId;
//       if (userId) {
//         const next = { ...subscribers.value };
//         delete next[userId];
//         subscribers.value = next;

//         const audioEl = subscriberAudioElements.value[userId];
//         if (audioEl?.parentElement) {
//           audioEl.parentElement.removeChild(audioEl);
//         }
//         const nextAudio = { ...subscriberAudioElements.value };
//         delete nextAudio[userId];
//         subscriberAudioElements.value = nextAudio;
//       }

//       if (connectionId) {
//         const nextConnMap = { ...subscribersByConnectionId.value };
//         delete nextConnMap[connectionId];
//         subscribersByConnectionId.value = nextConnMap;

//         const matchUserId = Object.keys(userIdToConnectionId.value).find(
//           (key) => userIdToConnectionId.value[key] === connectionId
//         );
//         if (matchUserId) {
//           const nextUserConn = { ...userIdToConnectionId.value };
//           delete nextUserConn[matchUserId];
//           userIdToConnectionId.value = nextUserConn;
//         }
//       }
//     });

//     const tokenResp = await getMatchOpenViduToken(matchId);
//     const token = tokenResp?.data?.token;
//     const sessionId = tokenResp?.data?.sessionId;
//     if (!token || !sessionId) {
//       message.error('음성 채팅 토큰을 가져오지 못했습니다.');
//       return;
//     }

//     await ovSession.value.connect(token, {
//       clientData: JSON.stringify({
//         userId: userStore.user?.userId,
//         nickname: userStore.user?.nickname
//       })
//     });

//     const pub = ov.value.initPublisher(undefined, {
//       audioSource: undefined,
//       videoSource: false,
//       publishAudio: desiredLocalAudioState.value,
//       publishVideo: false,
//       mirror: false
//     });

//     await ovSession.value.publish(pub);
//     publisher.value = pub;
//     localAudioEnabled.value = desiredLocalAudioState.value;
//   } catch (err) {
//     console.error('[Voice] connect failed:', err);
//     message.error('음성 채팅 연결에 실패했습니다.');
//   }
// };

// const disconnectVoice = () => {
//   try {
//     ovSession.value?.disconnect();
//   } finally {
//     ovSession.value = null;
//     ov.value = null;
//     publisher.value = null;
//     subscribers.value = {};
//     subscriberAudioElements.value = {};
//     userIdToConnectionId.value = {};
//     subscribersByConnectionId.value = {};
//   }
// };

// const toggleLocalAudio = () => {
//   desiredLocalAudioState.value = !desiredLocalAudioState.value;
//   localAudioEnabled.value = desiredLocalAudioState.value;
//   if (publisher.value) {
//     publisher.value.publishAudio(desiredLocalAudioState.value);
//   }
// };

// const toggleVoiceMute = (userId: string) => {
//   const userId_str = String(userId);
//   const isMuted = !voiceMutedUsers.value[userId_str];
//   voiceMutedUsers.value[userId_str] = isMuted;

//   const subscriber = subscribers.value[userId_str];
//   if (subscriber) {
//     subscriber.subscribeToAudio(!isMuted);
//     return;
//   }

//   const connectionId = userIdToConnectionId.value[userId_str];
//   if (connectionId && subscribersByConnectionId.value[connectionId]) {
//     subscribersByConnectionId.value[connectionId].subscribeToAudio(!isMuted);
//   }
// };

// Emoji
// ----------
const showEmojiPicker = async () => {
  showEmojiPanel.value = !showEmojiPanel.value;
  console.log('[Emoji] showEmojiPanel.value:', showEmojiPanel.value);
};

// Timer
// ----------
let designTimerInterval: ReturnType<typeof setInterval> | null = null;

const startDesignTimer = () => {
  stopDesignTimer();

  designTimerInterval = setInterval(() => {
    if (matchStore.gameInfo.designTimePerSec > 0) {
      matchStore.gameInfo.designTimePerSec -= 1;
    } else {
      stopDesignTimer();
    }
  }, 1000);
};

const stopDesignTimer = () => {
  if (designTimerInterval !== null) {
    clearInterval(designTimerInterval);
    designTimerInterval = null;
  }
};

watch(() => matchStore.gameInfo.phaseType, (phaseType) => {
  if (phaseType === 'DESIGN') {
    if (props.mode === 'AI') {
      matchStore.gameInfo.designTimePerSec = 0;
      stopDesignTimer();
      return;
    }
    startDesignTimer();
    return;
  }

  stopDesignTimer();
}, { immediate: true });

watch(() => matchStore.lastJudgeResult, (result) => {
  if (!result) return;
  if (result.success) {
    message.success(result.message || '정답입니다!');
  } else {
    message.error(result.message || '오답입니다.');
  }
}, { immediate: true });

watch(() => matchStore.lastGameEndMessage, (msg) => {
  if (msg) {
    message.info(msg);
  }
}, { immediate: true });

const formattingTime = computed(() => {
  const value = matchStore.gameInfo.phaseType === 'DESIGN' ? matchStore.gameInfo.designTimePerSec : matchStore.gameInfo.remainingTimePerSec;
  const mins = Math.floor(value / 60);
  const secs = value % 60;
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
});

// Turn indicators
// ----------
const isMyTurn = computed(() => String(matchStore.gameInfo.currentTurnUserId) === String(userStore.user?.userId));
const isPlaying = computed(() => matchStore.gameInfo.phaseType === 'PLAYING');
const isDesign = computed(() => matchStore.gameInfo.phaseType === 'DESIGN');

const turnStateLabel = computed(() => {
  if (!isPlaying.value) return '설계 시간 진행 중';
  return isMyTurn.value ? '내 턴 진행 중' : '상대 턴 진행 중';
});

const turnStatePillClass = computed(() => {
  if (!isPlaying.value) return 'bg-sky-500/20 text-sky-300 border-sky-400/40';
  return isMyTurn.value
    ? 'bg-emerald-500/25 text-emerald-300 border-emerald-400/40'
    : 'bg-indigo-500/20 text-indigo-300 border-indigo-400/40';
});

const turnStateDotClass = computed(() => {
  if (!isPlaying.value) return 'bg-sky-300';
  return isMyTurn.value ? 'bg-emerald-300' : 'bg-indigo-300';
});

const timerWrapClass = computed(() => {
  if (!isPlaying.value) return 'bg-slate-700/20 border-slate-500/60 shadow-[0_0_16px_rgba(56,189,248,0.25)]';
  return isMyTurn.value
    ? 'bg-emerald-500/10 border-emerald-400/60 shadow-[0_0_20px_rgba(52,211,153,0.35)]'
    : 'bg-indigo-500/10 border-indigo-400/60 shadow-[0_0_18px_rgba(129,140,248,0.35)]';
});

const timerIconClass = computed(() => {
  if (!isPlaying.value) return 'text-sky-300';
  return isMyTurn.value ? 'text-emerald-300' : 'text-indigo-300';
});

const timerClass = computed(() => {
  if (!isPlaying.value) return '';
  return matchStore.gameInfo.remainingTimePerSec <= 10 ? 'text-red-400 pulse' : '';
});

const maxTurnTime = ref<number>(0);
const lastTurnKey = ref<string>('');
watch([() => matchStore.gameInfo.currentTurnUserId, () => matchStore.gameInfo.remainingTimePerSec], () => {
  const key = `${matchStore.gameInfo.currentTurnUserId}`;
  if (lastTurnKey.value !== key) {
    lastTurnKey.value = key;
    maxTurnTime.value = matchStore.gameInfo.remainingTimePerSec || maxTurnTime.value || 0;
  }

  if (isPlaying.value && matchStore.gameInfo.remainingTimePerSec > maxTurnTime.value) {
    maxTurnTime.value = matchStore.gameInfo.remainingTimePerSec;
  }
});

const maxDesignTime = ref<number>(0);
watch([() => matchStore.gameInfo.phaseType, () => matchStore.gameInfo.designTimePerSec], () => {
  if (isDesign.value) {
    if (maxDesignTime.value === 0 || matchStore.gameInfo.designTimePerSec > maxDesignTime.value) {
      maxDesignTime.value = matchStore.gameInfo.designTimePerSec;
    }
  }
});

const turnProgress = computed(() => {
  if (!isPlaying.value || maxTurnTime.value <= 0) return 0;
  const percent = Math.max(0, Math.min(100, (matchStore.gameInfo.remainingTimePerSec / maxTurnTime.value) * 100));
  return Number(percent.toFixed(1));
});

const designProgress = computed(() => {
  if (!isDesign.value || maxDesignTime.value <= 0) return 0;
  const percent = Math.max(0, Math.min(100, (matchStore.gameInfo.designTimePerSec / maxDesignTime.value) * 100));
  return Number(percent.toFixed(1));
});

const turnProgressColor = computed(() => {
  if (!isPlaying.value) return '#64748b';
  if (matchStore.gameInfo.remainingTimePerSec <= 5) return '#ef4444';
  if (matchStore.gameInfo.remainingTimePerSec <= 10) return '#f59e0b';
  return '#22c55e';
});

const designProgressColor = computed(() => {
  if (!isDesign.value) return '#64748b';
  if (matchStore.gameInfo.designTimePerSec <= 5) return '#ef4444';
  if (matchStore.gameInfo.designTimePerSec <= 10) return '#f59e0b';
  return '#38bdf8';
});

const progressLabel = computed(() => (isDesign.value ? '설계 진행률' : '턴 진행률'));
const progressPercent = computed(() => (isDesign.value ? designProgress.value : turnProgress.value));
const progressColor = computed(() => (isDesign.value ? designProgressColor.value : turnProgressColor.value));

const alertedKey = ref<string>('');
const alertedThresholds = ref<Set<number>>(new Set());
watch([() => matchStore.gameInfo.currentTurnUserId, () => matchStore.gameInfo.remainingTimePerSec], () => {
  if (!isPlaying.value) return;

  const key = `${matchStore.gameInfo.currentTurnUserId}`;
  if (alertedKey.value !== key) {
    alertedKey.value = key;
    alertedThresholds.value = new Set();
  }

  const remaining = matchStore.gameInfo.remainingTimePerSec;
  if (isMyTurn.value && (remaining === 10 || remaining === 5)) {
    if (!alertedThresholds.value.has(remaining)) {
      alertedThresholds.value.add(remaining);
      message.warning(`남은 시간 ${remaining}초`);
    }
  }
});

// AI Code Playback
// ----------
let aiPlaybackInterval: ReturnType<typeof setInterval> | null = null;
let aiPlaybackId: number | null = null;

const stopAiPlayback = () => {
  if (aiPlaybackInterval) {
    clearInterval(aiPlaybackInterval);
    aiPlaybackInterval = null;
  }
  // aiPlaybackId = null; // ID는 리셋하지 않음 (중복 호출 방지용)
};

const applyCodeToYDoc = (code: string, append = false) => {
  const doc = ydoc.value;
  if (!doc) return;
  const ytext = doc.getText('code');

  if (append) {
    ytext.insert(ytext.length, code);
  } else {
    ytext.delete(0, ytext.length);
    if (code.length > 0) {
      ytext.insert(0, code);
    }
  }
};

const startAiPlayback = (targetCode: string, durationSec?: number) => {
  const doc = ydoc.value;
  if (!doc) return;

  // 현재 에디터에 있는 코드
  const currentCode = doc.getText('code').toString();
  const normalizedTarget = normalizeText(targetCode);

  // 1. 이미 내용이 똑같으면 중단
  if (currentCode === normalizedTarget) {
    return;
  }

  // 2. 만약 새로운 코드가 현재 코드의 "연장선"이라면 (뒤에만 추가된 경우) -> 이어서 타이핑
  if (normalizedTarget.startsWith(currentCode)) {
    stopAiPlayback(); // 기존 타이핑 중단하고 새로운 목표로 재설정

    const addedPart = normalizedTarget.slice(currentCode.length);
    if (!addedPart) return;

    // 남은 시간 동안 추가된 부분만 타이핑
    const totalSeconds = Math.max(1, Math.round(durationSec ?? matchStore.gameInfo.remainingTimePerSec ?? 1));

    // 토큰(단어) 단위로 쪼개기
    const tokenRegex = /[A-Za-z_]+|\d+|\S|\s+/g;
    const tokens: string[] = [];
    let match: RegExpExecArray | null;
    while ((match = tokenRegex.exec(addedPart)) !== null) {
      tokens.push(match[0]);
    }

    if (tokens.length === 0) {
      applyCodeToYDoc(addedPart, true);
      return;
    }

    // 시간 계산 (너무 느리지 않게 최소 속도 보장)
    const tokensPerStep = Math.max(1, Math.ceil(tokens.length / (totalSeconds * 10))); // 0.1초 단위 업데이트 가정
    let currentTokenIndex = 0;

    aiPlaybackInterval = setInterval(() => {
      // 한 번에 여러 토큰 추가
      let chunk = '';
      for (let i = 0; i < tokensPerStep; i++) {
        if (currentTokenIndex < tokens.length) {
          chunk += tokens[currentTokenIndex];
          currentTokenIndex++;
        }
      }

      if (chunk) {
        applyCodeToYDoc(chunk, true);
      }

      // 종료 조건
      if (currentTokenIndex >= tokens.length) {
        stopAiPlayback();
        // 혹시 모를 오차 보정 (최종 결과물로 덮어쓰기 보다는, 길이 체크 후 부족하면 채우기)
        const finalCurrent = doc.getText('code').toString();
        if (finalCurrent.length < normalizedTarget.length) {
          applyCodeToYDoc(normalizedTarget.slice(finalCurrent.length), true);
        }
      }
    }, 100); // 100ms 마다 업데이트

  } else {
    // 3. 완전히 다른 코드라면 (초기화 or 수정) -> 즉시 덮어쓰기
    // AI가 코드를 "수정"하는 경우는 거의 없고 보통 append만 하므로, 
    // 여기서는 즉시 반영하여 꼬임을 방지함.
    console.log('[AI Playback] Code mismatch/reset. Applying instanly.');
    stopAiPlayback();
    applyCodeToYDoc(normalizedTarget, false);
  }
};

watch(() => matchStore.aiCodePlayback, (payload) => {
  if (!payload || props.mode !== 'AI') return;
  aiPlaybackId = payload.id;
  startAiPlayback(payload.code, payload.durationSec);
});

watch(() => matchStore.aiReport, (report) => {
  if (!report) return;
  if (report.matchId && report.matchId !== matchStore.gameInfo.matchId) return;
  const matchId = report.matchId || matchStore.gameInfo.matchId;
  if (matchId) router.push({ name: 'aiReport', params: { matchId } });
});

// Leave
// ----------
const handleLeave = async () => {
  const confirmed = confirm('정말 나가시겠습니까?');
  if (confirmed) {
    matchStore.clearProgrammingLanguage(); // 나갈 때 언어 설정 초기화
    sendLeaveRequest(matchStore.gameInfo.matchId);
    disconnect();
    // disconnectVoice();
    router.push({ name: 'main' });
  }
};

// Submit
// ----------
const handleSubmit = async () => {
  if (matchStore.waitingForAiReport) {
    message.warning('AI 리포트를 기다리는 중입니다.');
    return;
  }
  if (matchStore.gameInfo.phaseType !== 'PLAYING') {
    message.warning('지금은 제출할 수 없습니다.');
    return;
  }

  const myUserId = userStore.user?.userId;
  if (String(matchStore.gameInfo.currentTurnUserId) !== String(myUserId)) {
    message.warning('현재는 제출할 수 있는 차례가 아닙니다.');
    return;
  }

  const code = ydoc.value?.getText('code').toString() ?? '';
  if (!code.trim()) {
    message.warning('제출할 코드가 없습니다.');
    return;
  }

  matchStore.setLastSubmittedCode(code);

  const rawLanguage = matchStore.gameInfo.programmingLanguage ?? 'python';
  const language = rawLanguage.toString().toUpperCase();
  if (!matchStore.gameInfo.matchId) {
    message.error('매치 정보가 없습니다.');
    return;
  }

  try {
    const resp = await submitCode(matchStore.gameInfo.matchId, {
      sourceCode: code,
      language
    });
    if (resp.statusCode === 200 && resp.data) {
      message.success('코드가 제출되었습니다!');
    } else {
      message.warning(resp.message || '제출 요청 실패');
    }
  } catch (err: any) {
    const serverMessage = err?.response?.data?.message;
    const fallback = '제출 중 오류가 발생했습니다.';
    message.error(serverMessage || fallback, { duration: 8000 });
  }

  // alert('코드가 제출되었습니다');
};

</script>

<style scoped>
.floating-emoji {
  z-index: 50;
  animation: float 4s ease-in-out forwards;
  will-change: transform, opacity;
}

@keyframes float {
  0% {
    opacity: 1;
    transform: translateY(0px) scale(1);
  }

  25% {
    opacity: 1;
  }

  75% {
    opacity: 0.6;
  }

  100% {
    opacity: 0;
    transform: translateY(-12vh) scale(0.2);
  }
}

.pulse {
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0% {
    opacity: 1;
  }

  50% {
    opacity: 0.55;
  }

  100% {
    opacity: 1;
  }
}
</style>