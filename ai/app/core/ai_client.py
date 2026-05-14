from anthropic import AsyncAnthropic
from app.core.config import settings

# 공통 클라이언트 인스턴스
ai_client = AsyncAnthropic(
    api_key=settings.ANTHROPIC_API_KEY,
    base_url=settings.ANTHROPIC_BASE_URL
)