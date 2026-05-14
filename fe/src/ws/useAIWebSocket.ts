import * as Y from 'yjs';
import { ref } from "vue";
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


// Singleton state (shared across views)
const ws = ref<WebSocket | null>(null);
const connected = ref(false);
const ydoc = ref<Y.Doc | null>(null);

export const useAIWebSocket = () => {

  // Etc
  const userStore = useUserStore();
  const matchStore = useMatchStore();

  let problemRequested = false;

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

  const setupYDoc = () => {
    if (ydoc.value) return ydoc.value;

    const doc = new Y.Doc();
    doc.getText('code');

    doc.on('update', (update: Uint8Array, origin: any) => {
      if (origin !== 'remote' && ws.value?.readyState === WebSocket.OPEN) {
        console.info('[AI WS Yjs] 바이너리 송신:', {
          byteLength: update.byteLength,
          origin: origin ?? 'local'
        });
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
  const connect = (matchId: string, token: string) => {
    try {
      problemRequested = false;
      if (!token) {
        console.warn('[AI WS] 토큰이 없습니다. 연결을 중단합니다.');
        return;
      }

      if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) {
        console.warn('[AI WS] 이미 연결되어 있습니다.');
        return;
      }

      const wsBase = getWebSocketBase();
      if (!wsBase) {
        console.warn('[AI WS] WebSocket 주소를 찾을 수 없습니다.');
        return;
      }
      ws.value = new WebSocket(`${wsBase}/ws/match?matchId=${matchId}&token=${token}`);
      ws.value.binaryType = 'arraybuffer';

      // 연결 성공
      ws.value.onopen = () => {
        console.info('[AI WS] 연결 성공');
        connected.value = true;

        matchStore.setMatchId(matchId);
        matchStore.setJudgeResult(null);
        matchStore.setGameEndMessage(null);

        // JOIN 요청 전송
        sendMessage('JOIN', matchId);

        // ----------

        // Yjs 초기화
        setupYDoc();
        console.log('[AI WS: Yjs] 초기화 성공');

        // 동기화 요청
        const initState = ydoc.value ? Y.encodeStateAsUpdate(ydoc.value) : new Uint8Array();
        if (ws.value?.readyState === WebSocket.OPEN && initState.length) {
          ws.value.send(initState);
        }
      };

      // 연결 해제
      ws.value.onclose = (event) => {
        if (event.code === 1000 || event.code === 1001) {
          // 정상적인 종료
          console.info('[AI WS] 연결 종료');
        } else {
          console.warn('[AI WS 연결 실패] ', event);
        }

        connected.value = false;
      };

      // 연결 오류
      ws.value.onerror = (err) => {
        console.error('[AI WS] 오류: ', err);
        connected.value = false;
      };

      // 메시지 처리
      ws.value.onmessage = async (event) => {
        // Yjs 동기화
        if (event.data instanceof ArrayBuffer) {
          console.info('[AI WS Yjs] 바이너리 데이터 수신:', {
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
              const textLen = ydoc.value.getText('code').length;
              console.info('[AI WS Yjs] 바이너리 갱신 적용:', {
                byteLength: event.data.byteLength,
                textLength: textLen
              });
            } catch (err) {
              console.error('[AI WS Yjs] 갱신 적용 실패: ', err);
            }
          }
          return;
        }

        // Blob도 바이너리로 처리
        if (event.data instanceof Blob) {
          console.info('[AI WS Yjs] 바이너리 데이터 수신(Blob):', {
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
              const textLen = ydoc.value.getText('code').length;
              console.info('[AI WS Yjs] 바이너리(Blob) 갱신 적용:', {
                byteLength: arrayBuffer.byteLength,
                textLength: textLen
              });
            } catch (err) {
              console.error('[AI WS Yjs] 갱신 적용 실패: ', err);
            }
          }
          return;
        }

        // 일반 메시지
        try {
          const data = JSON.parse(event.data);
          handleMessage(data);
        } catch (err) {
          console.error('[AI WS 메시지 Error: ', err);
        }
      };
    } catch (err) {
      console.warn('[AI WS] 연결 오류: ', err);
    }
  };

  const disconnect = () => {
    if (ydoc.value) {
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
      console.info('[AI WS] 메시지 전송:', message);
      ws.value.send(JSON.stringify(message));
      return;
    }

    console.warn('[AI WS] 메시지 전송 실패: ', ws.value?.readyState);
  };

  const requestProblemIfNeeded = (matchId: string) => {
    if (problemRequested || matchStore.gameInfo.problemInfo) return;
    problemRequested = true;
    sendMessage('PROBLEM_REQUEST', matchId);
  };

  const handleMessage = async (data: any) => {
    console.info('[AI WS] 메시지 수신: ', data);

    const safeJsonParse = (value: unknown) => {
      if (typeof value !== 'string') return value as Record<string, unknown> | null;
      try {
        return JSON.parse(value) as Record<string, unknown>;
      } catch {
        return null;
      }
    };

    // console.log(data);

    const applyAiTurnContext = (duration?: number) => {
      matchStore.gameInfo.currentTurnUserId = 'AI_BOT';
      if (typeof duration === 'number' && !Number.isNaN(duration)) {
        matchStore.gameInfo.remainingTimePerSec = duration;
      }
    };

    switch (data.type) {
      case 'PARTICIPANTS_UPDATE': {
        const resp = data;
        if (resp?.userIds && Array.isArray(resp.userIds)) {
          try {
            const users: MatchUserInfo[] = [];

            for (const userId of resp.userIds) {
              if (String(userId).toUpperCase() === 'AI_BOT') {
                users.push({
                  userId: 'AI_BOT',
                  nickname: 'AI Bot',
                  remainingHp: 100,
                  profileImageUrl: ''
                });
                continue;
              }

              const resp2 = await getUserInfo(Number(userId));
              if (resp2.statusCode === HttpStatusCode.Ok) {
                const user: MatchUserInfo = {
                  nickname: resp2.data.nickname,
                  remainingHp: 100,
                  userId: String(userId),
                  profileImageUrl: resp2.data.profileImageUrl
                };
                users.push(user);
              }
            }
            matchStore.setUserList(users);
            console.log('[AI WS] 참가자 목록 업데이트:', users);
          } catch (err: any) {
            console.error('[AI WS] PARTICIPANTS_UPDATE 오류: ', err);
          }
        }
      } break;

      case 'VOTING_START': {
        const candidates = Array.isArray(data?.candidates) ? data.candidates : [];
        matchStore.setVoteCandidates(candidates);
        matchStore.gameInfo.phaseType = 'VOTING';
      } break;

      case 'PHASE_CHANGED': {
        const resp = data as PhaseResponse;
        if (resp) {
          if (resp.duration !== undefined) {
            matchStore.gameInfo.designTimePerSec = matchStore.gameInfo.mode === 'AI' ? 0 : resp.duration;
          }

          matchStore.gameInfo.phaseType = resp.phase as PhaseType;

          if (resp.currentTurn !== undefined && resp.currentTurn !== null) {
            matchStore.gameInfo.currentTurnUserId = resp.currentTurn;
          }

          if (resp.phase === 'DESIGN' || resp.phase === 'PLAYING') {
            requestProblemIfNeeded(data?.matchId ?? matchStore.gameInfo.matchId);
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

          requestProblemIfNeeded(data?.matchId ?? matchStore.gameInfo.matchId);
        }
      } break;

      case 'HP_UPDATE': {
        const resp = data as HPUpdateResponse;
        if (resp) {
          matchStore.setHP(resp.userId, resp.remainingHp);
        }
      } break;

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
        console.warn('[AI WS] HINT_ERROR:', data?.message);
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
          console.warn('[AI WS] AI_HINT payload empty:', data);
        }
      } break;

      case 'CODE_UPDATE': {
        const resp = data as CodeUpdateResponse & {
          userId?: string;
          senderId?: string;
          sender?: string;
          duration?: number;
          turnTime?: number;
          updateBase64?: string;
          code?: string;
          ai_code?: string;
        };
        if (resp?.updateBase64 && ydoc.value) {
          try {
            const update = base64ToUint8(resp.updateBase64);
            Y.applyUpdate(ydoc.value, update, 'remote');
          } catch (err) {
            console.error('[AI WS Yjs] CODE_UPDATE 적용 실패:', err);
          }
          break;
        }
        const sender = resp.sender ?? resp.senderId ?? resp.userId;
        const aiCode = resp.code ?? resp.ai_code;
        if (aiCode && sender === 'AI_BOT') {
          const duration = resp.duration ?? resp.turnTime ?? matchStore.gameInfo.remainingTimePerSec;
          applyAiTurnContext(duration);
          matchStore.setAiCodePlayback(aiCode, duration);
        }
        // const resp = data as CodeUpdateResponse & { userId?: string; senderId?: string; sender?: string; duration?: number; turnTime?: number; updateBase64?: string };
        // if (resp?.updateBase64 && ydoc.value) {
        //   try {
        //     const update = base64ToUint8(resp.updateBase64);
        //     Y.applyUpdate(ydoc.value, update, 'remote');
        //   } catch (err) {
        //     console.error('[Match WS Yjs] CODE_UPDATE 적용 실패:', err);
        //   }
        //   break;
        // }
        // const sender = resp.sender ?? resp.senderId ?? resp.userId;
        // if (resp?.code && sender === 'AI_BOT') {
        //   const duration = resp.duration ?? resp.turnTime ?? matchStore.gameInfo.remainingTimePerSec;
        //   applyAiTurnContext(duration);
        //   matchStore.setAiCodePlayback(resp.code, duration);
        // }
      } break;

      // 모든 사용자에게 이모지 출력
      case 'EMOJI': {
        const resp = data as EmojiResponse;
        if (resp?.emoji) {
          // 거품 이모지 추가
          console.log(`[AI WS] 이모지 발송: ${resp.emoji}`);
          matchStore.addFloatingEmoji(resp.emoji);
          console.log(`[AI WS] floatingEmojis 길이:`, matchStore.floatingEmojis.length);
        }
      } break;

      // 문제 정보
      case 'PROBLEM_RESPONSE': {
        const resp = data?.problem as ProblemInfo;
        if (resp) {
          matchStore.gameInfo.problemInfo = resp;
          if (data?.translatedProblem) {
            matchStore.gameInfo.problemInfo.translatedProblem = data.translatedProblem;
          }
          problemRequested = true;
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
        matchStore.gameInfo.phaseType = 'END';
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
      } break;

      case 'AI_CODE':
      case 'AI_CODE_RESPONSE': {
        if (data?.code) {
          const duration = data?.duration ?? data?.turnTime ?? matchStore.gameInfo.remainingTimePerSec;
          applyAiTurnContext(duration);
          matchStore.setAiCodePlayback(data.code, duration);
        }
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
      console.warn('[AI WS] 존재하지 않는 힌트 유형');
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
      console.info('[AI WS] LEAVE 메시지 전송');
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
    }
  };

  const sendReadyRequest = (matchId: string) => {
    const message = {
      type: 'AI_READY',
      matchId
    };
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify(message));
    }
  };

  const sendAIRequest = (matchId: string, customCode?: string) => {
    const code = customCode ?? ydoc.value?.getText('code').toString() ?? '';

    const message = {
      type: 'TURN_END',
      matchId,
      code
    };
    if (ws.value?.readyState === WebSocket.OPEN) {
      console.info('[AI WS] AI 요청 전송 (TURN_END)');
      ws.value.send(JSON.stringify(message));
    } else {
      console.warn('[AI WS] 전송 실패: 연결되지 않음');
    }
  };

  return {
    connect, disconnect, connected,
    ydoc,
    sendMessage, sendEmojiRequest, sendHintRequest, sendLeaveRequest, sendVoteRequest, sendReadyRequest, sendAIRequest
  }
};
