export type PhaseType = 
  'VOTING'    |     // 투표
  'DESIGN'    |     // 설계 
  'PLAYING'   |     // 게임 중
  'END'             // 종료
;

export interface ProblemInfo {
  difficulty:               string,     // 난이도
  expectedAuxSpace:         string,     // 공간 복잡도
  expectedTimeComplexity:   string,     // 시간 복잡도
  inputOutput:              string,     // 입출력 예시
  memoryLimit:              string,     // 메모리 제한
  problemId:                number,     // 문제 번호
  question:                 string,     // 문제 내용
  source:                   string,     // 출처
  timeLimit:                string,     // 시간 제한
  url:                      string,     // 출처 URL
  translatedProblem?:       string,     // (한국어) 문제 내용
};

export interface MatchGameInfo {
  matchId:              string,           // 매치 ID
  mode?:               string,           // 모드 (AI, FRIENDS, CONTEST)
  phaseType:            PhaseType,        // 페이지 유형
  problemInfo?:         ProblemInfo,      // 문제 정보
  programmingLanguage?: string,           // 프로그래밍 언어
  designTimePerSec:     number,           // 설계 시간 (초 단위)
  remainingTimePerSec:  number,           // 남은 시간 (인-게임 초 단위),
  currentTurnUserId:    number | string,  // 현재 턴의 사용자 ID
};