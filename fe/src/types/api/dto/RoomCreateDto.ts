export interface RoomCreateDto {
  title:      string,     // 방 이름
  password:   string,     // 비밀번호
  capacity:   number,     // 최대 인원 수
  language:   string,     // 프로그래밍 언어
  algorithm:  string,     // 알고리즘 유형
  difficulty: string,     // 난이도
  turnTime:   number,     // 턴 시간
  mode:       string,     // 모드
};