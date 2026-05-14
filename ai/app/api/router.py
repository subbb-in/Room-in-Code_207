from fastapi import APIRouter
from app.api.endpoints import ai_hint
from app.api.endpoints import ai_feedback
from app.api.endpoints import ai_relay

api_router = APIRouter()


api_router.include_router(ai_hint.router, prefix="/ai", tags=["AI Hint"])
api_router.include_router(ai_feedback.router, prefix="/ai", tags=["AI Feedback"])
api_router.include_router(ai_relay.router, prefix="/ai", tags=["AI Relay"])
