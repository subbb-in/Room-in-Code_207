export interface TimerResponse {
  type:           string,     // 메시지 타입
  matchId:        string,     // 매치 ID
  remainingTime:  number,     // 남은 시간 (초 단위)
  currentUserId:  number,     // 현재 턴의 사용자 ID
  seq:            string,     // 턴 시퀀스 (진행된 턴 수?)
};