from pydantic import BaseModel
from typing import Optional

class AIHintRequest(BaseModel):
    #user_id: int
    # problem_id로 problems 테이블의 question, solutions, difficulty, time_limit, memory_limit 를 조회하기 위한 값
    problem_id: int
    # 사용자가 채점 서버에 제출한 코드(텍스트)
    answer_code: str
    # answer_code의 language
    language: str

    # 채점 서버 -> 게임 서버 -> AI 서버
    # 채점 서버에서 게임 서버에 보냈던 결과 중 시간 복잡도와 메모리 사용량 채점 결과
    time_complexity: Optional[str] = None
    mem_usage: Optional[str] = None

class AIHintResponse(BaseModel):
    hint: str
    # AI가 생성한 1줄 피드백

