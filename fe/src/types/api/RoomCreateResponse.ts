export interface RoomCreateResponse {
  roomId:     string,     // 방의 식별번호(UUID)
  title:      string,     // 방 이름
  language:   string,     // 프로그래밍 언어
  algorithm:  string,     // 알고리즘 유형
  difficulty: string,     // 난이도
  capacity:   number,     // 최대 인원 수
  count:      number,     // 현재 인원 수
  state:      string,     // 상태
  mode:       string,     // 모드
};