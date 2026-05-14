from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import Problem
from app.schemas.feedback import AIFeedbackResponse, AIFeedbackRequest
from app.services.ai_feedback_service import generate_ai_comment, refactor_code, recommend_problems

router = APIRouter()

@router.post("/ai-feedback", response_model=AIFeedbackResponse)
async def create_ai_feedback(request:AIFeedbackRequest, db : Session = Depends(get_db)):
    problem = db.query(Problem).filter(Problem.problem_id == request.problem_id).first()

    if not problem:
        raise HTTPException(status_code=404, detail="해당 문제를 찾을 수 없습니다.")

    try:
        refactored_code = await refactor_code(problem.question,
                                              problem.solutions,
                                              request.language,
                                              request.user_code)
        comment = await generate_ai_comment(problem.question,
                                            request.language,
                                            request.user_code,
                                            refactored_code,
                                            request.time_complexity,
                                            request.mem_usage,problem.time_limit,
                                            problem.memory_limit)
        recommendation = await recommend_problems(problem.question)

        return AIFeedbackResponse(comment=comment,
                                  refactored_code=refactored_code,
                                  recommendation=recommendation)
    except Exception as e:
        print(f"AI Feedback Generation Error:{e}")
        raise HTTPException(status_code=500, detail="AI 피드백 생성 중 오류가 발생했습니다. ")