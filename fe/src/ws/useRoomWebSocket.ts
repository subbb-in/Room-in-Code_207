import { onUnmounted, ref } from "vue";
import { useUserStore } from "@/stores/userStore";
import router from "@/router";
import { getWebSocketBase } from '@/ws/wsBase';

export const useRoomWebSocket = () => {
  const userStore = useUserStore();
  const ws = ref<WebSocket | null>(null);
  const connected = ref(false);
  const currentCount = ref(0);
  const maxCount = ref(0);

  // 웹 소켓 연결
  const connect = (roomId: string, userId: string) => {
    try {
      if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) {
        console.log('[WebSocket] 이미 연결되어 있습니다.');
        return;
      }

      const finalRoomId = roomId;
      const token = userStore.accessToken;
      console.log('[WebSocket 연결 시도] roomId:', finalRoomId, 'userId:', userId);

      if (!finalRoomId) {
        console.error('[WebSocket 연결 실패] roomId가 없습니다!');
        return;
      }

      if (!token) {
        console.error('[WebSocket 연결 실패] 토큰이 없습니다!');
        return;
      }
      
      const wsBase = getWebSocketBase();
      if (!wsBase) {
        console.error('[WebSocket 연결 실패] WebSocket 주소를 찾을 수 없습니다!');
        return;
      }
      ws.value = new WebSocket(`${wsBase}/ws/rooms?roomId=${finalRoomId}&userId=${userId}&token=${token}`);

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
      console.log('[WebSocket] LEAVE 메시지 전송');
      sendMessage('LEAVE', '');

      // 메시지 전송 후 약간의 지연을 두고 연결 종료
      setTimeout(() => {
        if (ws.value) {
          ws.value.close();
        }
      }, 100);
    }
  };

  // 메시지 처리 함수
  const handleMessage = (data: any) => {
    console.log('[handleMessage] 받은 메시지 타입:', data.type, '전체 데이터:', data);

    switch (data.type) {
      // 초기 연결 완료
      case 'CONNECTED': {
        console.log('[CONNECTED] 웹소켓 연결됨: ', data);

        // DATA
        currentCount.value = data.data.currentCount;
        maxCount.value = data.data.maxCount;

        console.log(`[CONNECTED] 현재 인원: ${currentCount.value}/${maxCount.value}`);
      } break;

      // 참가자 입장
      case 'JOIN': {
        console.log('[JOIN] 참가자 입장: ', data);

        // DATA
        currentCount.value = data.data.currentCount;
        maxCount.value = data.data.maxCount;

        console.log(`[JOIN] 참가자 입장: ${currentCount.value}/${maxCount.value}`);
      } break;

      // 참가자 퇴장
      case 'LEAVE': {
        console.log('[LEAVE] 참가자 퇴장: ', data);

        // DATA
        currentCount.value = data.data.currentCount;
        maxCount.value = data.data.maxCount;

        console.log(`[LEAVE] 참가자 퇴장: ${currentCount.value}/${maxCount.value}`);
      } break;

      // 게임 시작
      case 'START': {
        console.log('[START] 게임 시작: ', data);

        const matchId = data?.roomId ?? data?.data?.openViduSessionId;

        if (matchId) {
          // FRIENDS 모드로 이동
          router.push({ 
            name: 'friends',
            params: { matchId }
          });
        } else {
          console.warn('[START] matchId가 없습니다:', data);
        }
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

  // 자동 연결 해제 처리 (누수 방지)
  onUnmounted(() => {
    disconnect();
  });

  return {
    connect, disconnect, connected, sendMessage,
    currentCount, maxCount
  }
};