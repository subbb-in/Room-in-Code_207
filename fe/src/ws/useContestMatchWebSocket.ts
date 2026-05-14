import * as Y from 'yjs';
import { onUnmounted, ref, onBeforeUnmount } from "vue";
import { useRouter } from 'vue-router';
import { useMatchStore } from '@/stores/matchStore';
import type { PhaseResponse } from '@/types/ws/match/response/PhaseResponse';
import type { PhaseType, ProblemInfo } from '@/types/ws/match/MatchGameInfo';
import type { TimerResponse } from '@/types/ws/match/response/TimerResponse';
import type { HPUpdateResponse } from '@/types/ws/match/response/HPUpdateResponse';
import type { HintResponse } from '@/types/ws/match/response/HintResponse';
import { useUserStore } from '@/stores/userStore';
import type { CodeUpdateResponse } from '@/types/ws/match/response/CodeUpdateResponse';
import type { EmojiResponse } from '@/types/ws/match/response/EmojiResponse';
import { getUserInfo } from '@/api/userService';
import { HttpStatusCode } from 'axios';
import { getWebSocketBase } from '@/ws/wsBase';
import type { MatchUserInfo } from '@/types/ws/match/MatchUserInfo';


export const useContestMatchWebSocket = () => {
  const logPrefix = '[Match WS]';
  const yjsPrefix = '[Match WS: Yjs]';
  const uint8ToBase64 = (bytes: Uint8Array) => {
    let binary = '';
    const chunkSize = 0x8000;
    for (let i = 0; i < bytes.length; i += chunkSize) {
      binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
    }
    return btoa(binary);
  };

  const base64ToUint8 = (base64: string) => {
    const binary = atob(base64);
    const len = binary.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i += 1) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
  };
  // WebSocket
  const ws = ref<WebSocket | null>(null);
  const connected = ref(false);

  // YJS
  const ydoc = ref<Y.Doc | null>(null);

  // Etc
  const userStore = useUserStore();
  const matchStore = useMatchStore();
  const router = useRouter();

  const setupYDoc = () => {
    if (ydoc.value) return ydoc.value;

    const doc = new Y.Doc();
    doc.getText('code');

    doc.on('update', (update: Uint8Array, origin: any) => {
      if (origin !== 'remote' && ws.value?.readyState === WebSocket.OPEN) {
        ws.value.send(update);
        if (matchStore.gameInfo.matchId) {
          const fallback = {
            type: 'CODE_UPDATE',
            matchId: matchStore.gameInfo.matchId,
            updateBase64: uint8ToBase64(update)
          };
          ws.value.send(JSON.stringify(fallback));
        }
      }
    });

    ydoc.value = doc;
    return doc;
  };

  // 소켓 연결
  const connect = (matchId: string, userId: string, token: string) => {
    try {
      const wsBase = getWebSocketBase();
      if (!wsBase) {
        console.warn('[Match WS] WebSocket 주소를 찾을 수 없습니다.');
        return;
      }
      const wsUrl = `${wsBase}/ws/match?matchId=${matchId}&userId=${userId}&token=${token}`;
      console.info(`${logPrefix} 연결 시도:`, wsUrl);
      ws.value = new WebSocket(wsUrl);
      ws.value.binaryType = 'arraybuffer';

      // 연결 성공
      ws.value.onopen = () => {
        console.info(`${logPrefix} 연결 성공`, {
          readyState: ws.value?.readyState,
          matchId,
          userId
        });
        connected.value = true;

        // 매치 ID 저장
        matchStore.gameInfo.matchId = matchId;

        // JOIN 요청 전송
        sendMessage('JOIN', matchId);

        // 문제 정보 요청
        sendMessage('PROBLEM_REQUEST', matchId);

        // ----------

        // Yjs 초기화
        setupYDoc();
        if (ydoc.value) {
          console.log(`${yjsPrefix} 초기화 성공`, {
            clientId: ydoc.value.clientID,
            hasCode: ydoc.value.share.has('code')
          });
        }

        // 동기화 요청
        const initState = ydoc.value ? Y.encodeStateAsUpdate(ydoc.value) : new Uint8Array();
        if (ws.value?.readyState === WebSocket.OPEN && initState.length) {
          console.log(`${yjsPrefix} 초기 상태 전송`, { byteLength: initState.length });
          ws.value.send(initState);
        }
      };

      // 연결 해제
      ws.value.onclose = (event) => {
        if (event.code === 1000 || event.code === 1001) {
          // 정상적인 종료
          console.info(`${logPrefix} 연결 종료`, {
            code: event.code,
            reason: event.reason
          });
        } else {
          console.warn('[WebSocket 연결 실패] ', event);
        }

        connected.value = false;
      };

      // 연결 오류
      ws.value.onerror = (err) => {
        console.error(`${logPrefix} 오류: `, err);
        connected.value = false;
      };

      // 메시지 처리
      ws.value.onmessage = async (event) => {
        // Yjs 동기화
        if (event.data instanceof ArrayBuffer) {
          console.info('[Match WS Yjs] 바이너리 데이터 수신: ', {
            byteLength: event.data.byteLength
          });

          if (!ydoc.value) {
            setupYDoc();
          }

          if (ydoc.value) {
            try {
              if (!ydoc.value.share.has('code')) {
                ydoc.value.getText('code');
              }
              Y.applyUpdate(ydoc.value, new Uint8Array(event.data), 'remote');
              const ytext = ydoc.value.getText('code');
              console.info('[Match WS Yjs] 갱신 적용 성공', {
                textLength: ytext.length
              });
            } catch (err) {
              console.error('[Match WS Yjs] 갱신 적용 실패: ', err);
              setupYDoc();
            }
          }
          return;
        }

        // Blob도 바이너리로 처리
        if (event.data instanceof Blob) {
          console.info('[Match WS Yjs] 바이너리 데이터 수신(Blob): ', {
            size: event.data.size,
            type: event.data.type
          });

          if (!ydoc.value) {
            setupYDoc();
          }

          if (ydoc.value) {
            const arrayBuffer = await event.data.arrayBuffer();
            try {
              // 구조 검증 - 'code' 텍스트가 없으면 사전에 생성
              if (!ydoc.value.share.has('code')) {
                ydoc.value.getText('code');
              }

              Y.applyUpdate(ydoc.value, new Uint8Array(arrayBuffer), 'remote');
              const ytext = ydoc.value.getText('code');
              console.info('[Match WS Yjs] 갱신 적용 성공', {
                textLength: ytext.length
              });
            } catch (err) {
              console.error('[Match WS Yjs] 갱신 적용 실패: ', err);
            }
          }
          return;
        }

        // 일반 메시지
        try {
          console.info(`${logPrefix} 텍스트 메시지 수신`, {
            length: typeof event.data === 'string' ? event.data.length : undefined
          });
          const data = JSON.parse(event.data);
          handleMessage(data);
        } catch (err) {
          console.error('[Match WS 메시지 Error: ', err);
        }
      };
    } catch (err) {
      console.warn('[Match WS] 연결 오류: ', err);
    }
  };

  const disconnect = () => {
    console.info(`${logPrefix} 연결 해제 요청`);
    if (ydoc.value) {
      console.info(`${yjsPrefix} destroy`, {
        clientId: ydoc.value.clientID
      });
      ydoc.value.destroy();
      ydoc.value = null;
    }
    if (ws.value) {
      ws.value.close();
      ws.value = null;
    }
    connected.value = false;
    matchStore.resetStore();
  };

  const sendMessage = (type: string, matchId: string) => {
    if (type.length && matchId.length && ws.value?.readyState === WebSocket.OPEN) {
      const message = {
        type: type,
        matchId
      };
      console.info('[Match WS] 메시지 전송: ', message);
      ws.value.send(JSON.stringify(message));
      return;
    }

    console.warn('[Match WS] 메시지 전송 실패: ', ws.value?.readyState);
  };

  const handleMessage = async (data: any) => {
    console.info('[Match WS] 메시지 수신: ', data);

    const safeJsonParse = (value: unknown) => {
      if (typeof value !== 'string') return value as Record<string, unknown> | null;
      try {
        return JSON.parse(value) as Record<string, unknown>;
      } catch {
        return null;
      }
    };

    switch (data.type) {
      case 'PARTICIPANTS_UPDATE': {
        const resp = data;
        if (resp?.userIds && Array.isArray(resp.userIds)) {
          try {
            const users: MatchUserInfo[] = [];

            for (const userId of resp.userIds) {
              const resp2 = await getUserInfo(Number(userId));
              if (resp2.statusCode === HttpStatusCode.Ok) {
                const user: MatchUserInfo = {
                  nickname: resp2.data.nickname,
                  remainingHp: 100,
                  userId: userId,
                  profileImageUrl: resp2.data.profileImageUrl
                };
                users.push(user);
              }
            }
            matchStore.setUserList(users);
            console.log('[Match WS] 참가자 목록 업데이트:', users);
          } catch (err: any) {
            console.error('[Match WS] PARTICIPANTS_UPDATE 오류: ', err);
          }
        }
      } break;

      case 'PHASE_CHANGED': {
        const resp = data as PhaseResponse;
        if (resp) {
          // 설계 시간
          matchStore.gameInfo.designTimePerSec = resp.duration!;

          // 페이즈
          matchStore.gameInfo.phaseType = resp.phase as PhaseType;

          if (resp.currentTurn !== undefined && resp.currentTurn !== null) {
            matchStore.gameInfo.currentTurnUserId = resp.currentTurn;
          }

          // 프로그래밍 언어 정보 설정 (서버에서 전달되는 경우)
          const language = data?.language ?? data?.programmingLanguage;
          if (language) {
            console.log('[Match WS] PHASE_CHANGED - 언어 설정:', language);
            matchStore.setProgrammingLanguage(language);
          }

          // 페이지 이동 (사용자 요청)
          const matchId = matchStore.gameInfo.matchId;
          if (matchId) {
            router.replace({
              name: 'contest',
              params: { matchId }
            });
          }
        }
      } break;

      case 'TIMER': {
        const resp = data as TimerResponse;
        if (resp) {
          // 남은 시간 (초 단위)
          matchStore.gameInfo.remainingTimePerSec = resp.remainingTime;

          // 현재 턴의 사용자 ID
          matchStore.gameInfo.currentTurnUserId = resp.currentUserId;
        }
      } break;

      case 'HP_UPDATE': {
        const resp = data as HPUpdateResponse;
        if (resp) {
          matchStore.setHP(resp.userId, resp.remainingHp);
        }
      } break;

      // 어? 근데 이거 누군가 프론트 수정하면
      // 누구나 보게 될 거 같은데? 서버에서 검증 필요할 듯
      case 'HINT_RESPONSE': {
        console.log('힌트 응답');
        const resp = data as HintResponse;
        if (resp) {
          console.log(resp);
          // 현재 userId와 응답의 userId와 비교
          if (userStore.user?.userId.toString() === resp.userId) {
            matchStore.setHP(resp.userId, resp.remainingHp);
            if (typeof matchStore.setHintContent === 'function') {
              matchStore.setHintContent(resp.sequence, resp.content ?? null);
            }
          }
        }
      } break;

      case 'HINT_ERROR': {
        console.warn('[Match WS] HINT_ERROR:', data?.message);
      } break;

      case 'AI_HINT': {
        const hintPayload = data?.data?.aiHint
          ?? data?.data?.ai_hint
          ?? data?.data?.hint
          ?? data?.aiHint
          ?? data?.ai_hint
          ?? data?.hint;
        let hintText = '';

        if (typeof hintPayload === 'string') {
          hintText = hintPayload;
        } else if (hintPayload && typeof hintPayload === 'object') {
          const hintObj = hintPayload as Record<string, unknown>;
          const preferred = hintObj.comment ?? hintObj.hint ?? hintObj.message ?? hintObj.content;
          hintText = typeof preferred === 'string' ? preferred : JSON.stringify(hintObj, null, 2);
        }

        if (hintText) {
          matchStore.setHintContent(0, hintText);
        } else {
          console.warn('[Match WS] AI_HINT payload empty:', data);
        }
      } break;

      // Yjs 동기화 중이라 사용 안 해도 됨
      case 'CODE_UPDATE': {
        const resp = data as CodeUpdateResponse & { updateBase64?: string };
        if (resp?.updateBase64 && ydoc.value) {
          try {
            const update = base64ToUint8(resp.updateBase64);
            Y.applyUpdate(ydoc.value, update, 'remote');
          } catch (err) {
            console.error('[Match WS Yjs] CODE_UPDATE 적용 실패:', err);
          }
        }
      } break;

      // 모든 사용자에게 이모지 출력
      case 'EMOJI': {
        const resp = data as EmojiResponse;
        if (resp?.emoji) {
          // 거품 이모지 추가
          console.log(`[Match WS] 이모지 발송: ${resp.emoji}`);
          matchStore.addFloatingEmoji(resp.emoji);
          console.log(`[Match WS] floatingEmojis 길이:`, matchStore.floatingEmojis.length);
        }
      } break;

      // 문제 정보
      case 'PROBLEM_RESPONSE': {
        const resp = data?.problem as ProblemInfo;
        if (resp) {
          matchStore.gameInfo.problemInfo = resp;
          const translated = data?.translatedProblem ?? resp?.translatedProblem;
          if (translated) {
            matchStore.gameInfo.problemInfo.translatedProblem = translated;
          }
        }

        // 프로그래밍 언어 정보 설정
        const language = data?.language ?? data?.programmingLanguage;
        if (language) {
          console.log('[Match WS] PROBLEM_RESPONSE - 언어 설정:', language);
          matchStore.setProgrammingLanguage(language);
        }
      } break;

      case 'VOTING_START': {
        console.log('[Match WS] VOTING_START 메시지 수신:', data);
        if (data.candidates) {
          console.log('[Match WS] voteCandidates 저장 중:', data.candidates);
          matchStore.setVoteCandidates(data.candidates);
          console.log('[Match WS] voteCandidates 저장 완료, matchStore.voteCandidates:', matchStore.voteCandidates);

          // 투표 페이지로 이동
          const matchId = matchStore.gameInfo.matchId;
          if (matchId) {
            console.log('[Match WS] 투표 페이지로 이동:', matchId);
            router.push({
              name: 'contestVote',
              params: { matchId }
            });
          }
        }
      } break;

      case 'JUDGE_RESULT': {
        matchStore.setJudgeResult({
          success: !!data?.success,
          message: data?.message,
          stdout: data?.stdout,
          execution: data?.execution
        });
      } break;

      case 'GAME_END': {
        matchStore.setGameEndMessage(data?.message ?? '게임이 종료되었습니다.');
        matchStore.gameInfo.phaseType = 'END' as PhaseType;
        // JUDGE_RESULT success 후 종료된 경우 AI_REPORT 수신까지 버튼 잠금 + "AI 리포트 대기 중" 표시
        if (matchStore.lastJudgeResult?.success) {
          matchStore.setWaitingForAiReport(true);
        }
      } break;

      case 'AI_REPORT': {
        const aiFeedback = safeJsonParse(
          data?.data?.aiFeedback
          ?? data?.data?.ai_feedback
          ?? data?.aiFeedback
          ?? data?.ai_feedback
        );
        const gameResult = safeJsonParse(data?.data?.gameResult);
        const gradedResult = safeJsonParse(data?.data?.gradedResult);
        const matchId = data?.matchId ?? matchStore.gameInfo.matchId;
        matchStore.setAiReport({
          matchId,
          userId: data?.userId,
          aiFeedback,
          gameResult,
          gradedResult,
          raw: data
        });
        console.info('[Match WS] AI_REPORT 수신', { aiFeedback, gameResult, gradedResult });
      } break;

      case 'USER_LEFT': {
        if (data?.userId) {
          matchStore.removeUser(data.userId);
        }
      } break;
    }
  };

  const sendEmojiRequest = (matchId: string, emoji: string) => {
    const message = {
      type: 'EMOJI',
      matchId,
      emoji,
    };
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify(message));
    }
  };

  const sendHintRequest = (matchId: string, userId: string, sequence: number) => {
    if (sequence < 1 || sequence > 3) {
      console.warn('[Match WS] 존재하지 않는 힌트 유형');
      return;
    }

    const message = {
      type: 'HINT_REQUEST',
      matchId,
      userId,
      sequence
    };
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify(message));
    }
  };

  const sendLeaveRequest = (matchId: string) => {
    const message = {
      type: 'LEAVE',
      matchId
    };
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify(message));
      console.info('[Match WS] LEAVE 메시지 전송');
    }
  };

  const sendVoteRequest = (matchId: string, algo: string) => {
    const message = {
      type: 'VOTE',
      matchId,
      algo
    };
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify(message));
      console.info('[Match WS] VOTE 메시지 전송:', algo);
    }
  };

  onUnmounted(() => {
    disconnect();
  });

  // 라우터 변경 감지 (뒤로가기 등)
  const unsubscribe = router.beforeEach((to, from, next) => {
    const contestRoutes = new Set(['contest', 'contestVote']);
    const leavingContest = contestRoutes.has(String(from.name));
    const enteringContest = contestRoutes.has(String(to.name));

    if (leavingContest && !enteringContest) {
      disconnect();
    }
    next();
  });

  onBeforeUnmount(() => {
    unsubscribe();
  });

  return {
    connect, disconnect, connected,
    ydoc,
    sendMessage, sendEmojiRequest, sendHintRequest, sendLeaveRequest, sendVoteRequest
  }
};