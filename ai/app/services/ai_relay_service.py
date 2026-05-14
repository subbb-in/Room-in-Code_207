import random
import logging
from app.core.ai_client import ai_client
from app.core.config import settings

# 로거 설정
logger = logging.getLogger(__name__)


async def generate_ai_code(
        problem_question: str,
        user_code: str,
        language: str
) -> str:
    """
    Tree of Thought 기법을 사용하여 사용자의 전략을 분석하고,
    '전략 방해' 또는 '전략 계승' 코드 중 하나를 랜덤하게 생성하여 반환합니다.
    """

    # 1. Tree of Thought 적용 프롬프트 구성
    # 사용자 코드의 의도를 다각도로 분석(ToT)하고 두 가지 시나리오의 코드를 생성하도록 요청합니다.

    prompt = f"""
    당신은 릴레이 알고리즘 게임의 AI 대전 상대입니다. 
    [문제 내용]과 [현재까지 작성된 사용자 코드]를 바탕으로 다음 단계의 사고 과정을 거쳐 코드를 생성하세요.

    [문제 내용]
    {problem_question}

    [사용자 제출 코드 ({language})]
    {user_code}

    [Tree of Thought 사고 과정]
    1. 분석(Thought 1): 사용자가 이 코드를 통해 구현하려는 구체적인 알고리즘 전략이나 의도는 무엇인가?
    2. 확장(Thought 2 - 방해): 사용자의 전략을 무력화하거나, 코드를 복잡하게 만들어 풀이를 어렵게 만드는 '방해 코드'는 무엇인가?
    3. 확장(Thought 3 - 계승): 사용자의 의도를 정확히 파악하여 최적의 효율로 코드를 이어가는 '협력 코드'는 무엇인가?

    [요청 사항]
    - 위 사고 과정을 내부적으로 수행하되, 결과값으로는 아래 두 가지 버전의 코드만 생성하세요.
    - 코드 외의 설명은 생략하세요.
    - 구분자 "---CODE_SEP---"를 사용하여 두 코드를 구분하세요.

    형식:
    <방해 코드>
    (사용자의 전략을 방해하는 코드)
    ---CODE_SEP---
    <계승 코드>
    (사용자의 전략에 이어지는 코드)
    """

    try:
        logger.info("Anthropic API 호출: 릴레이 코드 생성 시도")

        message = await ai_client.messages.create(
            max_tokens=2048,
            messages=[{"content": prompt, "role": "user"}],
            model="claude-sonnet-4-5-20250929",
        )

        full_response = message.content[0].text.strip()

        # 2. 생성된 두 가지 코드 분리
        codes = full_response.split("---CODE_SEP---")

        if len(codes) < 2:
            return "AI 코드 생성 중 구조적 오류 발생"

        # 3. Random 선택 (방해 vs 계승)
        selected_code = random.choice(codes).strip()

        # 태그 제거(있을 경우) 및 최종 정제
        final_code = selected_code.replace("<방해 코드>", "").replace("<계승 코드>", "").strip()

        logger.info("AI 릴레이 코드 생성 성공")
        return final_code

    except Exception as e:
        logger.error(f"AI Relay Code Generation Error: {e}")
        return f"// 에러 발생으로 인한 기본 코드 반환\n// {str(e)}"