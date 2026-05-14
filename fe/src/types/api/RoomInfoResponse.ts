export interface RoomInfoResponse {
  id:     string,     // uuid
  title:  string,     // 방 이름
  pwd:    string,     // 비밀번호
  cnt:    number,     // 현재 인원 수
  max:    number,     // 최대 인원 수
  lang:   string,     // 프로그래밍 언어
  diff:   string,     // 난이도
  algo:   string,     // 알고리즘 유형
  time:   string,     // 턴 시간
  mode:   string,     // 모드
  stat:   string,     // 상태
};