from pydantic import BaseModel


class AIRelayRequest(BaseModel):
    problem_question: str
    user_code: str
    language: str

class AIRelayResponse(BaseModel):
    ai_code: str