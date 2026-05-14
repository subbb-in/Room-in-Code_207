import { ref } from "vue";
import { useUserStore } from "@/stores/userStore";
import { useContestStore } from "@/stores/contestStore";
import router from "@/router";
import { getWebSocketBase } from '@/ws/wsBase';

// 상태 변수를 모듈 스코프로 이동하여 모든 컴포넌트에서 공유 (Singleton Pattern)
const ws = ref<WebSocket | null>(null);
const connected = ref(false);
const currentCount = ref(0);
const maxCount = ref(0);

export const useContestWebSocket = () => {
  const userStore = useUserStore();
  const contestStore = useContestStore();

  // 웹 소켓 연결
  const connect = () => {
    try {
      // 이미 연결되어 있거나 연결 중이라면 패스
      if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) {
        console.log('[WebSocket] 이미 연결되어 있습니다.');
        return;
      }

      const token = userStore.accessToken;
      console.log('[WebSocket 연결 시도]');

      if (!token) {
        console.error('[WebSocket 연결 실패] 토큰이 없습니다!');
        return;
      }

      const wsBase = getWebSocketBase();
      if (!wsBase) {
        console.error('[WebSocket 연결 실패] WebSocket 주소를 찾을 수 없습니다!');
        return;
      }
      ws.value = new WebSocket(`${wsBase}/ws/contest?token=${token}`);

      // 연결 성공
      ws.value.onopen = () => {
        console.log('[WebSocket 연결 성공]');
        connected.value = true;
      };

      // 연결 해제
      ws.value.onclose = (event) => {
        if (event.code === 1000 || event.code === 1001) {
          // 정상 종료 (1000: Normal Closure, 1001: Going Away)
          console.log('[WebSocket 연결 종료]');
        } else {
          // 비정상 종료
          console.log('[WebSocket 연결 실패] code:', event.code, 'reason:', event.reason);
        }
        connected.value = false;
        ws.value = null; // 연결 객체 초기화
      };

      // 연결 오류
      ws.value.onerror = (err) => {
        console.error('[WebSocket 오류]: ', err);
        connected.value = false;
      };

      // 메시지 처리
      ws.value.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[WebSocket 데이터]: ', data);

          // 데이터 처리 함수
          handleMessage(data);
        } catch (err) {
          console.error('[WebSocket 데이터 오류]: ', err);
        }
      };
    } catch (err) {
      console.error('[WebSocket 연결 오류]: ', err);
      connected.value = false;
    }
  };

  // 웹 소켓 연결 해제
  const disconnect = () => {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      // 서버에 LEAVE 메시지 전송
      console.log('[WebSocket] LEAVE 메시지 전송 (disconnect 호출)');
      sendMessage('LEAVE', '');

      // 메시지 전송 후 약간의 지연을 두고 연결 종료
      setTimeout(() => {
        if (ws.value) {
          ws.value.close();
          ws.value = null;
          connected.value = false;
        }
      }, 100);
    } else {
      console.log('[WebSocket] 연결이 없거나 이미 종료됨 (disconnect 호출)');
    }
  };

  // 메시지 처리 함수
  const handleMessage = (data: any) => {
    console.log('[handleMessage] 받은 메시지 타입:', data.type, '전체 데이터:', data);

    switch (data.type) {
      case 'START': {
        const matchId = data?.roomId ?? data?.data?.openViduSessionId;

        if (matchId) {
          contestStore.isMatched = true;
          console.log('[START] contest vote 페이지 이동:', matchId);
          router.push({ name: 'vote', params: { matchId } }).catch((err) => {
            console.error('[START] contest vote 페이지 이동 실패:', err);
          });
        } else {
          console.warn('[START] matchId가 없습니다:', data);
        }
      } break;

      // 게임 시작
      case 'MATCH_FOUND': {
        console.log('[MATCH_FOUND] 게임 시작: ', data);

        // 매칭 성공 상태 업데이트
        contestStore.isMatched = true;

        // 웹소켓 연결 해제
        disconnect();

        // matchId는 roomId와 동일
        const matchId = data.matchId;

        // VOTE 모드로 이동
        console.log(`[WebSocket] 이동 시도 -> name: 'vote', params: { matchId: ${matchId} }`);
        router.push({
          name: 'vote',
          params: { matchId }
        }).then(() => {
          console.log('[WebSocket] 페이지 이동 성공');
        }).catch((err) => {
          console.error('[WebSocket] 페이지 이동 실패:', err);
        });
      } break;
    }
  };

  const sendMessage = (type: string, data: any) => {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      const message = JSON.stringify({ type, data });
      console.log('[WebSocket] 메시지 전송:', type, data);
      ws.value.send(message);
    } else {
      console.warn('[WebSocket] 메시지 전송 실패 - 연결 상태:', ws.value?.readyState);
    }
  };

  // singleton이므로 onUnmounted 자동 해제는 제거 (명시적으로 disconnect 호출해야 함)

  return {
    connect, disconnect, connected, sendMessage,
    currentCount, maxCount
  }
};