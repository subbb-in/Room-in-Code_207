import logging
from app.core.config import settings
from app.core.ai_client import ai_client
# from app.database import chroma_client

# 퓨샷 프롬프팅
FEW_SHOT_EXAMPLES = """
[예시 1]
- 최종 힌트 : N의 값이 1e6을 넘어갈 때 재귀 호출을 수행하면 중복 계산은 몇 번 발생할까?  

[예시 2]
- 최종 힌트 : 큐에 데이터를 넣는 시점에 방문 표시를 하지 않으면 어떤 문제가 발생할까? 

[예시 3]
- 최종 힌트 : 간선이 음의 값인데 다익스트라 알고리즘을 적용하면 어떤 상황이 발생할까? 
"""


# 로거 설정
logger = logging.getLogger(__name__)

# 엔드포인트 및 API 키 설정
# client = AsyncAnthropic(
#     api_key=settings.ANTHROPIC_API_KEY,
#     base_url=settings.ANTHROPIC_BASE_URL
# )


async def generate_ai_hint(
        problem_question: str,
        correct_solution: str,
        user_code: str,
        language: str,
        time_result: str = None,
        mem_result: str = None
) -> str:
    """
    문제 정보와 사용자 코드를 분석하여 Anthropic API를 통해 1줄 분량의 힌트를 생성합니다.
    """



    # 2. CoT 기법을 적용한 프롬프트 구성
    prompt = f"""
        당신은 알고리즘 멘토입니다. [정답 코드]와 [문제 내용]을 바탕으로 사용자의 [오답 코드]를 분석하여 CoT(사고 과정)를 거친 후 1줄 힌트를 제공하세요.
        
        {FEW_SHOT_EXAMPLES}

        [정답 코드]
        {correct_solution}

        [문제 내용]
        {problem_question}

        [사용자 제출 코드 ({language})]
        {user_code}

        [분석 및 사고 과정]
        1. 문제 내용에서 요구하는 알고리즘 역량이 무엇인지 파악한다. 
        2. 정답 코드와 사용자의 오답 코드를 비교해 사용자의 코드가 해당 원리를 어떻게 위반했거나 놓쳤는지 분석한다.
        3. 직접적인 정답은 제시하지 말되 사용자 코드의 치명적인 문제를 한 문장으로 요약한다. 

        [요청 사항]
        - 위 분석 및 사고 과정을 거치되 출력은 하지 말아야 한다. 
        - 사용자가 올바른 방향으로 알고리즘을 풀도록 유도하는 최종 힌트만을 도출해 출력한다. 
        - 답변 형식은 반드시 한국어 한 문장으로 구성된 의문문이어야 한다. 
        
        
        
        """

    try:
        # 3. 디버깅: API 호출 직전 로그
        print(f"Anthropic API 호출 시도 중... (Model: claude-sonnet-4-5-20250929)")

        message = await ai_client.messages.create(
            max_tokens=1024,
            messages=[{"content": prompt, "role": "user"}],
            model="claude-sonnet-4-5-20250929",
        )

        result_hint = message.content[0].text.strip()

        print(f"AI 생성 성공: {result_hint}")
        return result_hint

    except Exception as e:
        print(f"에러 타입: {type(e)}")
        print(f"에러 메시지: {e}")

        return "외부 API 호출 실패"