import logging
from typing import Dict
from app.core.ai_client import ai_client
from app.services.qdrant_service import hybrid_search

logger = logging.getLogger(__name__)

'''
    # 사용자가 작성한 코드, 채점한 결과(사용한 시간, 메모리)를 기준으로 
    # AI가 한 마디 응원 코멘트를 남겨줌 
        -> ex. 코드 리팩토링 결과를 같이 넘겨서 그것에 대한 피드백을 남김 
    # 한국어 답변의 자연스러움을 위해 상용 모델 사용 : API 호출 
'''


async def generate_ai_comment(
        problem_question : str,
        language : str,
        user_code : str,
        refactored_code : str,
        user_time: float,
        user_mem: int,
        target_time : str,
        target_mem : str
        ) -> str:
    prompt = f"""
        당신은 알고리즘 멘토입니다. 사용자가 푼 문제와 제출한 코드를 분석하여 성장을 돕는 따뜻하지만 날카로운 피드백을 제공하세요.

        [문제 내용]
        {problem_question}

        [문제 제약 사항]
        - 제한 시간 : {target_time}
        - 제한 메모리 : {target_mem}
        
        [사용자 제출 코드 ({language})]
        {user_code}

        [실행 결과]
        - 사용자 코드 실행 시간: {user_time}
        - 사용자 코드 메모리 사용량: {user_mem}
    
        [AI가 리팩토링한 사용자 코드]
        {refactored_code}
        
        [요청 사항]
        - 사용자 코드와 AI가 리팩토링한 사용자 코드를 비교해 사용자 코드의 발전 방향성을 포착하세요. 
        - 코드의 시간/공간 효율성이나 알고리즘 접근 방식에 대해 분석하세요.
        - 잘한 점과 개선할 점을 포함하되, 반드시 한국어 한 문장으로 작성하세요.
        """

    try:
        message = await ai_client.messages.create(
            max_tokens=1024,
            messages=[{"content": prompt, "role": "user"}],
            model="claude-sonnet-4-5-20250929",
        )
        return message.content[0].text.strip()
    except Exception as e:
        print(f"AI Comment Generation Error: {e}")
        return "코드 분석 중 오류가 발생했습니다."

'''
    # 사용자 코드에 대한 리팩토링 기능 구현 
    # prompt 작성 시 원래 정답과 나의 코드를 전달해 리팩토링 코드 반환
    # 호출할 client에 역할을 부여, Few-shot 기법 사용, Self-Augmented Prompting 기법 사용 
'''

async def refactor_code(
        problem_question: str,
        correct_solution: str,
        language: str,
        user_code: str
) -> str:
    """
    사용자의 코드를 분석하여 성능 개선이나 가독성 향상을 위한 리팩토링된 코드를 제안합니다.
    """

    prompt = f"""
    당신은 시니어 소프트웨어 엔지니어이자 알고리즘 전문가입니다. 
    다음 알고리즘 문제에 대해 사용자가 작성한 코드를 리팩토링하세요.

    [문제 내용]
    {problem_question}

    [참고용 정답 코드]
    {correct_solution}

    [사용자 작성 코드 ({language})]
    {user_code}

    [요청 사항]
    1. 사용자의 로직을 유지하면서도 더 효율적인(시간/공간 복잡도 개선) 코드로 수정하세요.
    2. 가독성이 떨어지는 변수명이나 구조를 개선하세요.
    3. 출력은 반드시 리팩토링된 '코드'만 반환해야 하며, 다른 설명 텍스트는 포함하지 마세요.
    4. 코드 블록(```) 기호 없이 코드 자체만 출력하세요.
    """

    try:
        # API 호출
        message = await ai_client.messages.create(
            max_tokens=2048,
            messages=[{"content": prompt, "role": "user"}],
            model="claude-sonnet-4-5-20250929",
        )

        refactored_result = message.content[0].text.strip()
        return refactored_result

    except Exception as e:
        print(f"Refactor Code Error: {e}")
        return "// 리팩토링 코드를 생성하는 중 오류가 발생했습니다."

'''
    # Qdrant 벡터 DB에서 유사 문제 검색
    # Question(70%) + Solutions(30%) 하이브리드 유사도 검색
'''
async def recommend_problems(
        problem_question : str
) -> Dict[str, str]:
    """
    입력 문제와 유사한 문제 1개를 추천합니다.
    Qdrant 하이브리드 검색 사용 (Question 70% + Solutions 30%)
    
    Returns:
        Dict[str, str]: {"추천 문제": "url"}
    """

    
    try:
        results = hybrid_search(problem_question, top_k=1)
        
        if not results:
            return {
                "추천 문제": "추천 결과가 없습니다."
            }
        
        result = results[0]
        url = result.get("url", "")
        name = result.get("name", "문제")
        
        return {
            "추천 문제": url if url else f"URL 없음 ({name})"
        }
        
    except Exception as e:
        logger.error(f"Problem recommendation failed: {e}")
        return {
            "추천 문제": "추천 중 오류가 발생했습니다."
        }


