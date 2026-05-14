from typing import Dict
from pydantic import BaseModel


class AIFeedbackRequest(BaseModel):
    problem_id : int
    user_code : str
    # user_id를 기반으로 피드백 or 문제 생성 고려 중
    # user_id : str
    language : str
    time_complexity : float
    mem_usage : int


class AIFeedbackResponse(BaseModel):
    # 사용자 코드에 대한 한 줄 코멘트
    comment : str
    # 사용자 코드 리펙토링한 코드
    refactored_code : str
    # 추천하는 문제 {문제 이름 : URI, 문제 이름 : URI}
    recommendation : Dict[str, str]
