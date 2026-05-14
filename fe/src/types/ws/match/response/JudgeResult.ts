export interface JudgeResult {
  type:       string,   // 메시지 타입
  userId:     string,   // 사용자 ID
  success:    boolean,  // 성공 여부
  message:    string,   // 메시지
  stdout:     string,   // 코드 출력값
  execution: {
    time:     number,   // 실행시간
    memory:   string,   // 메모리 사용량
  }
}