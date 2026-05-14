export interface HintResponse {
  type:         string,     // 메시지 타입
  matchId:      string,     // 매치 ID
  userId:       string,     // 사용자 ID
  sequence:     number,     // 힌트 유형?
  content:      string,     // 힌트 내용
  remainingHp:  number,     // 남은 HP
};