from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import Problem
from app.schemas.hint import AIHintRequest, AIHintResponse
from app.services.ai_hint_service import generate_ai_hint

router = APIRouter()


@router.post("/ai-hint", response_model=AIHintResponse)
async def create_ai_hint(request: AIHintRequest, db: Session = Depends(get_db)):
    # 1. DB에서 문제 정보(질문, 정답 코드, 난이도 등) 조회
    problem = db.query(Problem).filter(Problem.problem_id == request.problem_id).first()

    if not problem:
        raise HTTPException(status_code=404, detail="해당 문제를 찾을 수 없습니다.")

    # 2. AI 서비스 로직 호출 (외부 API 연동)
    try:
        # DB에서 가져온 정답 코드와 사용자가 제출한 코드를 비교 분석하도록 전달
        hint_text = await generate_ai_hint(
            problem_question=problem.question,
            correct_solution=problem.solutions,
            user_code=request.answer_code,
            language=request.language,
            time_result=request.time_complexity,
            mem_result=request.mem_usage
        )

        return AIHintResponse(hint=hint_text)

    except Exception as e:
        # 로깅 후 사용자에게 에러 반환
        print(f"AI Hint Generation Error: {e}")
        raise HTTPException(status_code=500, detail="AI 힌트 생성 중 오류가 발생했습니다.")