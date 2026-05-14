export interface RoomLeaveResponse {
  message:        string,     // 메시지
  remainingCount: number,     // 남은 인원 수
  deleted:        boolean,    // 방 삭제 여부
};