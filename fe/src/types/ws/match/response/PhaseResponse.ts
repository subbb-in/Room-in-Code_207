export interface PhaseResponse {
  type:         string,        // 메시지 타입
  phase:        string,        // DESIGN 또는 PLAYING
  duration?:    number,        // 설계 시간 (DESIGN용)
  currentTurn?: string,        // 현재 턴의 사용자 ID (PLAYING용)
  userIds?:     (string|number)[]; // 참가자 ID 목록
};