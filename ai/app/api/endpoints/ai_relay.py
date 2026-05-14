from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import Problem
from app.schemas.relay import AIRelayResponse, AIRelayRequest
from app.services.ai_relay_service import generate_ai_code

router = APIRouter()

@router.post("/ai-relay",response_model = AIRelayResponse)
async def creat_ai_code(request:AIRelayRequest):

    try:
        ai_code = await generate_ai_code(
            problem_question=request.problem_question,
            user_code = request.user_code,
            language = request.language

        )
        return AIRelayResponse(ai_code=ai_code)

    except Exception as e:
        print(f"AI Relay Generation Error: {e}")
        raise HTTPException(status_code=500, detail="AI 코드 생성 중 오류가 발생했습니다.")